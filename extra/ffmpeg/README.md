# Updating bundled FFmpeg

From the repository root, run:

```sh
python3 extra/ffmpeg/update_ffmpeg.py
```

The script discovers the latest stable Jellyfin portable release, obtains GitHub's
SHA-256 checksums, and updates `lib/jhv/<platform>/ffmpeg`. It generates `extra/ffmpeg/ffmpeg.json` and
the About-dialog notice in `resources/licenses/FFmpeg-Notices.txt` with the selected
version, download URLs, hashes, and source links. **Do not prepare or edit the
JSON file before updating.** It is the record of what was installed.
Commit it and the generated notice together with the updated executables, so
the recorded versions and hashes stay matched to the binaries in the repository.

For a preview or an offline check of the installed files:

```sh
python3 extra/ffmpeg/update_ffmpeg.py --list
python3 extra/ffmpeg/update_ffmpeg.py --check
```

All four platforms use the same release from
[Jellyfin](https://github.com/jellyfin/jellyfin-ffmpeg/releases): GPL portable builds
for macOS ARM64 and AMD64, Linux AMD64, and Windows AMD64 (Clang). The archives
contain standalone executables without companion codec libraries. The current
builds require macOS 12 or later and glibc 2.28 or later on Linux. An incomplete
release or a version older than the installed record is rejected.

Downloads are checked against GitHub's SHA-256 hashes, and the executables are
checked for GPLv3, libx264, and libx265 configuration flags. Matching installed
binaries are verified against the saved record without downloading them again.
All downloads and candidate checks finish before any installed files are replaced. Temporary downloads are removed automatically.

Python 3.11 or later is sufficient. No extra Python packages are needed. The
script never downloads FFmpeg source or build recipes and never compiles or
executes anything. The source links record the Jellyfin FFmpeg revision and published
build recipes, not a verification of every included library's source.

After updating, review the diff and run `ant jar` to package the generated notice.
Test JHV's H.264, H.264 better, H.265, H.265 better, and PNG-series exports on the
target systems. The updater's integrity checks do not replace runtime testing.

The FFmpeg executables use Git LFS because some builds exceed
GitHub's normal file-size limit. Run `git lfs pull` after a checkout made without
automatic LFS downloads.

Offline tests for discovery, archive handling, and safe replacement:

```sh
python3 -m unittest discover -s extra/ffmpeg -p test_update_ffmpeg.py -v
```
