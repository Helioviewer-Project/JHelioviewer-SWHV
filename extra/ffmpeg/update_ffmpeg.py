#!/usr/bin/env python3
"""Update bundled FFmpeg to the providers' latest stable prebuilt releases.

Python 3.11+, stdlib only. Run without arguments to update. ffmpeg.json is the
generated record, not a configuration file to prepare by hand. Use --list to
preview available builds or --check to verify installed files offline.
No source archives are downloaded, and nothing is compiled or executed.
"""

import argparse
import copy
from datetime import datetime, timezone
from functools import cache
import hashlib
import json
from pathlib import Path
import re
import shutil
import tarfile
from tempfile import TemporaryDirectory
import time
from urllib.error import URLError
from urllib.parse import urlencode, urlsplit
from urllib.request import Request, urlopen
import zipfile

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = Path(__file__).with_name("ffmpeg.json")
JARS = {
    "macos-arm64": "jhv-natives-macos-arm64.jar",
    "macos-amd64": "jhv-natives-macos.jar",
    "linux-amd64": "jhv-natives-linux.jar",
    "windows-amd64": "jhv-natives-windows.jar",
}
REQUIRED_FLAGS = {
    b"--enable-gpl",
    b"--enable-version3",
    b"--enable-libx264",
    b"--enable-libx265",
}
RIEDL = "https://ffmpeg.martin-riedl.de"
GITHUB = "https://api.github.com/repos"


def request(url):
    try:
        return urlopen(
            Request(url, headers={"User-Agent": "JHV-FFmpeg-Updater"}),
            timeout=60,
        )
    except URLError as error:
        raise ValueError(f"{url}: {error}") from error


def fetch_json(url):
    with request(url) as response:
        return json.load(response)


@cache
def github_revision(repository, ref):
    return fetch_json(f"{GITHUB}/{repository}/commits/{ref}")["sha"]


def discover_macos(arch, page):
    # Numeric release versions exclude the snapshot links on the same page.
    matches = set(
        re.findall(
            rf"""(/download/macos/{arch}/(\d+)_(\d+(?:\.\d+)+)/ffmpeg\.zip)["']""",
            page,
        )
    )
    if len(matches) != 1:
        raise ValueError(f"Expected one stable macOS {arch} download on {RIEDL}")
    path, timestamp, version = matches.pop()
    url = RIEDL + path
    info_url = url.rsplit("/", 1)[0] + "/versions.txt"
    with request(info_url) as response:
        info = response.read().decode("utf-8")
    if not info.startswith(f"ffmpeg version {version}-"):
        raise ValueError(f"Release/version mismatch: {info_url}")
    with request(url + ".sha256") as response:
        fields = response.read().decode("ascii").split()
    if not fields or not re.fullmatch(r"[0-9a-f]{64}", fields[0]):
        raise ValueError(f"Invalid checksum for {url}")
    checksum = fields[0]
    built = datetime.fromtimestamp(int(timestamp), timezone.utc)
    query = urlencode({"until": built.isoformat(), "limit": 1})
    # Riedl does not report the recipe revision in the binary. Record the last
    # published recipe commit at build time, not a claimed exact build revision.
    commits = fetch_json(
        f"https://git.martin-riedl.de/api/v1/repos/ffmpeg/build-script/commits?{query}"
    )
    if not commits:
        raise ValueError(
            f"No published build recipe found for macOS {arch} at {built.isoformat()}"
        )
    recipes = commits[0]["sha"]
    return {
        "provider": "Martin Riedl",
        "version": version,
        "source_revision": github_revision("FFmpeg/FFmpeg", f"n{version}"),
        "recipes_url": f"https://git.martin-riedl.de/ffmpeg/build-script/src/commit/{recipes}",
        "build_info_url": info_url,
        "url": url,
        "archive_sha256": checksum,
        "member": "ffmpeg",
    }, built.year


