# Keeping third-party notices in sync

After adding, updating, or removing JARs in `lib`, run:

```sh
python3 extra/licenses/sync_licenses.py
```

The script scans the installed JARs and writes the extracted notices to
`resources/licenses/ThirdParty-Notices.txt`. Common license terms are kept in
shared files, and supplemental notices are included only when needed by the
installed artifacts.

`extra/licenses/licenses.json` is the generated inventory of artifacts, checksums, and
notice sources. It is not configuration and does not control extraction.
Deleting it is harmless, as the next run recreates it from the installed files.
Commit the inventory and generated notices with dependency changes.

To check without writing or deleting anything:

```sh
python3 extra/licenses/sync_licenses.py --check
```

Python 3.11 or later is required. The script runs offline using the standard
library. It does not download anything, execute binaries, or modify JARs.

## Extraction and exceptions

License and notice documents are extracted from each JAR, including nested JARs.
The script preserves their text and attributions, replacing identical common
license terms with references to the shared copies. Embedded Maven license
declarations are recorded too. An exact Apache 2.0 license URL resolves to the
shared Apache text without any library-specific entry. Other license names and
URLs are not guessed.

There is no general component catalog. The remaining exceptions supply
information that the artifacts omit:

- Missing notices use upstream texts from `extra/licenses` only when the JAR has
  neither its own notice nor a supported license declaration.
- Supplemental notices for known shaded dependencies are retained alongside the
  enclosing JAR's terms.
- Native dependencies are identified from actual archive members, with separate
  notices for the incorporated libraries.

The ANGLE updater refreshes its supplemental text from the checksummed build
artifacts and runs this synchronizer after installation. Other supplemental
texts are not automatically refreshed or verified against new builds. When updating an affected dependency, check whether its upstream
terms or incorporated components changed. The inventory distinguishes extracted
documents, declarations, and supplemental sources. It does not certify that a
licensing review has been performed.

If usable license information is missing, the script stops before changing
anything and identifies the artifact. Unrecognized members of the mixed JHV
native archives also stop the sync. A new library with embedded notices needs
no script entry.

Removing a dependency removes notices that no remaining artifact needs.
Shared license texts remain until their last user is gone.

## Protected files

`JHelioviewer.txt`, `EULA.txt`, and `Kakadu.txt` are never written, renamed, or
deleted. `FFmpeg-Notices.txt` remains owned by `update_ffmpeg.py` and is also left
untouched. Its shared GPL text stays in the distribution as long as that notice
exists.

Only notice files bearing the script's generated-file header can be replaced or
removed. Other files are left alone, and an output-name collision stops the sync.
All input validation finishes before writing begins.

## Tests

```sh
python3 -m unittest discover -s extra/licenses -p test_sync_licenses.py -v
```
