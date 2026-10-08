# JPEG 2000 and JPIP tests

Requires JDK 25, Python 3, and compiled JHV classes. From the repository root:

```sh
ant compile
python3 extra/test/j2k/run_j2k_tests.py
```

The default suite is offline and needs no JPEG 2000 native library:

- Image memory: cache invalidation, replacement and eviction release native buffers without
  repainting or garbage collection; layer slots and queued deliveries retain their own references,
  and the last release frees the pixels. Weighted LRU eviction follows recency rather than frequency.
  A full-cache, 512-frame playback check changes resolution with equally scaled images and budget;
  all new frames must be cache hits from the second loop onward.
- HTTP streams: fixed-length and chunked bodies, response boundaries,
  zero-length reads at EOF, premature EOF, draining a chunked body on close, and no second
  read after a timeout in chunk framing or payload.
- Socket: a local server checks the channel request and its response framing, refused
  channels, the metadata and frame request lines, two requests sent ahead with their bodies
  returned in order, plain and gzip chunked bodies larger than the receive buffer, gzip headers
  and trailers split across chunks, graceful channel close,
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

To also retrieve from ROB using the actual JPIP socket, client, disk cache and decoder:

```sh
python3 extra/test/j2k/run_j2k_tests.py --live
```

The native libraries come from the natives jar of the host; `--bridge PATH` supplies a
`libjhvj2k` built by `native/jpeg2000/build.sh` while the jar has none.

The live test uses a fixed single-frame AIA 171 image from September 9, 2026. It retrieves
levels 2 and 0 through the reader and checks completion and the persisted level after each.
A second session restores from the disk cache after closing and reopening
its manager. Its socket is aborted after the metadata and the first coarse level, so the
tested resolutions cannot be downloaded again. Metadata and decoded pixel hashes are compared
between the sessions, not pinned across KDU versions. Allow up to three minutes and several
MB of network traffic. A service outage or removed fixture fails the test, rather than
silently skipping it.

The live suite requests a fixed ROB movie through the `getJPX` API so the server creates
it if needed, then uses the returned JPIP URI. It compares the first four frames using sequential
requests without the reader, the actual reader prefetch pump, reopened disk-cache restoration
with the socket aborted, and a session in which one cache entry is replaced by a block the
client refuses (the frame is fetched and stored again). A controlled signal after two sends
checks that two sent responses are drained before switching work. The pump is invoked directly
without a GUI view, while its worker remains idle. Completed or restored frames must reset
the reader's consecutive failure count within the pass.

The same movie is then opened by the real view, headless, with its reader thread. Paused and
first shown at its coarsest level, which opening already completed, the movie must still
download; a finer level of the shown frame must be fetched and delivered by the reader's
refresh. Playing, the whole movie is downloaded at a coarse level twice, once with the
connection aborted after a third of the frames; the reader must reconnect and the pixels of
every frame must be equal.
Reader refreshes must use the latest viewpoint, and a detached view's refresh must leave
the shared worker's replacement task current.

The view test captures opening responses and a finer window from that movie, then replays them
through a local server. Two failed passes must recover with an immediate first retry and a pause
before the second. Fourteen consecutive failed passes must exhaust retries and log the cause once,
even when every pass accepts byte-limited data before failing. A conflicting response must stop
the reader and purge this source's disk entries while preserving an unrelated entry. Equal-length
timestamp edits put frames out of order and make one frame use fallback metadata: disk keys must
use each frame's own timestamp, and the fallback frame must have no key. No capture folders are required.

The live suite also downloads a Callisto JP2 through the ROB API and checks horizontal
crops at the origin, interior, and right edge against a full-image decode. It exercises
all six Callisto resolution levels (0 through 5), including nonaligned requested regions.
Full-image hashes are printed for comparison when changing the decoder. These checks cover
grayscale pixels, not RGB composition or timeline drawing. The same fixture checks the shared
radio crop cache under its 438 MiB byte budget: eviction follows recency across seven days,
removing a day purges only its crops, and removed days cannot accept late decode results.
Displayed scalar pixels survive eviction and LUT changes. Immutable fixture arrays are reused
across day entries to exercise cache weights without allocating 438 MiB of pixels.

`run_native_test.sh` requires at least one JP2/JPX file argument and `KDU_VENDOR` to name a supplied Kakadu SDK.
It builds the bridge and checks it, through `J2KNative`, against Kakadu's
compositor as an independent decoder: geometry and pixels of every frame and level of the
given files, JPIP responses written by esajpip's server code, cache entries, refusals and
failures. The compositor's Java binding, `lib/kdu_jni.jar` here, is used by tests only.

For prebuilt KDU and bridge libraries, use the same native test without building
the server or generating JPIP responses:

```sh
python3 extra/test/j2k/check_native_build.py /path/to/platform-libraries --esajpip ../esajpip-SWHV
```

This checks committed RGB, grayscale and JPX fixtures and supports macOS, Linux
and Windows. On Windows it loads the core DLL before JNI/support and the bridge;
the file-descriptor count check applies only on Unix.

Test classes, extracted native libraries, and cache data use temporary directories, cleaned
up on normal exit. The live runner supports macOS and Linux x86-64.
The live suite has been exercised only on macOS arm64. No application settings or user cache are used.

This suite does not cover all reader lifecycle races, GUI rendering, all HTTP framing errors,
or GUI playback with 8 GiB of actual cached pixels. The ROB and movie reader checks decode raw pixels; the view check
uses the image metadata, but does not independently validate its geometry.
