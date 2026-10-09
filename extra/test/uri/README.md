# FITS Load Benchmark

Small off-JHV harness for timing the real JHV FITS loading path on a directory of FITS files.
The runner uses `bin:resources` plus `lib/*.jar` as the classpath, so JHV service-provider files such as the FastRice SPI are active.

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

`--mode Info` opens a `FITSSource`, which reads the header and collects percentile ranges without creating image buffers. `--mode Buffer` prepares the clipping range once before timing. It then decodes through the source, so its timing includes reading pixels, conversion, and filtering. Checksums and the format column apply only to `Buffer` mode.

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

## Fast Rice Verifier

Run the FastRice provider against nom-tam's Rice test fixtures and synthetic comparison cases:

```sh
extra/test/uri/run-fast-rice-verifier.sh ~/git/nom-tam-fits
```

If no path is given, it defaults to `~/git/nom-tam-fits`.

The verifier checks service-provider selection, the raw Rice fixtures, generated integer and floating-point data,
all combinations of 1/2/4-byte Rice encoding and byte/short/int output, with heap and direct input/output tested independently.
Heap short/int outputs are array-backed so the matrix exercises the fast decoder as well as its fallbacks.
Short and integer decoder boundary cases cover block transitions, every length from 1 to 256, sliced input and output, and consumed buffer positions.
For each width, 2,000 constructed tiles cover every block code and long unary zero runs with expected pixels calculated independently of both decoders.
These cases also check output guards and input/output positions.
It also compares complete compressed FITS files with their uncompressed reference through JHV's FITS loader.
Individual failures are reported without skipping subsequent cases. Any failure gives a nonzero exit status.

Small quantized cases have fixed expected values and run through both JHV's provider selection and nom-tam's decoder.
They cover no dithering, both dither modes, nulls and zeros followed by ordinary pixels, and double precision.
The expectations include the quantization corrections and dither-sequence fixes in the bundled nom-tam version.

FastRice handles short output with `BYTEPIX=2` and integer output with `BYTEPIX=4`.
Quantized float and double output uses the fast integer decoder followed by nom-tam's reconstruction.
The integer optimization requires heap input and output. Direct buffers and other encoded widths use upstream decoding.
Byte output also uses the upstream provider. The width matrix and `m13_rice.fits` check these fallback paths.

To check a candidate nom-tam JAR without replacing the bundled library:

```sh
JHV_NOM_TAM_JAR=/absolute/path/to/nom-tam-fits-candidate.jar \
  extra/test/uri/run-fast-rice-verifier.sh ~/git/nom-tam-fits
```

Both runners support `JHV_NOM_TAM_JAR`. They exclude the bundled nom-tam JAR and compile all current JHV sources
against the candidate into their temporary build directory. The verifier prints the loaded nom-tam location.
Without this variable, the runners use the bundled library as before.

## Timeline load check

`FramesLoadTest` runs the image layer loader headless on local FITS fixtures: three files sorted
into one timeline and the first frame decoded, a repeated file dropped with one warning, a zip
expanded alongside a file, an unreadable file skipped with a warning in a multi-file load and
failing a single-file load, and a single file keeping its base name. Compile it against
`bin:resources` plus `lib/*.jar` and run it with a temporary `user.home`:

```sh
java -Djava.awt.headless=true -Duser.home=/tmp/jhv-frames --enable-native-access=ALL-UNNAMED \
  -cp "<test classes>:bin:resources:lib/*" org.helioviewer.jhv.layers.FramesLoadTest \
  a.fits b.fits c.fits two.zip not-an-image
```
