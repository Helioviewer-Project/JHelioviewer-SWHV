# JPEG 2000 and JPIP tests

The offline and live runner requires JDK 25, Python 3, and compiled JHV classes.
From the repository root:

```sh
ant compile
python3 extra/test/j2k/run_j2k_tests.py
```

The default suite uses loopback servers and needs no external network or JPEG 2000
native library. Image-memory checks use the bundled LWJGL native allocator:

- Decode scheduling: repeated keys reuse running work or a result awaiting its EDT callback,
  with the latest delivery context. Checks cover A-B-A, failure/retry, invalidation,
  independent progress of different tasks, and unchanged unkeyed submission behavior.
- Image memory: cache invalidation, replacement and eviction release native buffers without
  repainting or garbage collection; layer slots and queued deliveries retain their own references,
  and the last release frees the pixels. Weighted LRU eviction follows recency rather than frequency.
  A full-cache, 512-frame playback check changes resolution with equally scaled images and budget;
  all new frames must be cache hits from the second loop onward.
  Retaining or releasing an already released image must throw with Java assertions disabled.
  Native image writers clear every pixel, transfer ownership at finish, and release their input
  immediately if filtering fails.
- HTTP streams: fixed-length and chunked bodies, response boundaries,
  zero-length reads at EOF, premature EOF, draining a chunked body on close, and no second
  read after a timeout in chunk framing or payload.
- Socket: a local server checks the channel request and its response framing, refused
  channels, the metadata and frame request lines, two requests sent ahead with their bodies
  returned in order, plain, gzip and deflate bodies decoded beyond the receive buffer,
  compression framing split across chunks, graceful channel close,
  aborting a stalled response without sending another request, and a stalled handshake
  interrupted on a virtual thread, which must close TCP.
- Cache serializer: round trip of an entry, direct/read-only buffers, unchanged input
  positions, and rejection of an entry without its level. The block of an entry is the
  client's; the client refuses a damaged one on import.
- Cache: failure to obtain the persistence lock leaves caching disabled without repeated logging.
  With the lock released: entries by level, replacement only by a finer level, removal.
  The test verifies that its cache stays beneath its temporary directory. On non-Windows hosts,
  it also runs with Windows cache-path selection enabled. The runner isolates both `user.home`
  and `java.io.tmpdir`; Windows requires an ASCII temporary path and fails before testing otherwise.
- Resolution selection: exact dimensions, one pixel above and below each boundary,
  either axis of a rectangular image, a single level and progressive completion.
  These checks exercise `getNextLevel`, not every resolution-selection method.
- Reader continuation: progressing byte-limited responses continue, stalled byte- or
  quality-limited responses stop, unexpected EOR reasons fail, and interruption prevents
  another request. These checks supply response summaries without invoking the native parser.

Compilation and each default test process have a 60-second timeout.

To also retrieve from ROB using the actual JPIP socket, client, disk cache and decoder:

```sh
python3 extra/test/j2k/run_j2k_tests.py --live
```

The native libraries come from the host's bundled native JAR. `--bridge PATH`
overrides its bridge with a supplied `libjhvj2k`; Kakadu still comes from the JAR
and must be compatible with that bridge.
The live runner supports macOS arm64, macOS x86-64 and Linux x86-64.
GC-dependent abandoned-view cleanup is opt-in with `--live --cleaner`.
The live view test has a 480-second internal deadline,
with a 510-second process timeout so it can report its own failure first.

`J2KReaderTest` shares reader setup, raw decoding and cache restoration checks for an image
and a movie. The single-frame fixture is an AIA 171 image from September 9, 2026. It retrieves
levels 2 and 0 through the reader and checks completion and the persisted level after each.
A second session restores from the disk cache after closing and reopening
its manager. Its socket is aborted after the metadata and the first coarse level, so the
tested resolutions cannot be downloaded again. Metadata and decoded pixel hashes are compared
between the sessions, not pinned across KDU versions. The reader test process has a
360-second timeout. A service outage or removed fixture fails the test, rather than
silently skipping it.

The live suite requests a fixed ROB movie through the `getJPX` API so the server creates
it if needed, then uses the returned JPIP URI. It compares the first four frames using sequential
requests without the reader, the actual reader prefetch pump, reopened disk-cache restoration
with the socket aborted, and a session in which one cache entry is replaced by a block the
client refuses (the frame is fetched and stored again). The pump is invoked directly without a
GUI view, while its worker remains idle. Completed or restored frames must reset
the reader's consecutive failure count within the pass.

The same movie is then opened by the real view, headless, with its reader thread. Paused and
first shown at its coarsest level, which opening already completed, the movie must still
download; a finer level of the shown frame must be fetched and delivered by the reader's
refresh. With the player in its playing state, the test selects frames explicitly and
downloads the whole movie at a coarse level twice, once with the connection aborted
after a third of the frames; the reader must reconnect and the pixels of
every frame must be equal.
Reader refreshes must use the latest viewpoint and requested resolution after zooming out,
even when a finer window finishes late. Detached refreshes are exercised with the
data handler cleared.

