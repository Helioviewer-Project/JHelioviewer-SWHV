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
  --mode Image \
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

`--mode Image` uses `FITSImage.readImage`, matching the normal local FITS initial load path including header XML creation. `--mode Buffer` uses `FITSImage.readImageBuffer` and is useful when isolating pixel conversion/filter work.

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
