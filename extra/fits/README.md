# FITS Load Benchmark

Small off-JHV harness for timing the real JHV FITS loading path on a directory of FITS files.
The runner uses `bin:resources` plus `lib/*.jar` as the classpath, so JHV service-provider files such as the FastRice SPI are active.

Run from the repository root:

```sh
extra/fits/run-benchmark.sh /path/to/fits/files
```

Useful options:

```sh
extra/fits/run-benchmark.sh --warmup 2 --iterations 5 /path/to/fits/files
extra/fits/run-benchmark.sh --mode Buffer --filter None /path/to/fits/files
extra/fits/run-benchmark.sh --mode Buffer --filter MGN /path/to/fits/files
extra/fits/run-benchmark.sh --no-checksum /path/to/fits/files
```

Recommended timing run:

```sh
ant compile

JHV_SKIP_COMPILE=1 extra/fits/run-benchmark.sh \
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

`--mode Info` uses `FITSImage.readInfo` to read metadata and collect percentile ranges without creating image buffers. `--mode Buffer` prepares the clipping range once before timing. It then uses `FITSImage.decode`, so its timing includes reading pixels, conversion, and filtering. Checksums and the format column apply only to `Buffer` mode.

For JProfiler startup recording, pass the JVM argument returned by `prepare_profiling`:

```sh
JHV_BENCHMARK_JVMARG='-agentpath:/path/to/libjprofilerti.jnilib=record=/path/to/conf.xml' \
  JHV_SKIP_COMPILE=1 extra/fits/run-benchmark.sh \
  --warmup 2 \
  --iterations 1 \
  --no-checksum \
  /path/to/fits/files
```

The runner filters JProfiler status lines out of the CSV output and prints `JPROFILER_SNAPSHOT=...` on stderr if the agent saves a snapshot file.

## Fast Rice Verifier

Run the FastRice provider against nom-tam's Rice test fixtures and synthetic comparison cases:

```sh
extra/fits/run-fast-rice-verifier.sh ~/git/nom-tam-fits
```

If no path is given, it defaults to `~/git/nom-tam-fits`.

The verifier checks service-provider selection, the raw Rice fixtures, generated integer and floating-point data,
all combinations of 1/2/4-byte Rice encoding and byte/short/int output, and heap/direct buffers with sliced input.
Short-decoder boundary cases cover block transitions, every length from 1 to 256, sliced input, and consumed buffer positions.
It also compares complete compressed FITS files with their uncompressed reference through JHV's FITS loader.
Individual failures are reported without skipping subsequent cases. Any failure gives a nonzero exit status.

Small quantized cases have fixed expected values and run through both JHV's provider selection and nom-tam's decoder. They pin
the existing 1.22 reconstruction behavior: no dithering, both dither modes, nulls and zeros followed by ordinary
pixels, and double precision. Changes to upstream behavior therefore require review even when both decoders
agree. In particular, the legacy dither-2 zero marker is a compatibility check, not a claim about the FITS standard.

FastRice handles short output with `BYTEPIX=2`. Other encoded widths use upstream short decoding, and other
output types use the upstream provider. The width matrix and `m13_rice.fits` check this fallback behavior.

To check a candidate nom-tam JAR without replacing the bundled library:

```sh
JHV_NOM_TAM_JAR=/absolute/path/to/nom-tam-fits-candidate.jar \
  extra/fits/run-fast-rice-verifier.sh ~/git/nom-tam-fits
```

Both runners support `JHV_NOM_TAM_JAR`. They exclude the bundled nom-tam JAR and compile all current JHV sources
against the candidate into their temporary build directory. The verifier prints the loaded nom-tam location.
Without this variable, the runners use the bundled library as before.
