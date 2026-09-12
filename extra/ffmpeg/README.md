# Updating bundled FFmpeg

From the repository root, run:

```sh
python3 extra/ffmpeg/update_ffmpeg.py
```

The script discovers the current stable prebuilt releases, obtains the providers'
checksums, and updates the four native JARs. It generates `extra/ffmpeg/ffmpeg.json` and
the About-dialog notice in `resources/licenses/FFmpeg-Notices.txt` with the selected
versions, download URLs, hashes, and source links. **Do not prepare or edit the
JSON file before updating.** It is the record of what was installed.
Commit it and the generated notice together with the updated native JARs, so
the recorded versions and hashes stay matched to the binaries in the repository.

For a preview or an offline check of the installed files:

```sh
python3 extra/ffmpeg/update_ffmpeg.py --list
python3 extra/ffmpeg/update_ffmpeg.py --check
```

The providers are [Martin Riedl](https://ffmpeg.martin-riedl.de/) for macOS
and [BtbN](https://github.com/BtbN/FFmpeg-Builds/releases) for Linux and Windows.
The updater selects Mac release builds and BtbN's newest stable release branch
from a dated release, excluding snapshots, nonfree builds, and shared-library
packages. BtbN builds can include fixes after the latest numbered FFmpeg release.
Versions may therefore differ between platforms. An older version than the
installed record is rejected rather than silently downgraded.

Downloads are checked against provider SHA-256 hashes, and the executables are
checked for GPLv3, libx264, and libx265 configuration flags. Matching installed
binaries are verified against the saved record without downloading them again.
All candidate JARs are prepared and their unrelated entries checked before any
installed files are replaced. Temporary downloads are removed automatically.

Python 3.11 or later is sufficient. No extra Python packages are needed. The
script never downloads FFmpeg source or build recipes and never compiles or
executes anything. The source links record the FFmpeg revisions and published
provider recipes, not a verification of every included library's source.

After updating, review the diff and run `ant jar` to package the generated notice.
Test JHV's H.264, H.264 better, H.265, H.265 better, and PNG-series exports on the
target systems. The updater's integrity checks do not replace runtime testing.

Offline tests for discovery, archive handling, and safe replacement:

```sh
python3 -m unittest discover -s extra/ffmpeg -p test_update_ffmpeg.py -v
```
