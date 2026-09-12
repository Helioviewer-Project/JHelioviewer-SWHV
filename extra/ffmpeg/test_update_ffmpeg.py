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
    b"--prefix=/build --enable-gpl --enable-version3 --enable-libx264 --enable-libx265"
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
        self.manifest = {"copyright_year": 2026, "builds": {}}
        self.archives = {}
        self.binaries = {}
        self.original_jars = {}
        for platform, filename in updater.JARS.items():
            binary = platform.encode() + b"\0" + CONFIG + b"\0"
            self.binaries[platform] = binary
            member = "package/bin/ffmpeg"
            archive = io.BytesIO()
            if platform == "linux-amd64":
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
            url = f"https://example.org/{platform}{suffix}"
            self.archives[url] = archive.getvalue()
            self.manifest["builds"][platform] = {
                "provider": "Test provider",
                "version": "9.0.1",
                "source_revision": "a" * 40,
                "recipes_url": "https://example.org/recipes/commit",
                "build_info_url": "https://example.org/versions.txt",
                "url": url,
                "archive_sha256": sha256(archive.getvalue()),
                "member": member,
                "binary_sha256": sha256(binary),
            }
            jar = self.root / "lib/jhv" / filename
            with zipfile.ZipFile(jar, "w") as output:
                output.comment = b"keep archive comment"
                info = zipfile.ZipInfo("unrelated/native", (2020, 1, 2, 3, 4, 6))
                info.external_attr = 0o100755 << 16
                info.comment = b"keep entry comment"
                output.writestr(info, b"keep native library")
                output.writestr("META-INF/MANIFEST.MF", b"Manifest-Version: 1.0\n")
                output.writestr(f"jhv/{platform}/ffmpeg", b"old binary")
            self.original_jars[jar] = jar.read_bytes()
        self.network = patch.object(updater, "urlopen", side_effect=self.open_url)
        self.urlopen = self.network.start()
        self.addCleanup(self.network.stop)
        self.stdout = patch("sys.stdout", new_callable=io.StringIO)
        self.stdout.start()
        self.addCleanup(self.stdout.stop)

    def open_url(self, request, timeout):
        return io.BytesIO(self.archives[request.full_url])

    def assert_unchanged(self):
        for jar, original in self.original_jars.items():
            self.assertEqual(jar.read_bytes(), original)
        self.assertEqual(self.notice.read_text(), "old notice\n")
        self.assertFalse(self.record.exists())
        self.assertEqual(list(self.root.glob(".ffmpeg-update-*")), [])

    def test_updates_zip_and_tar_preserving_other_entries(self):
        updater.update(self.root, self.manifest)
        self.assertEqual(self.urlopen.call_count, 4)
        for platform, filename in updater.JARS.items():
            jar = self.root / "lib/jhv" / filename
            with zipfile.ZipFile(jar) as after, zipfile.ZipFile(
                io.BytesIO(self.original_jars[jar])
            ) as before:
                self.assertEqual(after.namelist(), before.namelist())
                self.assertEqual(after.comment, before.comment)
                for name in before.namelist():
                    if name.endswith("/ffmpeg"):
                        self.assertEqual(after.read(name), self.binaries[platform])
                    else:
                        self.assertEqual(after.read(name), before.read(name))
                        for attribute in (
                            "date_time",
                            "external_attr",
                            "comment",
                            "compress_type",
                        ):
                            self.assertEqual(
                                getattr(after.getinfo(name), attribute),
                                getattr(before.getinfo(name), attribute),
                            )
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
        before = {path: path.read_bytes() for path in self.original_jars}
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
        for version in ("8.1.2", "9.0"):
            discovered["builds"]["windows-amd64"]["version"] = version
            with self.subTest(version=version), self.assertRaisesRegex(
                ValueError, "downgrade"
            ):
                updater.reuse_recorded_hashes(discovered, self.manifest)
        self.assertLess(
            updater.version_key("n9.0.1-2-gabcdef"),
            updater.version_key("n9.0.1-11-gabcdef"),
        )

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

    def test_rejects_missing_jar_entry(self):
        jar = next(iter(self.original_jars))
        with self.assertRaisesRegex(ValueError, "expected one"):
            updater.replace_entry(
                jar, self.root / "candidate.jar", "wrong/ffmpeg", b"unused"
            )
        self.assertEqual(jar.read_bytes(), self.original_jars[jar])

    def test_manifest_validation(self):
        path = self.root / "ffmpeg.json"
        path.write_text(json.dumps(self.manifest))
        self.assertEqual(updater.load_manifest(path), self.manifest)
        for field, value in (
            ("archive_sha256", "bad"),
            ("source_revision", "main"),
            ("url", "http://example.org/ffmpeg.zip"),
        ):
            manifest = copy.deepcopy(self.manifest)
            manifest["builds"]["macos-arm64"][field] = value
            path.write_text(json.dumps(manifest))
            with self.subTest(field=field), self.assertRaises(ValueError):
                updater.load_manifest(path)
        del self.manifest["builds"]["macos-arm64"]
        path.write_text(json.dumps(self.manifest))
        with self.assertRaisesRegex(ValueError, "exactly the four"):
            updater.load_manifest(path)


