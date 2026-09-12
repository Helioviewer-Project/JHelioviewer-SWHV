#!/usr/bin/env python3
"""Stage ANGLE jars, test old/new rendering, and optionally install the tested jars."""
import argparse
import hashlib
import html
import io
import json
import os
from pathlib import Path
import platform as host_platform
import re
import shutil
import subprocess
import sys
import tarfile
import zipfile

ROOT = Path(__file__).resolve().parents[2]
PLATFORMS = {
    "macos-arm64": ("macos-arm64", "dylib"),
    "macos-x64": ("macos", "dylib"),
    "linux-x64": ("linux", "so"),
    "windows-x64": ("windows", "dll"),
}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def artifact(directory, platform, version):
    sums = {}
    for line in (directory / "SHA256SUMS").read_text().splitlines():
        checksum, name = line.split("  ", 1)
        path = directory / name
        if not path.resolve().is_relative_to(directory.resolve()) or not re.fullmatch(r"[0-9a-f]{64}", checksum):
            raise ValueError(f"Invalid checksum entry: {name}")
        if digest(path) != checksum:
            raise ValueError(f"Checksum mismatch: {path}")
        sums[name] = checksum
    config = dict(line.split("=", 1) for line in (directory / "build-config.sh").read_text().splitlines()
                  if re.match(r"^[A-Z_]+=", line))
    if config.get("ELECTRON_VERSION") != version:
        raise ValueError(f"{platform}: expected Electron {version}")
    if f"PLATFORM={platform}" not in (directory / "build-info.txt").read_text().splitlines():
        raise ValueError(f"Wrong platform: {directory}")
    suffix = PLATFORMS[platform][1]
    for name in ("build-config.sh", "build-info.txt", "binary-report.txt", "ANGLE-LICENSE.txt", "libEGL." + suffix, "libGLESv2." + suffix):
        if name not in sums:
            raise ValueError(f"Missing checksummed file: {name}")
    return config


def license_notice(directory, revision):
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("Invalid ANGLE source revision")
    text = (directory / "ANGLE-LICENSE.txt").read_text(encoding="utf-8")
    text = "\n".join(line.removeprefix("// ").removeprefix("//").rstrip() for line in text.splitlines()).strip()
    if not text:
        raise ValueError("Empty ANGLE license")
    return f"ANGLE\n=====\n\nSource: https://chromium.googlesource.com/angle/angle/+/{revision}/LICENSE\n\n{text}\n"


def replace_libraries(original, destination, replacements):
    found = set()
    with zipfile.ZipFile(original) as source, zipfile.ZipFile(destination, "w") as target:
        for info in source.infolist():
            name = Path(info.filename).name
            if name in replacements:
                if name in found:
                    raise ValueError(f"Duplicate native library: {name}")
                found.add(name)
                data = replacements[name]
            else:
                data = source.read(info)
            target.writestr(info, data)
        target.comment = source.comment
    if found != replacements.keys():
        raise ValueError(f"Missing jar entries: {replacements.keys() - found}")


def install(root, output, original_hashes):
    for name, checksum in original_hashes.items():
        if digest(root / "lib/jhv" / name) != checksum:
            raise RuntimeError("Repository jars changed during testing")
    replacements = {Path("lib/jhv") / name: output / "candidate-jars" / name for name in original_hashes}
    replacements[Path("extra/angle/angle.json")] = output / "angle.json"
    replacements[Path("extra/licenses/ANGLE.txt")] = output / "ANGLE.txt"
    # The license synchronizer can update or remove generated notices.
    notices = root / "resources/licenses"
    paths = set(replacements) | {Path("extra/licenses/licenses.json")}
    paths.update(path.relative_to(root) for path in notices.glob("*.txt"))
    backups = output / "original-files"
    backups.mkdir()
    existing = {path for path in paths if (root / path).exists()}
    for path in existing:
        (backups / path).parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(root / path, backups / path)
    try:
        for path, source in replacements.items():
            shutil.copyfile(source, root / path)
        subprocess.run([sys.executable, str(root / "extra/licenses/sync_licenses.py")], cwd=root, check=True)
    except BaseException:
        paths.update(path.relative_to(root) for path in notices.glob("*.txt"))
        for path in paths:
            if path in existing:
                shutil.copyfile(backups / path, root / path)
            else:
                (root / path).unlink(missing_ok=True)
        raise


