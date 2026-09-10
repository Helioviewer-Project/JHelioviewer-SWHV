# JPIP tests

Requires JDK 25, Python 3, and compiled JHV classes. From the repository root:

```sh
ant compile
python3 extra/test/jpip/run_jpip_tests.py
```

The default suite is offline and needs no native library:

- Parser: explicit/inherited identifiers, supported class mapping, payloads, empty messages,
  numeric boundaries, truncated headers/data, invalid integers, and EOR completion reasons.
  It runs with a 64 MB heap to catch oversized allocation from a bogus payload length.
- HTTP streams: fixed-length and chunked bodies, response boundaries,
  zero-length reads at EOF, premature EOF, and draining a chunked body on close.
- Socket cleanup: a local server verifies graceful channel close and aborting a stalled
  response without sending another request. A stalled constructor handshake is interrupted
  on a virtual thread and must close TCP. Two queued requests are also aborted together.
  Pipelining checks response-to-frame matching, pending counts, partial/complete response order,
  and sequential use after draining.
  No external network or native library is needed.
- Cache serializer: round trips of every databin record component, empty streams, large identifiers, direct/read-only
  buffers, unchanged input positions, and rejection of truncated or invalid entries.
- Cache: failure to obtain the persistence lock leaves caching disabled without repeated logging.
  This replaces the older standalone cache test, updating its retired cache directory name.

To also retrieve from ROB using the actual JPIP socket, native cache, and decoder:

```sh
python3 extra/test/jpip/run_jpip_tests.py --live
```

The live test uses a fixed single-frame AIA 171 image from September 9, 2026. It opens
independent sessions with the normal response limit and a 16 KB limit, retrieves levels
2 and 0, checks completion and dimensions, and compares metadata and decoded pixel hashes.
A third session restores levels 2 and 0 from the disk cache after closing and reopening
its manager. Its socket is closed after metadata/64x64 initialization, before restoration
and decoding, so the tested resolutions cannot be downloaded again. It also checks resolution
upgrades, rejection of a coarse cache entry for a finer request, and clearing the cache.
This tests persistence within the current build, not compatibility with older cache formats.
The limited session must require multiple requests. Hashes are compared between sessions,
not pinned across KDU versions. Allow up to three minutes and several MB of network traffic.
A service outage or removed fixture fails the test, rather than silently skipping it.

The live suite also compares the first four frames of a fixed ROB movie using sequential
requests, the actual reader prefetch pump, and reopened disk-cache restoration. A controlled
signal after two sends checks that two sent responses are drained before
switching work. The pump is invoked directly without a GUI view, while its worker remains idle.
This does not exercise the outer reader retry loop or GUI priority refresh.

Test classes, extracted native libraries, and cache data use temporary directories, cleaned
up on normal exit. The live runner supports the bundled macOS and Linux x86-64 libraries.
Only macOS arm64 has been exercised. No application settings or user cache are used.

This is an initial suite. It does not cover all reader lifecycle races,
GUI rendering, all HTTP framing errors, or cache eviction. Parser field assertions use reflection
to keep production methods private. Live decoding supplies only the coordinate conversion
needed by the unfiltered decoder, so it does not validate metadata geometry.

Current JHV is the maintained reference. No historical adapters are retained.
