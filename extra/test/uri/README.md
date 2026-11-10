# FITS reader checks and loading benchmark

Run the Java reader checks against the bundled native library:

```sh
python3 extra/test/run_tests.py uri
```

`FITSReaderTest` checks planes and strided subsets against 13 small reference
fixtures covering pixel types, compression formats and tile boundaries. It also
checks deferred NTF headers, mapping cleanup, Unicode and bracketed paths,
gzip success and failure cleanup, the CCOR2 missing-pixel clipping regression,
and the packaged detector mask. Source checks cover image-HDU and cube-plane
selection, selected metadata, default selection, bounds and displayed row order.
`FITSSeriesTest` loads a two-file cube series through the production layer loader
and checks selection across files, decoded pixels, ordering and timestamps. It
also checks direct selection reloads, busy/unchanged-selection no-ops, retention
after failed selections and selection reset when loading a new file.

To check fresh Java classes with only a candidate native library available:

```sh
python3 extra/test/uri/check_fits_reader.py /path/to/build/output --platform macos-arm64
```

See the [native FITS guide](../../cfitsio/README.md) for builds and fixture provenance.

## Loading benchmark

The benchmark times the production JHV FITS loading path on explicit input files.

Run from the repository root:

```sh
extra/test/uri/run-benchmark.sh /path/to/fits/files
```

Useful options:

```sh
extra/test/uri/run-benchmark.sh --warmup 2 --iterations 5 /path/to/fits/files
extra/test/uri/run-benchmark.sh --mode Buffer --filter None /path/to/fits/files
extra/test/uri/run-benchmark.sh --mode Buffer --filter MGN /path/to/fits/files
extra/test/uri/run-benchmark.sh --no-checksum /path/to/fits/files
```

Recommended timing run:

```sh
ant compile

JHV_SKIP_COMPILE=1 extra/test/uri/run-benchmark.sh \
  --mode Info \
  --warmup 2 \
  --iterations 5 \
  --no-checksum \
  /path/to/fits/files
```

Run it once to warm filesystem cache/JIT and ignore that output, then run the same command again and compare the second CSV between branches.
Use `--no-checksum` for timing; checksums are useful for output comparisons but add measurable work.

Output is CSV:

```text
file,bytes,width,height,format,mode,filter,iteration,total_ms,checksum,status
```

`--mode Info` opens a `FITSSource`, which reads the header and collects percentile ranges without creating image buffers. `--mode Buffer` prepares a clipping range before timing. Each timed call constructs another source, including its clipping sample read, and then decodes, converts, and filters. It does not measure playback through a retained source. Checksums and the format column apply only to `Buffer` mode.

For JProfiler startup recording, pass the JVM argument returned by `prepare_profiling`:

```sh
JHV_BENCHMARK_JVMARG='-agentpath:/path/to/libjprofilerti.jnilib=record=/path/to/conf.xml' \
  JHV_SKIP_COMPILE=1 extra/test/uri/run-benchmark.sh \
  --warmup 2 \
  --iterations 1 \
  --no-checksum \
  /path/to/fits/files
```

The runner filters JProfiler status lines out of the CSV output and prints `JPROFILER_SNAPSHOT=...` on stderr if the agent saves a snapshot file.

## Timeline load check

`FramesLoadTest` runs the image layer loader headless on local FITS fixtures: three files sorted
into one timeline and every frame decoded with distinct pixels, a repeated file dropped with one warning, a zip
expanded alongside a file, an unreadable file skipped with a warning in a multi-file load and
failing a single-file load, and a single file keeping its base name. Both loader tests run
in the default JPEG 2000 test suite, using committed FITS inputs copied to a temporary
directory and a ZIP created there. From the repository root:

```sh
ant compile
python3 extra/test/j2k/run_j2k_tests.py
```

`LayerLoadTest` checks the layer's load-finished future through the real registry, headless:
a successful FITS load completes true and a finished layer answers at once, a failed first load
removes the layer and completes false, and removing a layer with a pending wait completes it
false. It also checks empty and directory inputs and interrupted directory enumeration.

FITS cube selection supports spatial axes independent of the plane axis. Cubes
with nonzero PC1_3, PC2_3, CD1_3 or CD2_3 are rejected because the 2D WCS
interpreter cannot represent their plane-dependent geometry.
