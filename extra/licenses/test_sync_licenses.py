#!/usr/bin/env python3
"""Offline notice extraction, synchronization, and protection tests."""

import hashlib
import io
import json
from pathlib import Path
import re
import shutil
import sys
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent))
import sync_licenses as syncer


class LicenseSyncTest(unittest.TestCase):
    def setUp(self):
        temporary = TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name).resolve()
        self.sources = self.root / "extra/licenses"
        self.output = self.root / "resources/licenses"
        self.report_path = self.root / "extra/licenses/licenses.json"
        for directory in (self.sources, self.output, self.root / "lib"):
            directory.mkdir(parents=True)
        self.apache = "Apache License\nExample terms retained in full."
        self.mit = (syncer.ROOT / "extra/licenses/MIT.txt").read_text()
        self.shared = ("Apache-2.0.txt", "MIT.txt", "GPL-3.0.txt")
        for name, text in zip(
            self.shared, (self.apache, self.mit, "Example GPL terms.")
        ):
            (self.sources / name).write_text(text)
        for name in (
            "JHelioviewer.txt",
            "EULA.txt",
            "Kakadu.txt",
            "FFmpeg-Notices.txt",
        ):
            (self.output / name).write_text("Do not alter " + name)
        self.protected = self.snapshot()
        self.missing, self.additional, self.native = {}, {}, {}
        self.stdout = patch("sys.stdout", new_callable=io.StringIO).start()
        self.addCleanup(patch.stopall)

    def snapshot(self):
        return {
            p.name: (p.read_bytes(), p.stat().st_mtime_ns)
            for p in self.output.iterdir()
            if p.is_file()
        }

    def jar(self, name, entries):
        path = self.root / "lib" / name
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, "w") as archive:
            for entry, data in entries.items():
                archive.writestr(entry, data)
        return path

    def run_sync(self, check=False):
        with patch.object(syncer, "SHARED", self.shared), patch.object(
            syncer, "MISSING_NOTICES", self.missing
        ), patch.object(syncer, "ADDITIONAL_NOTICES", self.additional), patch.object(
            syncer, "NATIVE_NOTICES", self.native
        ):
            syncer.sync(self.root, check)

    def assert_protected(self):
        current = self.snapshot()
        for name, contents in self.protected.items():
            self.assertEqual(current[name], contents)

    def notices(self):
        return (self.output / "ThirdParty-Notices.txt").read_text()

    def report(self):
        return json.loads(self.report_path.read_text())

    def pom(
        self, url="https://www.apache.org/licenses/LICENSE-2.0.txt", name="Apache 2"
    ):
        return (
            '<project xmlns="http://maven.apache.org/POM/4.0.0"><licenses><license>'
            f"<name>{name}</name><url>{url}</url>"
            "</license></licenses></project>"
        )

    def test_add_update_remove_without_a_catalog(self):
        foo = self.jar(
            "foo-1.jar", {"LICENSE": self.apache, "NOTICE": "Copyright Alice"}
        )
        bar = self.jar("bar.jar", {"LICENSE": self.apache})
        self.run_sync()
        self.assertIn("Copyright Alice", self.notices())
        self.assertNotIn("Example terms retained", self.notices())
        self.assertTrue((self.output / "Apache-2.0.txt").exists())
        foo.unlink()
        foo = self.jar("foo-2.jar", {"LICENSE": self.apache, "NOTICE": "Copyright Bob"})
        self.run_sync()
        self.assertIn("foo-2.jar", self.notices())
        self.assertIn("Copyright Bob", self.notices())
        self.assertNotIn("Copyright Alice", self.notices())
        foo.unlink()
        self.run_sync()
        self.assertNotIn("foo-2.jar", self.notices())
        bar.unlink()
        self.run_sync()
        self.assertFalse((self.output / "ThirdParty-Notices.txt").exists())
        self.assertFalse((self.output / "Apache-2.0.txt").exists())
        self.assertTrue((self.output / "GPL-3.0.txt").exists())
        self.assertEqual(self.report()["artifacts"], [])
        self.assert_protected()

    def test_changed_license_is_not_overridden_by_a_known_library_name(self):
        jar = self.jar("caffeine-1.jar", {"LICENSE": self.apache})
        self.run_sync()
        jar.unlink()
        self.jar("caffeine-2.jar", {"LICENSE": "Different license\nCopyright New"})
        self.run_sync()
        self.assertIn("Different license", self.notices())
        self.assertFalse((self.output / "Apache-2.0.txt").exists())

    def test_maven_declared_apache_license_needs_no_library_entry(self):
        self.jar("unknown.jar", {"META-INF/maven/example/unknown/pom.xml": self.pom()})
        self.run_sync()
        self.assertIn("Declared in lib/unknown.jar!", self.notices())
        self.assertIn("Apache-2.0.txt", self.notices())
        self.assertTrue((self.output / "Apache-2.0.txt").exists())
        self.assertEqual(self.report()["artifacts"][0]["supplemental_notices"], [])

    def test_unknown_license_url_is_not_guessed_from_its_name(self):
        self.jar(
            "caffeine.jar",
            {
                "META-INF/maven/example/caffeine/pom.xml": self.pom(
                    "https://example.org/custom-license", "Apache 2"
                )
            },
        )
        with self.assertRaisesRegex(ValueError, "No license text found"):
            self.run_sync()
        self.assertEqual(self.snapshot(), self.protected)

    def test_apache_url_resolution_is_exact(self):
        for url in (
            "https://www.apache.org.evil/licenses/LICENSE-2.0",
            "https://www.apache.org/licenses/LICENSE-2.0?custom=true",
            "https://www.apache.org/licenses/LICENSE-1.0",
        ):
            self.assertIsNone(syncer.declared_terms(url))
        for suffix in ("", ".txt", ".html"):
            self.assertEqual(
                syncer.declared_terms(
                    "http://www.apache.org/licenses/LICENSE-2.0" + suffix
                ),
                "Apache-2.0.txt",
            )

    def test_fallback_is_only_used_when_embedded_information_is_missing(self):
        self.missing["foo-*.jar"] = "Foo.txt"
        (self.sources / "Foo.txt").write_text("Copyright Fallback\nTerms in MIT.txt.")
        first = self.jar("foo-1.jar", {"Foo.class": b"code"})
        self.run_sync()
        self.assertIn(
            "extra/licenses/Foo.txt",
            self.report()["artifacts"][0]["supplemental_notices"],
        )
        first.unlink()
        self.jar("foo-2.jar", {"LICENSE": "New terms\nCopyright Embedded"})
        self.run_sync()
        self.assertIn("Copyright Embedded", self.notices())
        self.assertFalse((self.output / "Foo.txt").exists())
        self.assertFalse((self.output / "MIT.txt").exists())

    def test_maven_declaration_also_supersedes_a_missing_notice_fallback(self):
        self.missing["foo*.jar"] = "Unused.txt"
        self.jar("foo.jar", {"META-INF/maven/example/foo/pom.xml": self.pom()})
        self.run_sync()
        self.assertEqual(self.report()["artifacts"][0]["supplemental_notices"], [])

    def test_shaded_supplement_is_kept_alongside_embedded_terms(self):
        self.additional["foo*.jar"] = "Extra.txt"
        (self.sources / "Extra.txt").write_text("Copyright Extra\nTerms in MIT.txt.")
        self.jar("foo.jar", {"LICENSE": self.apache})
        self.run_sync()
        self.assertIn("Extra.txt", self.notices())
        self.assertIn("Copyright Extra", (self.output / "Extra.txt").read_text())
        self.assertTrue((self.output / "MIT.txt").exists())

    def test_nested_jar_notices_and_declarations_are_extracted(self):
        nested = io.BytesIO()
        with zipfile.ZipFile(nested, "w") as archive:
            archive.writestr("LICENSE", "Copyright Nested\nNested terms")
            archive.writestr("META-INF/maven/a/b/pom.xml", self.pom())
        self.jar("foo.jar", {"LICENSE": self.apache, "nested.jar": nested.getvalue()})
        self.run_sync()
        self.assertIn("lib/foo.jar!nested.jar!LICENSE", self.notices())
        self.assertIn("Copyright Nested", self.notices())
        self.assertIn("nested.jar!META-INF/maven/a/b/pom.xml", self.notices())

    def test_nested_notice_does_not_claim_to_cover_its_parent(self):
        nested = io.BytesIO()
        with zipfile.ZipFile(nested, "w") as archive:
            archive.writestr("LICENSE", self.apache)
        self.jar(
            "bare-parent.jar",
            {"nested.jar": nested.getvalue(), "Parent.class": b"code"},
        )
        with self.assertRaisesRegex(ValueError, "No license text found"):
            self.run_sync()

    def test_mit_copyright_and_alternate_terms_are_preserved(self):
        terms = self.mit[self.mit.index("Permission is hereby granted,") :]
        altered = terms.replace(
            "THE SOFTWARE IS PROVIDED",
            "The Software shall be used for Good, not Evil.\n\nTHE SOFTWARE IS PROVIDED",
        )
        self.jar(
            "foo.jar",
            {
                "LICENSE": "Copyright Alice\n" + terms,
                "other.LICENSE": "Copyright Bob\n" + altered,
            },
        )
        self.run_sync()
        text = self.notices()
        for phrase in ("Copyright Alice", "Copyright Bob", "Good, not Evil", "MIT.txt"):
            self.assertIn(phrase, text)
        self.assertEqual(text.count("Permission is hereby granted"), 1)

    def test_apache_url_variants_deduplicate_only_identical_terms(self):
        apache = (syncer.ROOT / "extra/licenses/Apache-2.0.txt").read_text()
        (self.sources / "Apache-2.0.txt").write_text(apache)
        self.jar(
            "foo.jar",
            {
                "LICENSE": apache.replace("http://", "https://"),
                "COPYING": apache.replace("https://", "http://"),
            },
        )
        self.run_sync()
        self.assertNotIn("TERMS AND CONDITIONS", self.notices())
        self.assertIn("Apache-2.0.txt", self.notices())

    def test_native_notices_follow_actual_members_not_jar_name(self):
        self.native["test"] = ("Native.txt",)
        (self.sources / "Native.txt").write_text("Copyright Native")
        self.jar("arbitrary-name.jar", {"libtest.so": b"binary"})
        self.run_sync()
        self.assertEqual(
            self.report()["artifacts"][0]["native_members"],
            ["lib/arbitrary-name.jar!libtest.so"],
        )
        self.assertIn("Native.txt", self.notices())
        self.assertTrue((self.output / "Native.txt").exists())

    def test_unknown_binary_is_not_covered_by_a_java_fallback(self):
        self.missing["lwjgl-*.jar"] = "LWJGL.txt"
        (self.sources / "LWJGL.txt").write_text("Copyright LWJGL")
        self.jar("lwjgl-1.jar", {"surprise.so": b"binary"})
        with self.assertRaisesRegex(ValueError, "No native notice found"):
            self.run_sync()
        self.assertEqual(self.snapshot(), self.protected)

    def test_mixed_native_archive_unknown_member_prevents_changes(self):
        self.native["EGL"] = ("ANGLE.txt",)
        (self.sources / "ANGLE.txt").write_text("Copyright ANGLE")
        self.jar(
            "jhv/jhv-natives-test.jar",
            {"jhv/test/libEGL.so": b"angle", "jhv/test/ffmpeg": b"ffmpeg"},
        )
        self.run_sync()
        before, report = self.snapshot(), self.report_path.read_bytes()
        self.jar(
            "jhv/jhv-natives-test.jar",
            {"jhv/test/libEGL.so": b"angle", "new-resource": b"unknown"},
        )
        with self.assertRaisesRegex(ValueError, "Unrecognized native archive member"):
            self.run_sync()
        self.assertEqual(self.snapshot(), before)
        self.assertEqual(self.report_path.read_bytes(), report)
        self.assert_protected()

    def test_missing_notice_prevents_pruning(self):
        foo = self.jar("foo.jar", {"LICENSE": self.apache})
        self.run_sync()
        before = self.snapshot()
        foo.unlink()
        self.jar("unknown.jar", {"Unknown.class": b"code"})
        with self.assertRaisesRegex(ValueError, "No license text found"):
            self.run_sync()
        self.assertEqual(self.snapshot(), before)

    def test_missing_lib_is_not_treated_as_removing_all_dependencies(self):
        (self.root / "lib").rmdir()
        with self.assertRaisesRegex(ValueError, "Missing lib directory"):
            self.run_sync()
        self.assertEqual(self.snapshot(), self.protected)

    def test_json_is_generated_and_never_controls_extraction(self):
        self.jar("foo.jar", {"LICENSE": self.apache})
        self.run_sync()
        expected, notices = self.report_path.read_bytes(), self.snapshot()
        self.report_path.unlink()
        self.run_sync()
        self.assertEqual(self.report_path.read_bytes(), expected)
        self.report_path.write_text("not even JSON")
        with self.assertRaisesRegex(ValueError, "out of sync"):
            self.run_sync(check=True)
        self.assertEqual(self.report_path.read_text(), "not even JSON")
        self.run_sync()
        self.assertEqual(self.report_path.read_bytes(), expected)
        self.assertEqual(self.snapshot(), notices)

    def test_check_detects_binary_change_without_modifying_anything(self):
        jar = self.jar("foo.jar", {"LICENSE": self.apache, "Foo.class": b"old"})
        self.run_sync()
        before, report = self.snapshot(), self.report_path.read_bytes()
        self.jar("foo.jar", {"LICENSE": self.apache, "Foo.class": b"new"})
        with self.assertRaisesRegex(ValueError, "out of sync"):
            self.run_sync(check=True)
        self.assertEqual(self.snapshot(), before)
        self.assertEqual(self.report_path.read_bytes(), report)
        self.run_sync()
        self.assertEqual(self.snapshot(), before)
        self.assertEqual(
            self.report()["artifacts"][0]["sha256"],
            hashlib.sha256(jar.read_bytes()).hexdigest(),
        )

    def test_repeat_and_check_are_write_free(self):
        self.jar("foo.jar", {"LICENSE": self.apache})
        with self.assertRaisesRegex(ValueError, "out of sync"):
            self.run_sync(check=True)
        self.assertEqual(self.snapshot(), self.protected)
        self.run_sync()
        before = self.snapshot()
        report_time = self.report_path.stat().st_mtime_ns
        self.run_sync(check=True)
        self.run_sync()
        self.assertEqual(self.snapshot(), before)
        self.assertEqual(self.report_path.stat().st_mtime_ns, report_time)

    def test_protected_files_cannot_be_supplement_targets(self):
        for name in (
            "JHelioviewer.txt",
            "EULA.txt",
            "KAKADU.txt",
            "FFmpeg-Notices.txt",
        ):
            with self.subTest(name=name):
                self.missing["absent.jar"] = name
                with self.assertRaisesRegex(ValueError, "Protected notice"):
                    self.run_sync()
                self.assertEqual(self.snapshot(), self.protected)

    def test_protected_files_cannot_be_shared_licenses(self):
        self.shared += ("EULA.txt",)
        with self.assertRaisesRegex(ValueError, "Protected notice"):
            self.run_sync()
        self.assertEqual(self.snapshot(), self.protected)

    def test_protected_files_with_generated_markers_are_never_pruned(self):
        for name in self.protected:
            (self.output / name).write_text(syncer.GENERATED + "still protected")
        self.protected = self.snapshot()
        self.run_sync()
        self.assert_protected()

    def test_symlink_to_protected_file_is_not_followed(self):
        self.jar("foo.jar", {"LICENSE": self.apache})
        for path in (self.output / "ThirdParty-Notices.txt", self.report_path):
            with self.subTest(path=path):
                path.symlink_to(self.output / "EULA.txt")
                with self.assertRaisesRegex(ValueError, "symlink"):
                    self.run_sync()
                path.unlink()
                self.assertEqual(self.snapshot(), self.protected)

    def test_unmanaged_files_are_neither_removed_nor_overwritten(self):
        (self.output / "Manual.txt").write_text("Keep me")
        self.run_sync()
        self.assertEqual((self.output / "Manual.txt").read_text(), "Keep me")
        self.jar("foo.jar", {"LICENSE": self.apache})
        (self.output / "ThirdParty-Notices.txt").write_text("Manually maintained")
        before = self.snapshot()
        with self.assertRaisesRegex(ValueError, "unmanaged"):
            self.run_sync()
        self.assertEqual(self.snapshot(), before)

    def test_invalid_or_conflicting_supplement_name_is_rejected(self):
        for name in (
            "../EULA.txt",
            "/tmp/notice.txt",
            "sub/notice.txt",
            "apache-2.0.txt",
            "ThirdParty-Notices.txt",
        ):
            with self.subTest(name=name):
                self.missing["absent.jar"] = name
                with self.assertRaises(ValueError):
                    self.run_sync()
                self.assertEqual(self.snapshot(), self.protected)

    def test_case_conflicting_supplements_are_rejected(self):
        self.missing.update({"foo.jar": "Extra.txt", "bar.jar": "extra.txt"})
        with self.assertRaisesRegex(ValueError, "Conflicting supplement names"):
            self.run_sync()
        self.assertEqual(self.snapshot(), self.protected)

    def test_empty_or_duplicate_shared_license_is_rejected(self):
        self.shared += ("APACHE-2.0.txt",)
        with self.assertRaisesRegex(ValueError, "Duplicate shared license"):
            self.run_sync()
        self.shared = self.shared[:-1]
        (self.sources / "Apache-2.0.txt").write_text("   ")
        with self.assertRaisesRegex(ValueError, "Empty shared license"):
            self.run_sync()

    def test_corrupt_archive_missing_supplement_and_bad_pom_do_not_write(self):
        jar = self.root / "lib/foo.jar"
        jar.write_bytes(b"not a zip")
        with self.assertRaises(zipfile.BadZipFile):
            self.run_sync()
        self.missing["foo.jar"] = "Missing.txt"
        self.jar("foo.jar", {"Foo.class": b"code"})
        with self.assertRaises(FileNotFoundError):
            self.run_sync()
        self.missing.clear()
        self.jar("foo.jar", {"META-INF/maven/a/b/pom.xml": "<broken>"})
        with self.assertRaises(ET.ParseError):
            self.run_sync()
        self.assertEqual(self.snapshot(), self.protected)

    def test_real_inventory_and_removals_in_isolated_tree(self):
        shutil.copytree(
            syncer.ROOT / "extra/licenses", self.sources, dirs_exist_ok=True
        )
        self.shared = syncer.SHARED
        self.missing = syncer.MISSING_NOTICES
        self.additional = syncer.ADDITIONAL_NOTICES
        self.native = syncer.NATIVE_NOTICES
        jars = list((syncer.ROOT / "lib").rglob("*.jar"))
        for jar in jars:
            link = self.root / "lib" / jar.relative_to(syncer.ROOT / "lib")
            link.parent.mkdir(parents=True, exist_ok=True)
            link.symlink_to(jar)
        self.run_sync()
        self.assertEqual(len(self.report()["artifacts"]), len(jars))
        self.assertIn("IPTC Photo Metadata", self.notices())
        self.assertIn("Dracula", self.notices())
        # Preserve every copyright line in the currently distributed notices,
        # including nonstandard and supplemental terms, across the reshaping.
        new_text = " ".join(
            " ".join(p.read_text().split()) for p in self.output.glob("*.txt")
        )
        for old in (syncer.ROOT / "resources/licenses").glob("*.txt"):
            if old.name.casefold() in syncer.PROTECTED:
                continue
            for line in old.read_text().splitlines():
                if re.match(r"\s*copyright\b", line, re.I):
                    self.assertIn(" ".join(line.split()), new_text, (old.name, line))
        for pattern in (
            "formats/tika-core-*.jar",
            "ui/flatlaf-intellij-themes-*.jar",
            "caffeine-*.jar",
            "lwjgl/lwjgl-assimp-*.jar",
        ):
            for link in (self.root / "lib").glob(pattern):
                link.unlink()
        self.run_sync()
        self.assertNotIn("IPTC Photo Metadata", self.notices())
        self.assertNotIn("Dracula", self.notices())
        self.assertNotIn("lib/caffeine-", self.notices())
        self.assertFalse((self.output / "Assimp.txt").exists())
        self.assertFalse((self.output / "Draco.txt").exists())
        self.assertTrue((self.output / "Apache-2.0.txt").exists())
        self.run_sync(check=True)
        self.assert_protected()


if __name__ == "__main__":
    unittest.main()