def select_btbn_assets(release):
    branches = {}
    pattern = r"ffmpeg-(n\d+(?:\.\d+)+(?:-\d+-g[0-9a-f]+)?)-(linux64|win64)-gpl-(\d+(?:\.\d+)*)\.(tar\.xz|zip)"
    for asset in release["assets"]:
        match = re.fullmatch(pattern, asset["name"])
        if match:
            version, target, branch, extension = match.groups()
            if extension != ("tar.xz" if target == "linux64" else "zip"):
                continue
            key = tuple(map(int, branch.split(".")))
            branches.setdefault(key, {})[target] = (asset, version, extension)
    if not branches:
        raise ValueError("BtbN release contains no stable standalone GPL builds")
    selected = branches[max(branches)]
    if set(selected) != {"linux64", "win64"}:
        raise ValueError(
            "BtbN's newest stable branch is missing a platform, try again later"
        )
    if selected["linux64"][1] != selected["win64"][1]:
        raise ValueError(
            "BtbN platform revisions differ, try again after the release finishes"
        )
    return selected


def discover_releases():
    print("Checking current stable prebuilt releases...", flush=True)
    builds = {}
    years = []
    with request(RIEDL + "/") as response:
        page = response.read().decode("utf-8")
    for arch in ("arm64", "amd64"):
        builds[f"macos-{arch}"], year = discover_macos(arch, page)
        years.append(year)
    releases = fetch_json(f"{GITHUB}/BtbN/FFmpeg-Builds/releases?per_page=5")
    dated = [
        r
        for r in releases
        if not r["draft"]
        and not r["prerelease"]
        and re.fullmatch(r"autobuild-\d{4}-\d{2}-\d{2}-\d{2}-\d{2}", r["tag_name"])
    ]
    if not dated:
        raise ValueError("No dated BtbN release was found")
    release = max(dated, key=lambda r: r["tag_name"])
    tag = release["tag_name"]
    recipes = github_revision("BtbN/FFmpeg-Builds", tag)
    for target, (asset, version, extension) in select_btbn_assets(release).items():
        digest = asset.get("digest") or ""
        if not re.fullmatch(r"sha256:[0-9a-f]{64}", digest):
            raise ValueError(
                f"GitHub did not supply a SHA-256 digest for {asset['name']}"
            )
        revision = version.rsplit("-g", 1)[-1] if "-g" in version else version
        platform = "linux-amd64" if target == "linux64" else "windows-amd64"
        directory = asset["name"].removesuffix("." + extension)
        builds[platform] = {
            "provider": "BtbN",
            "version": version + "-" + tag[10:20].replace("-", ""),
            "source_revision": github_revision("FFmpeg/FFmpeg", revision),
            "recipes_url": f"https://github.com/BtbN/FFmpeg-Builds/tree/{recipes}",
            "build_info_url": release["html_url"],
            "url": asset["browser_download_url"],
            "archive_sha256": digest.removeprefix("sha256:"),
            "member": directory
            + ("/bin/ffmpeg" if target == "linux64" else "/bin/ffmpeg.exe"),
        }
    years.append(int(tag[10:14]))
    return {"copyright_year": max(years), "builds": builds}


def reuse_recorded_hashes(discovered, previous):
    # A matching archive identifies the same reviewed binary. Its saved hash
    # lets us verify the installed copy without downloading the archive again.
    for platform, build in discovered["builds"].items():
        old = previous["builds"].get(platform, {})
        if old and version_key(build["version"]) < version_key(old["version"]):
            raise ValueError(
                f"{platform}: provider offers an older version than the installed record, refusing downgrade"
            )
        if all(
            build[key] == old.get(key) for key in ("url", "archive_sha256", "member")
        ):
            build["binary_sha256"] = old["binary_sha256"]


def version_key(version):
    match = re.match(r"n?(\d+)\.(\d+)(?:\.(\d+))?(?:-(\d+)-g[0-9a-f]+)?", version)
    if not match:
        raise ValueError(f"Unrecognized FFmpeg version: {version}")
    return tuple(int(part or 0) for part in match.groups())


