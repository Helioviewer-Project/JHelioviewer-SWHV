# JPEG 2000 and JPIP tests

Requires JDK 25, Python 3, and compiled JHV classes. From the repository root:

```sh
ant compile
python3 extra/test/j2k/run_j2k_tests.py
```

The default suite is offline and needs no native library:

- HTTP streams: fixed-length and chunked bodies, response boundaries,
  zero-length reads at EOF, premature EOF, and draining a chunked body on close.
- Socket: a local server checks the channel request and its response framing, refused
  channels, the metadata and frame request lines, two requests sent ahead with their bodies
  returned in order, a chunked body larger than the receive buffer, graceful channel close,
  aborting a stalled response without sending another request, and a stalled handshake
  interrupted on a virtual thread, which must close TCP.
- Cache serializer: round trip of an entry, direct/read-only buffers, unchanged input
  positions, and rejection of an entry without its level. The block of an entry is the
  client's; the client refuses a damaged one on import.
- Cache: failure to obtain the persistence lock leaves caching disabled without repeated logging.
  With the lock released: entries by level, replacement only by a finer level, removal.

To also retrieve from ROB using the actual JPIP socket, client, disk cache and decoder:

```sh
python3 extra/test/j2k/run_j2k_tests.py --live
```

The native libraries come from the natives jar of the host; `--bridge PATH` supplies a
`libjhvj2k` built by `native/jpeg2000/build.sh` while the jar has none.

The live test uses a fixed single-frame AIA 171 image from September 9, 2026. It retrieves
levels 2 and 0 through the reader, checks completion, the persisted level after each and the
decoded dimensions. A second session restores from the disk cache after closing and reopening
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
without a GUI view, while its worker remains idle.

The same movie is then opened by the real view, headless, with its reader thread. Paused and
first shown at its coarsest level, which opening already completed, the movie must still
download; a finer level of the shown frame must be fetched and delivered by the reader's
refresh. Playing, the whole movie is downloaded at a coarse level twice, once with the
connection aborted after a third of the frames; the reader must reconnect and the pixels of
every frame must be equal.

The live suite also downloads a Callisto JP2 through the ROB API and checks horizontal
crops at the origin, interior, and right edge against a full-image decode. It exercises
all six Callisto resolution levels (0 through 5), including nonaligned requested regions.
Full-image hashes are printed for comparison when changing the decoder. These checks cover
grayscale pixels, not RGB composition or timeline drawing.

`run_native_test.sh` builds the bridge and checks it, through `J2KNative`, against Kakadu's
compositor as an independent decoder: geometry and pixels of every frame and level of the
given files, JPIP responses written by esajpip's server code, cache entries, refusals and
failures. The compositor's Java binding, `lib/kdu_jni.jar` here, is used by tests only.

Test classes, extracted native libraries, and cache data use temporary directories, cleaned
up on normal exit. The live runner supports macOS and Linux x86-64.
Only macOS arm64 has been exercised. No application settings or user cache are used.

This suite does not cover all reader lifecycle races, GUI rendering, all HTTP framing errors,
or cache eviction. Live decoding supplies only the coordinate conversion needed by the
unfiltered decoder, so it does not validate metadata geometry.
