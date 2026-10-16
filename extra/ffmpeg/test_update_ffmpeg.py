#!/usr/bin/env python3
"""Offline tests. Synthetic archives are never executed."""

import copy
import hashlib
import io
import json
from pathlib import Path
import sys
import tarfile
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent))
import update_ffmpeg as updater

CONFIG = (
    b"--extra-version=Jellyfin --enable-gpl --enable-version3 --enable-libx264 --enable-libx265 --prefix=/build"
)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


class UpdateFFmpegTest(unittest.TestCase):
    def setUp(self):
        self.directory = TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        (self.root / "lib/jhv").mkdir(parents=True)
        (self.root / "extra/ffmpeg").mkdir(parents=True)
        self.record = self.root / "extra/ffmpeg/ffmpeg.json"
        self.notice = self.root / "resources/licenses/FFmpeg-Notices.txt"
        self.notice.parent.mkdir(parents=True)
        self.notice.write_text("old notice\n")
        self.license = self.notice.with_name("GPL-3.0.txt")
        self.license.write_text("license retained unchanged\n")
        self.manifest = {
            "version": "8.1.3-1", "source_revision": "a" * 40,
            "release_url": updater.SOURCE + "/releases/tag/v8.1.3-1",
            "copyright_year": 2026, "builds": {},
        }
        self.archives = {}
        self.binaries = {}
        self.original_files = {}
        for platform in updater.PLATFORMS:
            binary = platform.encode() + b"\0" + CONFIG + b"\0"
            self.binaries[platform] = binary
            member = "ffmpeg.exe" if platform == "windows-amd64" else "ffmpeg"
            archive = io.BytesIO()
            if platform != "windows-amd64":
                suffix = ".tar.xz"
                with tarfile.open(fileobj=archive, mode="w:xz") as output:
                    info = tarfile.TarInfo(member)
                    info.size = len(binary)
                    output.addfile(info, io.BytesIO(binary))
            else:
                suffix = ".zip"
                with zipfile.ZipFile(archive, "w") as output:
                    output.writestr(member, binary)
                    output.writestr("../must-not-be-extracted", b"unrelated member")
            url = updater.SOURCE + f"/releases/download/v8.1.3-1/{platform}{suffix}"
            self.archives[url] = archive.getvalue()
            self.manifest["builds"][platform] = {
                "url": url,
                "archive_sha256": sha256(archive.getvalue()),
                "member": member,
                "binary_sha256": sha256(binary),
            }
            directory = self.root / "lib/jhv" / platform
            directory.mkdir()
            executable = directory / "ffmpeg"
            executable.write_bytes(b"old binary")
            executable.chmod(0o755)
            unrelated = directory / "library.so"
            unrelated.write_bytes(b"keep native library")
            self.original_files[executable] = executable.read_bytes()
            self.original_files[unrelated] = unrelated.read_bytes()
        self.network = patch.object(updater, "urlopen", side_effect=self.open_url)
        self.urlopen = self.network.start()
        self.addCleanup(self.network.stop)
        self.stdout = patch("sys.stdout", new_callable=io.StringIO)
        self.stdout.start()
        self.addCleanup(self.stdout.stop)

    def open_url(self, request, timeout):
        return io.BytesIO(self.archives[request.full_url])

    def assert_unchanged(self):
        for path, original in self.original_files.items():
            self.assertEqual(path.read_bytes(), original)
        self.assertEqual(self.notice.read_text(), "old notice\n")
        self.assertFalse(self.record.exists())
        self.assertEqual(list(self.root.glob(".ffmpeg-update-*")), [])

    def test_updates_zip_and_tar_preserving_other_files(self):
        updater.update(self.root, self.manifest)
        self.assertEqual(self.urlopen.call_count, 4)
        for platform in updater.PLATFORMS:
            directory = self.root / "lib/jhv" / platform
            self.assertEqual((directory / "ffmpeg").read_bytes(), self.binaries[platform])
            self.assertEqual((directory / "ffmpeg").stat().st_mode & 0o777, 0o755)
            self.assertEqual((directory / "library.so").read_bytes(), b"keep native library")
        self.assertEqual(self.notice.read_text(), updater.notice_text(self.manifest))
        self.assertIn("included in GPL-3.0.txt", self.notice.read_text())
        self.assertEqual(json.loads(self.record.read_text()), self.manifest)
        self.assertEqual(self.license.read_text(), "license retained unchanged\n")
        self.assertFalse((self.root / "must-not-be-extracted").exists())
        self.assertEqual(list(self.root.glob(".ffmpeg-update-*")), [])

    def test_missing_shared_license_changes_nothing(self):
        self.license.unlink()
        with self.assertRaisesRegex(ValueError, r"Bundled GPL-3\.0\.txt license is missing"):
            updater.update(self.root, self.manifest)
        self.urlopen.assert_not_called()
        self.assert_unchanged()

    def test_repeat_update_is_byte_identical_and_offline(self):
        updater.update(self.root, self.manifest)
        before = {path: path.read_bytes() for path in self.original_files}
        notice_time = self.notice.stat().st_mtime_ns
        self.urlopen.reset_mock()
        updater.update(self.root, self.manifest)
        self.urlopen.assert_not_called()
        for path, data in before.items():
            self.assertEqual(path.read_bytes(), data)
        self.assertEqual(self.notice.stat().st_mtime_ns, notice_time)

    def test_first_update_generates_record_without_input_file_or_binary_hashes(self):
        for build in self.manifest["builds"].values():
            del build["binary_sha256"]
        self.assertFalse(self.record.exists())
        with patch.object(updater, "ROOT", self.root), patch.object(
            updater, "MANIFEST", self.record
        ), patch.object(
            updater, "discover_releases", return_value=self.manifest
        ), patch.object(
            sys, "argv", ["update_ffmpeg.py"]
        ):
            updater.main()
        record = updater.load_manifest(self.record)
        for platform, binary in self.binaries.items():
            self.assertEqual(
                record["builds"][platform]["binary_sha256"], sha256(binary)
            )

    def test_preview_does_not_download_or_modify_files(self):
        with patch.object(
            updater, "discover_releases", return_value=self.manifest
        ), patch.object(updater, "update") as update, patch.object(
            sys, "argv", ["update_ffmpeg.py", "--list"]
        ):
            updater.main()
        update.assert_not_called()
        self.urlopen.assert_not_called()
        self.assert_unchanged()

    def test_reuses_hash_only_for_same_archive(self):
        discovered = copy.deepcopy(self.manifest)
        for build in discovered["builds"].values():
            del build["binary_sha256"]
        discovered["builds"]["windows-amd64"]["archive_sha256"] = "0" * 64
        updater.reuse_recorded_hashes(discovered, self.manifest)
        self.assertNotIn("binary_sha256", discovered["builds"]["windows-amd64"])
        self.assertEqual(
            discovered["builds"]["linux-amd64"]["binary_sha256"],
            self.manifest["builds"]["linux-amd64"]["binary_sha256"],
        )

    def test_refuses_downgrade(self):
        discovered = copy.deepcopy(self.manifest)
        for version in ("8.1.2-5", "8.1.3-0"):
            discovered["version"] = version
            with self.subTest(version=version), self.assertRaisesRegex(ValueError, "downgrade"):
                updater.reuse_recorded_hashes(discovered, self.manifest)
        self.assertLess(updater.version_key("8.1.3-2"), updater.version_key("8.1.3-11"))

    def test_check_is_offline_and_never_creates_staging_directory(self):
        with patch.object(
            updater,
            "TemporaryDirectory",
            side_effect=AssertionError("check must not stage"),
        ):
            with self.assertRaisesRegex(ValueError, "Does not match manifest"):
                updater.update(self.root, self.manifest, check=True)
        self.urlopen.assert_not_called()
        self.assert_unchanged()

    def test_check_matches_after_update_and_detects_stale_notice(self):
        updater.update(self.root, self.manifest)
        self.urlopen.reset_mock()
        updater.update(self.root, self.manifest, check=True)
        self.notice.write_text("stale")
        with self.assertRaisesRegex(ValueError, r"FFmpeg-Notices\.txt"):
            updater.update(self.root, self.manifest, check=True)
        self.urlopen.assert_not_called()
        self.assertEqual(self.notice.read_text(), "stale")

    def test_late_archive_checksum_failure_changes_nothing(self):
        build = self.manifest["builds"]["windows-amd64"]
        self.archives[build["url"]] += b"corruption"
        with self.assertRaisesRegex(ValueError, "Archive SHA-256 mismatch"):
            updater.update(self.root, self.manifest)
        self.assertEqual(self.urlopen.call_count, 4)
        self.assert_unchanged()

    def test_binary_checksum_failure_changes_nothing(self):
        self.manifest["builds"]["macos-arm64"]["binary_sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "Executable SHA-256 mismatch"):
            updater.update(self.root, self.manifest)
        self.assert_unchanged()

    def test_missing_archive_member_changes_nothing(self):
        self.manifest["builds"]["macos-arm64"]["member"] = "not-present"
        with self.assertRaises(KeyError):
            updater.update(self.root, self.manifest)
        self.assert_unchanged()

    def test_network_failure_changes_nothing(self):
        self.urlopen.side_effect = OSError("network down")
        with self.assertRaisesRegex(OSError, "network down"):
            updater.update(self.root, self.manifest)
        self.assert_unchanged()

    def test_rejects_nonfree_shared_and_missing_codecs(self):
        configurations = [
            CONFIG + b" --enable-nonfree",
            CONFIG + b" --enable-shared",
            CONFIG.replace(b"--enable-libx264", b"--disable-libx264"),
            CONFIG.replace(b"--enable-libx265", b"--disable-libx265"),
            CONFIG.replace(b"--enable-version3", b""),
            b"not an FFmpeg configuration",
        ]
        for configuration in configurations:
            binary = configuration + b"\0"
            with self.subTest(configuration=configuration), self.assertRaises(
                ValueError
            ):
                updater.verify_binary(binary, {"binary_sha256": sha256(binary)})

    def test_rejects_tar_symlink(self):
        archive = self.root / "symlink.tar.xz"
        with tarfile.open(archive, "w:xz") as output:
            info = tarfile.TarInfo("ffmpeg")
            info.type = tarfile.SYMTYPE
            info.linkname = "/etc/passwd"
            output.addfile(info)
        with self.assertRaisesRegex(ValueError, "Not a regular file"):
            updater.read_binary(
                archive, {"url": "https://example.org/test.tar.xz", "member": "ffmpeg"}
            )

    def test_manifest_validation(self):
        path = self.root / "ffmpeg.json"
        path.write_text(json.dumps(self.manifest))
        self.assertEqual(updater.load_manifest(path), self.manifest)
        for field, value in (
            ("archive_sha256", "bad"),
            ("url", "http://example.org/ffmpeg.zip"),
        ):
            manifest = copy.deepcopy(self.manifest)
            manifest["builds"]["macos-arm64"][field] = value
            path.write_text(json.dumps(manifest))
            with self.subTest(field=field), self.assertRaises(ValueError):
                updater.load_manifest(path)
        for field, value in (("source_revision", "main"), ("version", "latest"), ("release_url", "http://example.org")):
            manifest = copy.deepcopy(self.manifest)
            manifest[field] = value
            path.write_text(json.dumps(manifest))
            with self.subTest(field=field), self.assertRaises(ValueError):
                updater.load_manifest(path)
        del self.manifest["builds"]["macos-arm64"]
        path.write_text(json.dumps(self.manifest))
        with self.assertRaisesRegex(ValueError, "exactly the four"):
            updater.load_manifest(path)


class DiscoveryTest(unittest.TestCase):
    def setUp(self):
        version = "8.1.3-1"
        self.release = {
            "tag_name": "v" + version,
            "html_url": updater.SOURCE + "/releases/tag/v" + version,
            "published_at": "2026-09-27T14:06:00Z",
            "draft": False, "prerelease": False, "assets": [],
        }
        for platform, target in updater.PLATFORMS.items():
            suffix = "zip" if platform == "windows-amd64" else "tar.xz"
            name = f"jellyfin-ffmpeg_{version}_portable_{target}-gpl.{suffix}"
            self.release["assets"].append({
                "name": name, "digest": "sha256:" + "a" * 64,
                "browser_download_url": updater.SOURCE + "/releases/download/v" + version + "/" + name,
            })

    def discover(self):
        with patch.object(updater, "fetch_json", side_effect=[self.release, {"sha": "b" * 40}]):
            return updater.discover_releases()

    def test_selects_one_complete_portable_gpl_release(self):
        self.release["assets"].append({"name": "shared-or-nonfree.zip"})
        manifest = self.discover()
        self.assertEqual(manifest["version"], "8.1.3-1")
        self.assertEqual(manifest["source_revision"], "b" * 40)
        self.assertEqual(manifest["release_url"], self.release["html_url"])
        self.assertEqual(manifest["copyright_year"], 2026)
        self.assertEqual(set(manifest["builds"]), set(updater.PLATFORMS))
        for platform, build in manifest["builds"].items():
            self.assertEqual(build["archive_sha256"], "a" * 64)
            self.assertEqual(build["member"], "ffmpeg.exe" if platform == "windows-amd64" else "ffmpeg")
            self.assertNotIn("binary_sha256", build)

    def test_missing_platform_is_rejected(self):
        self.release["assets"].pop()
        with self.assertRaisesRegex(ValueError, "missing a platform"):
            self.discover()

    def test_missing_or_invalid_checksum_is_rejected(self):
        for digest in (None, "", "sha256:invalid"):
            self.release["assets"][0]["digest"] = digest
            with self.subTest(digest=digest), self.assertRaisesRegex(ValueError, "SHA-256"):
                self.discover()

    def test_unstable_release_is_rejected(self):
        for field in ("draft", "prerelease"):
            self.release[field] = True
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, "stable"):
                self.discover()
            self.release[field] = False
        self.release["tag_name"] = "v9.0-rc1"
        with self.assertRaisesRegex(ValueError, "Unrecognized"):
            self.discover()


if __name__ == "__main__":
    unittest.main()