def load_manifest(path):
    manifest = json.loads(path.read_text(encoding="utf-8"))
    if set(manifest["builds"]) != set(JARS):
        raise ValueError("Manifest must contain exactly the four supported platforms")
    for platform, build in manifest["builds"].items():
        for field, length in (
            ("archive_sha256", 64),
            ("binary_sha256", 64),
            ("source_revision", 40),
        ):
            if not re.fullmatch(f"[0-9a-f]{{{length}}}", build[field]):
                raise ValueError(f"{platform}: invalid {field}")
        for field in ("url", "recipes_url", "build_info_url"):
            url = urlsplit(build[field])
            if url.scheme != "https" or not url.netloc:
                raise ValueError(f"{platform}: {field} must be an HTTPS URL")
        if not urlsplit(build["url"]).path.endswith((".zip", ".tar.xz")):
            raise ValueError(f"{platform}: expected a .zip or .tar.xz binary archive")
    return manifest


def notice_text(manifest):
    text = f"""FFmpeg
======

Generated by extra/ffmpeg/update_ffmpeg.py. Build records are in extra/ffmpeg/ffmpeg.json.

Copyright (c) 2000-{manifest['copyright_year']} the FFmpeg developers
https://ffmpeg.org/

JHelioviewer uses an unmodified, separately executed FFmpeg binary for movie
and PNG export. These builds include libx264 and libx265 and are licensed
under the GNU General Public License, version 3 or later. The license is
included in GPL-3.0.txt and is available from JHelioviewer's About dialog.
FFmpeg is supplied without warranty, including any implied warranty of
merchantability or fitness for a particular purpose.

The provider's build recipes describe the included libraries as well as
FFmpeg itself. JHelioviewer's H.264 and H.265 exports use:
  x264: https://www.videolan.org/developers/x264.html
  x265: https://www.videolan.org/developers/x265.html
"""
    for platform in JARS:
        build = manifest["builds"][platform]
        text += f"""
{platform}
{'-' * len(platform)}
Provider: {build['provider']}
Version: {build['version']}
FFmpeg revision: {build['source_revision']}
FFmpeg source:
  https://github.com/FFmpeg/FFmpeg/tree/{build['source_revision']}
Published build recipes:
  {build['recipes_url']}
Build information:
  {build['build_info_url']}
Binary archive:
  {build['url']}
Archive SHA-256:
  {build['archive_sha256']}
Executable SHA-256:
  {build['binary_sha256']}
"""
    return text


def download(build, destination):
    print(f"Downloading {build['url']}", flush=True)
    with request(build["url"]) as response, destination.open("wb") as output:
        shutil.copyfileobj(response, output)
    with destination.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    if digest != build["archive_sha256"]:
        raise ValueError(f"Archive SHA-256 mismatch: {build['url']}")


def read_binary(archive, build):
    # Read only the named member. Never unpack archive paths onto the filesystem.
    if urlsplit(build["url"]).path.endswith(".zip"):
        with zipfile.ZipFile(archive) as source:
            return source.read(build["member"])
    with tarfile.open(archive, "r:xz") as source:
        member = source.getmember(build["member"])
        if not member.isfile():
            raise ValueError(f"Not a regular file: {member.name}")
        with source.extractfile(member) as stream:
            return stream.read()


def verify_binary(binary, build):
    digest = hashlib.sha256(binary).hexdigest()
    if build.get("binary_sha256") is not None and digest != build["binary_sha256"]:
        raise ValueError("Executable SHA-256 mismatch")
    # These providers embed the FFmpeg configure command as a NUL-terminated
    # string. Inspect it without running a foreign-architecture executable.
    configurations = [
        set(value.split()) for value in re.findall(rb"--prefix=[^\0\r\n]+", binary)
    ]
    if any(
        b"--enable-nonfree" in flags or b"--enable-shared" in flags
        for flags in configurations
    ):
        raise ValueError("Refusing a nonfree or shared-library build")
    if not any(REQUIRED_FLAGS <= flags for flags in configurations):
        raise ValueError("Expected GPLv3 build with libx264 and libx265 was not found")
    return digest


