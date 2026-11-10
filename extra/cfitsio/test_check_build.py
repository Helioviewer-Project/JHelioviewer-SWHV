import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

import check_build


class CandidateBuildTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="jhv-fits-record-")
        self.addCleanup(self.temporary.cleanup)
        self.output = Path(self.temporary.name)
        self.target = "macos-arm64"
        resource, name = check_build.PLATFORMS[self.target]
        self.library = self.output / resource / name
        self.library.parent.mkdir()
        self.library.write_bytes(b"candidate, not the bundled reader")
        self.record = {
            "platform": self.target,
            "source": json.loads((check_build.HERE / "cfitsio.json").read_text()),
            "bytes": self.library.stat().st_size,
            "sha256": hashlib.sha256(self.library.read_bytes()).hexdigest(),
        }
        self.records([self.record])

    def records(self, records):
        (self.output / "builds.json").write_text(json.dumps(records))

    def test_matching_candidate_and_manifest(self):
        library, record = check_build.checked_library(self.output, self.target)
        self.assertEqual(library, self.library)
        self.assertEqual(record, self.record)

    def test_rejects_changed_binary_even_with_same_size(self):
        self.library.write_bytes(b"X" * self.record["bytes"])
        with self.assertRaisesRegex(ValueError, "library does not match"):
            check_build.checked_library(self.output, self.target)

    def test_rejects_changed_dependencies(self):
        record = copy.deepcopy(self.record)
        record["source"]["cfitsio"]["version"] = "different"
        self.records([record])
        with self.assertRaisesRegex(ValueError, "pinned dependencies"):
            check_build.checked_library(self.output, self.target)

    def test_rejects_duplicate_or_missing_platform_record(self):
        for records in ([], [self.record, self.record]):
            with self.subTest(count=len(records)):
                self.records(records)
                with self.assertRaisesRegex(ValueError, "one build record"):
                    check_build.checked_library(self.output, self.target)


if __name__ == "__main__":
    unittest.main()
