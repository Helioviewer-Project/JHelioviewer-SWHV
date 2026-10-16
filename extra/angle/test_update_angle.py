import json
import subprocess
from unittest.mock import patch
from pathlib import Path
import tempfile
import unittest

from PIL import Image

import update_angle as updater


class UpdateAngleTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)

    def test_install_changes_only_selected_files(self):
        library = self.root / "lib/jhv/linux-amd64/libEGL.so"
        library.parent.mkdir(parents=True)
        library.write_bytes(b"old egl")
        other = library.with_name("libkdu_jni.so")
        other.write_bytes(b"untouched")
        output = self.root / "staged"
        for name, data in {"candidate/jhv/linux-amd64/libEGL.so": b"new egl", "angle.json": b"manifest", "ANGLE.txt": b"notice"}.items():
            path = output / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
        (self.root / "extra/angle").mkdir(parents=True)
        (self.root / "extra/licenses").mkdir(parents=True)
        with patch.object(updater.subprocess, "run"):
            updater.install(self.root, output, {"jhv/linux-amd64/libEGL.so": updater.digest(library)})
        self.assertEqual(library.read_bytes(), b"new egl")
        self.assertEqual(other.read_bytes(), b"untouched")

    def test_install_restores_all_files_after_sync_failure(self):
        output = self.root / "staged"
        originals = {
            "lib/jhv/linux-amd64/libEGL.so": b"old egl",
            "extra/angle/angle.json": b"old version",
            "extra/licenses/ANGLE.txt": b"old source",
            "extra/licenses/licenses.json": b"old inventory",
            "resources/licenses/ANGLE.txt": b"old notice",
        }
        for name, content in originals.items():
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
        for name in ("candidate/jhv/linux-amd64/libEGL.so", "angle.json", "ANGLE.txt"):
            path = output / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b"new")

        def fail_sync(*args, **kwargs):
            self.assertEqual((self.root / "extra/angle/angle.json").read_bytes(), b"new")
            (self.root / "resources/licenses/ANGLE.txt").unlink()
            (self.root / "resources/licenses/New.txt").write_bytes(b"new notice")
            (self.root / "extra/licenses/licenses.json").write_bytes(b"new inventory")
            raise subprocess.CalledProcessError(1, "sync")

        hashes = {"jhv/linux-amd64/libEGL.so": updater.digest(self.root / "lib/jhv/linux-amd64/libEGL.so")}
        with patch.object(updater.subprocess, "run", side_effect=fail_sync):
            with self.assertRaises(subprocess.CalledProcessError):
                updater.install(self.root, output, hashes)
        for name, content in originals.items():
            self.assertEqual((self.root / name).read_bytes(), content)
        self.assertFalse((self.root / "resources/licenses/New.txt").exists())

    def test_checksum_corruption_rejected_before_metadata(self):
        (self.root / "libEGL.so").write_bytes(b"bad")
        (self.root / "SHA256SUMS").write_text("0" * 64 + "  libEGL.so\n")
        with self.assertRaisesRegex(ValueError, "Checksum mismatch"):
            updater.artifact(self.root, "linux-x64", "44.3.0")

    def test_license_notice_preserves_text_and_pins_source(self):
        path = self.root / "ANGLE-LICENSE.txt"
        revision = "a" * 40
        path.write_bytes(b"// Copyright Example\r\n//\r\n//     Keep this notice.\r\n")
        notice = updater.license_notice(self.root, revision)
        self.assertIn("/+/" + revision + "/LICENSE", notice)
        self.assertTrue(notice.endswith("Copyright Example\n\n    Keep this notice.\n"))
        path.write_bytes(path.read_bytes().replace(b"\r\n", b"\n"))
        self.assertEqual(updater.license_notice(self.root, revision), notice)
        path.write_text("")
        with self.assertRaisesRegex(ValueError, "Empty ANGLE license"):
            updater.license_notice(self.root, revision)

    def test_comparison_tolerance_and_missing_cases(self):
        before, after = self.root / "before", self.root / "after"
        before.mkdir()
        after.mkdir()
        Image.new("RGB", (10, 10), (40, 40, 40)).save(before / "grid.png")
        Image.new("RGB", (10, 10), (41, 40, 40)).save(after / "grid.png")
        self.assertFalse(updater.compare_images(before, after, self.root, 2))
        changed = Image.new("RGB", (10, 10), (40, 40, 40))
        changed.putpixel((5, 5), (50, 40, 40))
        changed.save(after / "grid.png")
        self.assertTrue(updater.compare_images(before, after, self.root, 2))
        rows = json.loads((self.root / "comparison.json").read_text())
        self.assertEqual(rows[0]["pixels_over_tolerance"], 1)
        (after / "grid.png").unlink()
        with self.assertRaisesRegex(ValueError, "Missing or mismatched"):
            updater.compare_images(before, after, self.root, 2)


if __name__ == "__main__":
    unittest.main()
