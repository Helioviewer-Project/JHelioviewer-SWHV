# Image Buffer Cache

`ImageBuffer` stores image data in native memory. The `Cleaner` is the final
safety net, but relying only on GC can make memory grow badly during playback
tests whose decoded working set is larger than the cache.

The cache therefore has an explicit cleanup path for evicted buffers. That path
is deliberately narrow: `ImageBufferCache` owns cache membership, not every
temporary `ImageBuffer` reference created during decode or UI handoff.

The important distinction is between a buffer that merely exists transiently and
a buffer that has actually entered the live image/render path. Only the latter
is part of the deterministic cleanup contract.

## History

Before JHV 5, image upload went through JOGL APIs that accepted Java heap
buffers. Any copy into native memory was hidden inside the GL binding and its
lifetime was not represented in JHV code. In practice, that hidden
heap-to-native copy happened on the GL upload path, i.e. on the EDT/render
thread.

JHV 5 made that native memory explicit. `ImageBuffer` now owns native storage
used by GL upload. This is also how JHV avoids an extra heap-to-native copy on
the upload path: views publish `ImageBuffer` instances whose data is already in
native memory. The remaining producer-side heap-to-native copy is therefore paid
by the decoder/loader thread instead of by the EDT during texture upload. Cache
eviction is therefore no longer just about dropping Java heap arrays and waiting
for GC. A large playback working set can otherwise leave native memory alive
until the `Cleaner` eventually runs.

At the same time, the decoded image caches that had been local to `J2KView` and
`URIView` were centralized in `ImageBufferCache`. That made the memory budget
global and measurable, but it also means eviction can happen while decoded data
is in transit from a worker/cache hit to the EDT and then to `ImageLayer`.

The current contract exists because of that transition: explicit native cleanup
is necessary, but it must not free a buffer that is still queued for publication
or still uploaded by `GLImage`.

## Cleanup Policy

Evicted cache entries are retired for explicit cleanup, but only buffers that
actually reach the live image/render path are guaranteed to be cleaned up
deterministically. Rare buffers that are queued for delivery but never accepted
by an `ImageLayer` are left to the normal `Cleaner` path.

This is intentional. The cache cleanup path must stay simple and must not try to
model every transient reference held by worker threads or the EDT.

## Contract

- `ImageBufferCache.get()` and `ImageBufferCache.put()` are plain cache
  operations.
- Cache eviction does not immediately free native memory. It retires the buffer
  and lets `ImageBufferCache.reap()` decide when explicit cleanup is safe.
- `J2KView` and `URIView` protect an `ImageBuffer` from explicit cleanup only
  when handing it toward `View.DataHandler.handleData()`.
- `ImageLayer.handleData()` clears that protection only after accepting the
  buffer into the layer state.
- Non-layer handlers that copy the image data and do not retain the
  `ImageBuffer`, such as `RadioJ2KData`, must clear that protection after the
  copy.
- `Layers` collects buffers currently retained by `ImageLayer` and by the
  uploaded `GLImage` state.
- `ImageBufferCache.reap()` may explicitly free retired buffers only when they
  are not protected and not in that retained set.
- Retired buffers are tracked weakly, so failed or dropped handoffs do not keep
  buffers alive just because they passed through the cache cleanup machinery.

## Do Not Widen The Contract Casually

Several changes look simpler but are wrong:

- Do not make `get()` or `put()` imply UI ownership. Cache access is not the
  same thing as publishing a buffer to a layer.
- Do not free directly from the Caffeine removal listener. At that point the
  buffer may still be queued for EDT delivery or uploaded in `GLImage`.
- Do not add grace periods. A fixed number of render passes is a guess, not an
  ownership rule.
- Do not try to track every failed handoff. Those are rare strays and are left
  to the weak reference plus `Cleaner` fallback by design.

Changing this contract requires checking all decode-to-layer handoff paths and
all render-side references that can keep an `ImageBuffer` alive. In particular,
check `J2KView`, `URIView`, `ImageLayer`, `Layers`, and `GLImage` together.