`J2KViewTest` captures opening responses and small windows for two frames in memory,
then replays them through a local server. There are no committed replay fixtures;
these checks require `--live` to obtain the captures. The server holds both replies
until the prefetch pump has sent two requests.
The real `signal()` method queues newer work, then both replies are released: the pump must
consume them and yield without requesting a third frame. Its worker stays unstarted for this check;
the test does not replace the signal queue. The same captures drive the retry and refusal checks.
Two failed passes must recover, with a pause before the second retry. There is no upper
elapsed-time assertion on the first retry: scheduling stalls are not reader-imposed pauses.
Fourteen consecutive failed passes must exhaust retries and log the cause once,
even when every pass accepts byte-limited data before failing. A conflicting response must stop
the reader and purge this source's disk entries while preserving an unrelated entry. Equal-length
timestamp edits create duplicate and out-of-order frames: both movies must be rejected with
the offending frame indices and timestamps, and their connections must close before background
downloading starts. Ordered movies retain their per-frame disk keys.

The live suite also downloads a Callisto JP2 through the ROB API and checks horizontal
crops at the origin, interior, and right edge against a full-image decode. It exercises
all six Callisto resolution levels (0 through 5), including nonaligned requested regions.
Full-image hashes are printed for comparison when changing the decoder. These checks cover
crop/full-image consistency, not independent pixel correctness, RGB composition or
timeline drawing. The Callisto test process has a 180-second timeout.
The same fixture checks the shared radio crop cache under its 438 MiB byte budget:
values are soft so GC can reclaim unused crops
under memory pressure, and eviction follows recency across seven days.
Removing a day purges only its crops, and removed days cannot accept late decode results.
Displayed scalar pixels survive eviction and LUT changes. Immutable fixture arrays are reused
across day entries to exercise cache weights without allocating 438 MiB of pixels.

`run_native_test.sh` requires at least one JP2/JPX file argument, `KDU_VENDOR` to
name a supplied Kakadu SDK, and the esajpip server build dependencies. On the
macOS and Linux hosts supported by `native/jpeg2000/build.sh`, run:

```sh
KDU_VENDOR=/path/to/kakadu-sdk bash extra/test/j2k/run_native_test.sh /path/to/movie.jpx
```

It builds the bridge and checks it, through `J2KNative`, against Kakadu's
compositor as an independent decoder: geometry and pixels of every frame and level of the
given files, JPIP responses written by esajpip's server code, cache entries, refusals and
failures. One-byte frames, including indexed frames, use the compositor's
raw-component path; color frames use its rendered-layer path.
The compositor's Java binding, `extra/test/j2k/lib/kdu_jni.jar`, is used by tests only.
The Java process has a 600-second timeout; set `JHV_NATIVE_TEST_TIMEOUT` to change
that budget for a larger supplied corpus. This also bounds a stalled native compositor call.

For prebuilt KDU and bridge libraries, use the same native test without building
the server or generating JPIP responses:

```sh
python3 extra/test/j2k/check_native_build.py /path/to/platform-libraries --esajpip ../esajpip-SWHV
```

This requires Python 3, JDK 25 and the existing RGB, grayscale and JPX fixtures
in the esajpip checkout. It compiles the binding and native test itself; neither
compiled JHV classes nor server build dependencies are needed.
Run it on the platform and architecture matching the supplied libraries. The
packaged targets are macOS arm64, macOS x86-64, Linux x86-64 and Windows x86-64.
On Windows it loads the core DLL before JNI/support and the bridge;
the file-descriptor count check applies only on Unix.
With a 64 MiB Java heap, it also checks response parsing through the FFM binding:
every truncated prefix of a valid sample response, unknown classes, reserved headers,
overlong VBAS, a missing payload declared as nearly 2 GiB, and defined and invalid
EOR reasons. Native allocation is outside the Java heap limit; the missing-payload
case must return a refusal.
Response checks have a 30-second process timeout; fixture decoding has a separate
300-second timeout, and compilation has a 60-second timeout.
Palette files generated in a temporary directory assert fixed RGBA bytes for channel
ordering, grayscale replication, signed and unsigned sample conversion, opaque alpha
and last-entry padding.
Every palette file must decode to the fixed raw indices 0,1,2,3. One eight-bit
palette file also runs the compositor comparison and concurrent-job lifetime checks;
the two-bit file uses its fixed index expectation because the compositor scales
two-bit samples to grayscale.
Short palette output buffers, signed indices and index depths above eight bits
must be rejected. The generated files are removed afterward.

The Python runners put generated files in temporary directories and clean them up
on normal exit. The offline/live runner isolates both application settings and
caches beneath its temporary directory. The shell test keeps its build and generated
responses in `tmp/j2k-native`, or the directory named by `BUILD`; `ESAJPIP` overrides
its default source checkout, `../esajpip-SWHV`.

This suite does not cover all reader lifecycle races, GUI rendering, all HTTP framing errors,
or GUI playback with 8 GiB of actual cached pixels. Live pixel hashes compare runs
of the same decoder, while the native test compares against Kakadu's compositor.
The view check uses image metadata but does not independently validate its geometry.