def run_tests(jars, output):
    output.mkdir()
    with (output / "tests.log").open("w") as log:
        result = subprocess.run([sys.executable, str(ROOT / "extra/test/run_tests.py"),
                                 "--no-build", "--native-jars", str(jars),
                                 "--render-output", str(output), "opengl"],
                                cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError(f"Graphics tests failed: {output / 'tests.log'}")


def compare_images(before, after, output, tolerance):
    import numpy as np
    from PIL import Image
    names = {p.relative_to(before) for p in before.rglob("*.png")}
    if not names or names != {p.relative_to(after) for p in after.rglob("*.png")}:
        raise ValueError("Missing or mismatched rendering cases")
    rows = []
    for name in sorted(names):
        old = np.asarray(Image.open(before / name).convert("RGB"), dtype=np.int16)
        new = np.asarray(Image.open(after / name).convert("RGB"), dtype=np.int16)
        if old.shape != new.shape:
            raise ValueError(f"Image dimensions changed: {name}")
        difference = np.abs(old - new)
        maximum = int(difference.max())
        changed = int(np.count_nonzero(difference.max(axis=2) > tolerance))
        relative = Path("differences") / name
        path = output / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        Image.fromarray(np.minimum(difference * 8, 255).astype(np.uint8)).save(path)
        rows.append(dict(case=name.as_posix(), max_channel_delta=maximum,
                         pixels_over_tolerance=changed, mean_delta=float(difference.mean())))
    (output / "comparison.json").write_text(json.dumps(rows, indent=2) + "\n")
    body = ["<!doctype html><meta charset=utf-8><title>ANGLE rendering comparison</title>",
            "<style>body{font:15px sans-serif;background:#222;color:#eee}img{max-width:32%;height:auto;image-rendering:pixelated}section{border-top:1px solid #777;padding:12px}a{color:#9df}</style>",
            "<h1>ANGLE rendering comparison</h1><p>Previous / candidate / absolute difference ×8. Images are linked at native resolution.</p>"]
    for row in rows:
        name = row["case"]
        body.append(f"<section><h2>{html.escape(name)}</h2><p>Maximum channel difference: {row['max_channel_delta']}; pixels exceeding {tolerance}: {row['pixels_over_tolerance']}</p>")
        for prefix in ("before", "after", "differences"):
            src = html.escape(prefix + "/" + name, quote=True)
            body.append(f'<a href="{src}"><img src="{src}" alt="{prefix}"></a>')
        body.append("</section>")
    (output / "report.html").write_text("\n".join(body))
    return any(row["pixels_over_tolerance"] for row in rows)


def electron_reference(version, output):
    if sys.platform != "darwin" or host_platform.machine() != "arm64":
        raise ValueError("Electron reference download currently supports macOS ARM64")
    destination = Path.home() / f"electron-v{version}-darwin-arm64"
    executable = destination / "Electron.app/Contents/MacOS/Electron"
    if not executable.is_file():
        if destination.exists():
            raise ValueError(f"Incomplete Electron installation: {destination}")
        downloads = output / "electron-download"
        downloads.mkdir()
        name = f"electron-v{version}-darwin-arm64.zip"
        subprocess.run(["gh", "release", "download", "v" + version, "--repo", "electron/electron",
                        "--pattern", name, "--pattern", "SHASUMS256.txt", "--dir", str(downloads)], check=True)
        expected = next(line.split()[0] for line in (downloads / "SHASUMS256.txt").read_text().splitlines()
                        if line.split()[-1].lstrip("*") == name)
        if digest(downloads / name) != expected:
            raise ValueError("Electron checksum mismatch")
        # ditto preserves executable permissions and framework symlinks on macOS.
        subprocess.run(["ditto", "-x", "-k", str(downloads / name), str(destination)], check=True)
    reported = subprocess.check_output([str(executable), "--version"], text=True, timeout=60).strip()
    if reported != "v" + version:
        raise ValueError(f"Unexpected Electron executable version: {reported}")
    with (output / "electron-tests.log").open("w") as log:
        subprocess.run([sys.executable, str(ROOT / "extra/test/wcs/run_jhv_wcs_hpc_validation_suite.py"),
                        "--only", "electron_default_production_fragments", "electron_default_all_modes_color_diff_smoke"],
                       cwd=ROOT, env={**os.environ, "JHV_ELECTRON": str(executable)},
                       stdout=log, stderr=subprocess.STDOUT, check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version", help="Electron version, e.g. 44.3.0")
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--artifacts", type=Path, help="directory containing extracted angle-PLATFORM artifacts")
    source.add_argument("--release", help="release tag in bogdanni/angle-builds (downloads only)")
    parser.add_argument("--output", type=Path, required=True, help="new directory for staged jars and comparison report")
    parser.add_argument("--baseline-ref", default="HEAD", help="Git revision containing previous native jars")
    parser.add_argument("--pixel-tolerance", type=int, default=2, help="maximum accepted 8-bit channel difference")
    parser.add_argument("--electron-reference", action="store_true", help="also download/use matching macOS ARM Electron and run WCS reference tests")
    parser.add_argument("--install", action="store_true", help="replace jars only if tests and comparisons pass")
    args = parser.parse_args()
    if not re.fullmatch(r"\d+\.\d+\.\d+", args.version) or not 0 <= args.pixel_tolerance <= 255:
        parser.error("Invalid version or pixel tolerance")
    # Require comparison dependencies before modifying or downloading anything.
    import numpy
    import PIL
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    artifacts = args.artifacts.resolve() if args.artifacts else output / "downloads"
    if args.release:
        artifacts.mkdir()
        subprocess.run(["gh", "release", "download", args.release, "--repo", "bogdanni/angle-builds",
                        "--pattern", "angle-*", "--dir", str(artifacts)], check=True)
        for platform in PLATFORMS:
            destination = artifacts / ("angle-" + platform)
            destination.mkdir()
            if platform == "windows-x64":
                with zipfile.ZipFile(artifacts / ("angle-" + platform + ".zip")) as archive:
                    for name in archive.namelist():
                        if not (destination / name).resolve().is_relative_to(destination.resolve()):
                            raise ValueError("Unsafe archive path")
                    archive.extractall(destination)
            else:
                with tarfile.open(artifacts / ("angle-" + platform + ".tar.gz")) as archive:
                    archive.extractall(destination, filter="data")
    baseline = output / "baseline-jars"
    staged = output / "candidate-jars"
    baseline.mkdir()
    staged.mkdir()
    configs = []
    notices = []
    baseline_ref = subprocess.check_output(["git", "rev-parse", "--verify", args.baseline_ref + "^{commit}"], cwd=ROOT, text=True).strip()
    (output / "baseline.txt").write_text(baseline_ref + "\n")
    manifest = dict(electron_version=args.version, binaries={})
    original_hashes = {}
    for platform, (jar_platform, suffix) in PLATFORMS.items():
        directory = artifacts / ("angle-" + platform)
        configs.append(artifact(directory, platform, args.version))
        notices.append(license_notice(directory, configs[-1].get("ANGLE_SHA", "")))
        name = "jhv-natives-" + jar_platform + ".jar"
        original = ROOT / "lib/jhv" / name
        original_hashes[name] = digest(original)
        previous = subprocess.check_output(["git", "show", baseline_ref + ":lib/jhv/" + name], cwd=ROOT)
        libraries = {lib: (directory / lib).read_bytes() for lib in ("libEGL." + suffix, "libGLESv2." + suffix)}
        with zipfile.ZipFile(io.BytesIO(previous)) as archive:
            old_libraries = {Path(entry.filename).name: archive.read(entry) for entry in archive.infolist()
                             if Path(entry.filename).name in libraries}
        if old_libraries.keys() != libraries.keys():
            raise ValueError(f"Missing ANGLE libraries in baseline: {name}")
        replace_libraries(original, baseline / name, old_libraries)
        replace_libraries(original, staged / name, libraries)
        manifest["binaries"][platform] = {lib: digest(directory / lib) for lib in ("libEGL." + suffix, "libGLESv2." + suffix)}
    if any(config != configs[0] for config in configs[1:]):
        raise ValueError("Artifacts have different source/build pins")
    if any(notice != notices[0] for notice in notices[1:]):
        raise ValueError("Artifacts have different ANGLE licenses")
    (output / "ANGLE.txt").write_text(notices[0], encoding="utf-8")
    manifest["build"] = configs[0]
    (output / "angle.json").write_text(json.dumps(manifest, indent=2) + "\n")
    subprocess.run(["ant", "compile"], cwd=ROOT, check=True)
    subprocess.run([sys.executable, str(ROOT / "extra/test/run_tests.py"), "shaders"], cwd=ROOT, check=True)
    print("Testing previous libraries", flush=True)
    run_tests(baseline, output / "before")
    print("Testing candidate libraries", flush=True)
    run_tests(staged, output / "after")
    changed = compare_images(output / "before", output / "after", output, args.pixel_tolerance)
    print("Rendering report:", output / "report.html")
    if changed:
        print("Rendering differences require review. Repository jars were not changed.")
        return 1
    if args.electron_reference:
        electron_reference(args.version, output)
    if args.install:
        install(ROOT, output, original_hashes)
    print("Graphics assertions and rendering comparison passed." + (" Jars installed." if args.install else " Staged jars retained for review."))
    return 0


if __name__ == "__main__":
    sys.exit(main())
