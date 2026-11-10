import hashlib
import json
from pathlib import Path
import tarfile
import unittest


class PinnedSourcesTest(unittest.TestCase):
    def test_archives_match_verified_fingerprints_and_contain_build_inputs(self):
        here = Path(__file__).resolve().parent
        manifest = json.loads((here / "cfitsio.json").read_text())
        for key in ("cfitsio", "zlib"):
            with self.subTest(dependency=key):
                dependency = manifest[key]
                archive = here / f"{key}-{dependency['version']}.tar.xz"
                self.assertEqual(hashlib.sha256(archive.read_bytes()).hexdigest(), dependency["archive_sha256"])
                with tarfile.open(archive) as bundle:
                    members = set(bundle.getnames())
                    self.assertIn("CMakeLists.txt", members)
                    self.assertIn("fitsio.h" if key == "cfitsio" else "zlib.h", members)
                    self.assertIn("licenses/License.txt" if key == "cfitsio" else "README", members)


if __name__ == "__main__":
    unittest.main()