def replace_entry(jar, candidate, target, binary):
    with zipfile.ZipFile(jar) as source, zipfile.ZipFile(
        candidate, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9
    ) as output:
        if source.namelist().count(target) != 1:
            raise ValueError(f"{jar}: expected one {target} entry")
        output.comment = source.comment
        for entry in source.infolist():
            info = copy.copy(entry)
            if entry.filename == target:
                info.date_time = time.localtime()[:6]
                output.writestr(
                    info, binary, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9
                )
            else:
                output.writestr(info, source.read(entry))
    with zipfile.ZipFile(jar) as before, zipfile.ZipFile(candidate) as after:
        if before.namelist() != after.namelist():
            raise ValueError(f"{jar}: JAR entries changed")
        for entry in before.infolist():
            expected = binary if entry.filename == target else before.read(entry)
            if after.read(entry.filename) != expected:
                raise ValueError(f"{jar}: verification failed for {entry.filename}")
    shutil.copymode(jar, candidate)


def update(root, manifest, check=False):
    notice = root / "resources/licenses/FFmpeg-Notices.txt"
    if not notice.with_name("GPL-3.0.txt").is_file():
        raise ValueError("Bundled GPL-3.0.txt license is missing")
    pending = []
    for platform, filename in JARS.items():
        build = manifest["builds"][platform]
        jar = root / "lib/jhv" / filename
        with zipfile.ZipFile(jar) as archive:
            current = archive.read(f"jhv/{platform}/ffmpeg")
        matches = hashlib.sha256(current).hexdigest() == build.get("binary_sha256")
        if matches:
            verify_binary(current, build)
            print(f"{platform}: already matches {build['version']}")
        else:
            pending.append((platform, jar))
        del current
    if check:
        mismatches = [platform for platform, _ in pending]
        if not notice.exists() or notice.read_bytes() != notice_text(manifest).encode(
            "utf-8"
        ):
            mismatches.append("FFmpeg-Notices.txt")
        if mismatches:
            raise ValueError("Does not match manifest: " + ", ".join(mismatches))
        print("Manifest check passed.")
        return

    replacements = []
    # Keep staged JARs on the repository filesystem for atomic file replacement.
    # Downloads and all candidate checks finish before any installed file changes.
    with TemporaryDirectory(prefix=".ffmpeg-update-", dir=root) as directory:
        staging = Path(directory)
        for platform, jar in pending:
            build = manifest["builds"][platform]
            target = f"jhv/{platform}/ffmpeg"
            archive = staging / f"{platform}.archive"
            download(build, archive)
            binary = read_binary(archive, build)
            build["binary_sha256"] = verify_binary(binary, build)
            candidate = staging / jar.name
            replace_entry(jar, candidate, target, binary)
            del binary
            replacements.append((candidate, jar))
        generated = {
            notice: notice_text(manifest),
            root / "extra/ffmpeg/ffmpeg.json": json.dumps(manifest, indent=2) + "\n",
        }
        for destination, text in generated.items():
            data = text.encode("utf-8")
            if not destination.exists() or destination.read_bytes() != data:
                candidate = staging / destination.name
                candidate.write_bytes(data)
                replacements.append((candidate, destination))
        for candidate, destination in replacements:
            candidate.replace(destination)
            print(f"Updated {destination.relative_to(root)}")
    print("FFmpeg update complete. Review the diff before committing.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument(
        "--check",
        action="store_true",
        help="Check installed binaries and notice without downloading or changing files",
    )
    mode.add_argument(
        "--list",
        action="store_true",
        help="Show available stable builds without downloading binaries or changing files",
    )
    args = parser.parse_args()
    try:
        if args.check:
            update(ROOT, load_manifest(MANIFEST), check=True)
            return
        manifest = discover_releases()
        for platform, build in manifest["builds"].items():
            print(f"{platform}: {build['provider']} {build['version']}")
        if args.list:
            return
        if MANIFEST.exists():
            reuse_recorded_hashes(manifest, load_manifest(MANIFEST))
        update(ROOT, manifest)
    except (
        OSError,
        ValueError,
        KeyError,
        zipfile.BadZipFile,
        tarfile.TarError,
    ) as error:
        parser.exit(1, f"FFmpeg update failed: {error}\n")


if __name__ == "__main__":
    main()