class DiscoveryTest(unittest.TestCase):
    def setUp(self):
        updater.github_revision.cache_clear()
        self.addCleanup(updater.github_revision.cache_clear)

    @staticmethod
    def asset(
        version="n9.0.1-11-ge47273f4d9", branch="9.0", target="linux64", variant="gpl"
    ):
        extension = "tar.xz" if target == "linux64" else "zip"
        name = f"ffmpeg-{version}-{target}-{variant}-{branch}.{extension}"
        return {
            "name": name,
            "digest": "sha256:" + "a" * 64,
            "browser_download_url": "https://example.org/" + name,
        }

    def test_selects_highest_numeric_stable_branch_not_snapshot_or_nonfree(self):
        assets = [self.asset(), self.asset(target="win64")]
        for target in ("linux64", "win64"):
            assets += [
                self.asset(version="n10.0", branch="10.0", target=target),
                self.asset(version="N-123456-gabcdef", branch="11.0", target=target),
                self.asset(
                    version="n11.0", branch="11.0", target=target, variant="nonfree"
                ),
                self.asset(
                    version="n11.0", branch="11.0", target=target, variant="gpl-shared"
                ),
                self.asset(version="n11.0-rc1", branch="11.0", target=target),
            ]
        selected = updater.select_btbn_assets({"assets": assets})
        self.assertEqual(set(selected), {"linux64", "win64"})
        self.assertTrue(all(value[1] == "n10.0" for value in selected.values()))

    def test_incomplete_latest_branch_does_not_fall_back(self):
        assets = [
            self.asset(),
            self.asset(target="win64"),
            self.asset(version="n10.0", branch="10.0"),
        ]
        with self.assertRaisesRegex(ValueError, "missing a platform"):
            updater.select_btbn_assets({"assets": assets})

    def test_different_platform_revisions_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "revisions differ"):
            updater.select_btbn_assets(
                {
                    "assets": [
                        self.asset(),
                        self.asset(version="n9.0.1-12-gabcdef", target="win64"),
                    ]
                }
            )

    def test_macos_selects_release_link_and_resolves_tag_without_fetching_zip(self):
        url = updater.RIEDL + "/download/macos/arm64/1787073674_9.0.1/ffmpeg.zip"
        page = f'<a href="{url}">release</a><a href="{url}.sha256">checksum</a><a href="/download/macos/arm64/9999999999_N-123456-gabcdef/ffmpeg.zip">snapshot</a>'
        responses = [
            io.BytesIO(b"ffmpeg version 9.0.1-https://www.martin-riedl.de\n"),
            io.BytesIO(("a" * 64 + "  ffmpeg.zip\n").encode()),
        ]
        with patch.object(
            updater, "request", side_effect=responses
        ) as request, patch.object(
            updater, "fetch_json", return_value=[{"sha": "b" * 40}]
        ) as recipes, patch.object(
            updater, "github_revision", return_value="c" * 40
        ) as revision:
            build, year = updater.discover_macos("arm64", page)
        self.assertTrue(
            all(not call.args[0].endswith(".zip") for call in request.call_args_list)
        )
        self.assertEqual(build["url"], url)
        self.assertEqual(build["archive_sha256"], "a" * 64)
        self.assertEqual(build["member"], "ffmpeg")
        self.assertEqual(year, 2026)
        self.assertNotIn("binary_sha256", build)
        revision.assert_called_once_with("FFmpeg/FFmpeg", "n9.0.1")
        self.assertIn("until=", recipes.call_args.args[0])

    def test_macos_rejects_empty_or_invalid_checksum(self):
        page = '<a href="/download/macos/arm64/1787073674_9.0.1/ffmpeg.zip">release</a>'
        for checksum in (b"", b" \t\r\n", b"invalid  ffmpeg.zip\n"):
            responses = [
                io.BytesIO(b"ffmpeg version 9.0.1-test\n"),
                io.BytesIO(checksum),
            ]
            with self.subTest(checksum=checksum), patch.object(
                updater, "request", side_effect=responses
            ), patch.object(updater, "fetch_json") as recipes:
                with self.assertRaisesRegex(ValueError, "Invalid checksum for"):
                    updater.discover_macos("arm64", page)
                recipes.assert_not_called()

    def test_macos_rejects_empty_recipe_list(self):
        page = '<a href="/download/macos/arm64/1787073674_9.0.1/ffmpeg.zip">release</a>'
        responses = [
            io.BytesIO(b"ffmpeg version 9.0.1-test\n"),
            io.BytesIO(b"a" * 64 + b"  ffmpeg.zip\n"),
        ]
        with patch.object(updater, "request", side_effect=responses), patch.object(
            updater, "fetch_json", return_value=[]
        ), patch.object(updater, "github_revision") as revision:
            with self.assertRaisesRegex(ValueError, "No published build recipe found"):
                updater.discover_macos("arm64", page)
            revision.assert_not_called()

    def test_macos_missing_release_does_not_fall_back_to_snapshot(self):
        with patch.object(updater, "request") as request, self.assertRaisesRegex(
            ValueError, "Expected one stable"
        ):
            updater.discover_macos(
                "arm64",
                '<a href="/download/macos/arm64/9999999999_N-123456-gabcdef/ffmpeg.zip">snapshot</a>',
            )
        request.assert_not_called()

    def test_dated_release_is_used_instead_of_floating_latest(self):
        release = {
            "tag_name": "autobuild-2026-09-02-13-13",
            "html_url": "https://example.org/dated",
            "draft": False,
            "prerelease": False,
            "assets": [self.asset(), self.asset(target="win64")],
        }
        floating = {**release, "tag_name": "latest"}
        mac = {"provider": "Martin Riedl"}
        with patch.object(
            updater, "request", return_value=io.BytesIO(b"download page")
        ), patch.object(
            updater, "discover_macos", return_value=(mac, 2026)
        ), patch.object(
            updater, "fetch_json", return_value=[floating, release]
        ), patch.object(
            updater, "github_revision", return_value="c" * 40
        ) as revision, patch(
            "sys.stdout", new_callable=io.StringIO
        ):
            manifest = updater.discover_releases()
        linux = manifest["builds"]["linux-amd64"]
        self.assertEqual(linux["build_info_url"], release["html_url"])
        self.assertEqual(linux["version"], "n9.0.1-11-ge47273f4d9-20260902")
        self.assertEqual(
            linux["member"],
            self.asset()["name"].removesuffix(".tar.xz") + "/bin/ffmpeg",
        )
        revision.assert_any_call("FFmpeg/FFmpeg", "e47273f4d9")
        revision.assert_any_call("BtbN/FFmpeg-Builds", release["tag_name"])


if __name__ == "__main__":
    unittest.main()
