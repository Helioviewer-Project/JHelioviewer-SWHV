# Image Buffer Cache

`ImageBufferCache` stores decoded image data so that JHV can reuse it instead of
decoding the same frame, region, resolution level, and filter combination again.
This matters for playback, scrubbing, and synchronized layer updates: the viewer
often returns to data that was decoded only moments earlier, and re-decoding that
data is much more expensive than keeping it in a bounded cache.

The cache stores `ImageBuffer` instances. An `ImageBuffer` owns image pixels in
native memory, because GL upload can consume native buffers directly. The cache
therefore has two responsibilities: it avoids unnecessary decoding, and it keeps
the native memory used by decoded images within a predictable budget.

Each `ImageBuffer` also registers a fallback cleanup action with Java's
`Cleaner` API. That cleanup action runs after the JVM proves that the registered
object is no longer reachable, and it releases the native memory if normal cache
cleanup did not get there first. This is only the last safety net, not a cache
eviction strategy. Playback tests with working sets larger than the cache showed
that native allocations can outrun GC discovery of unreachable buffers,
eventually exhausting OS virtual memory. The fallback cleanup is therefore
deliberately conservative: it is registered on the direct `ByteBuffer` or
`ShortBuffer` owned by the `ImageBuffer`, and it frees the recorded native
address only after that buffer object is unreachable.

## Background

Before JHV 5, image upload went through JOGL APIs that accepted Java heap
buffers. If GL needed native memory, the heap-to-native copy was hidden inside
the JOGL upload path. That made the application code simpler, but it also hid a
large copy on the EDT/render thread, and JHV had no explicit object representing
the lifetime of the native upload storage.

JHV 5 made the upload buffer explicit. Views publish `ImageBuffer` instances, and
`GLImage` uploads from those buffers. For the common unfiltered J2K path, the
Kakadu compositor output is gathered directly into the final native
`ImageBuffer`, avoiding both the old hidden EDT upload copy and an intermediate
heap-to-native copy in the decoder.

The cache was centralized at the same time. Decoded image caches that had been
local to `J2KView` and `URIView` now share `ImageBufferCache`, making the memory
budget global and measurable. The tradeoff is that eviction has to respect data
that may already have left the cache but has not yet reached its final owner on
the EDT.

The cleanup contract is mainly about that handoff window. A buffer can leave the
cache, be queued toward the EDT, and only later be accepted by layer/rendering
state. During that interval, cache eviction may happen, but explicit native
cleanup must not.

## Decode Paths

Not every decoder can produce an `ImageBuffer` in the same way. The policy is to
write native buffers directly where that removes a hot-path copy, and to keep
heap arrays where Java-side decoding or filtering needs array access.

Unfiltered J2K data is the critical playback path. `J2KDecoder` writes Kakadu
compositor output directly into the final native `ImageBuffer`, so the decoded
data is already in the form needed by GL upload.

Filtered J2K data goes through a heap array. `ImageFilter` operates on Java
arrays, so the decoder builds the array first and constructs the final
`ImageBuffer` after filtering.

FITS decoding starts from Java-accessible FITS pixel storage. The reader does
scaling, clipping, and blank-pixel handling in Java, then writes display pixels
through `ImageBuffer.WriteBuffer`. With `ImageFilter.None`, that write buffer is
already the final native `ImageBuffer`. Filtered FITS data uses a heap array
first, because `ImageFilter` operates on arrays.

Generic image loading goes through `ImageIO` and `BufferedImage`. Gray images can
reuse raster arrays. Other image formats are converted through
`NativeImageFactory.createRGBAPremultipliedImage()`, because Java does not
provide a standard byte-RGBA-premultiplied `BufferedImage` type. That RGBA path
currently copies native image data back to a heap array before creating an
`ImageBuffer`. This could be improved, but it is not the critical AIA playback
path.

## Cleanup Policy

Cache eviction does not close an `ImageBuffer` immediately. It retires the
buffer. Retired buffers are then reaped later, once the rendering code reports
that they are no longer retained.

The important distinction is between a buffer that merely exists transiently and
a buffer that has entered the live image/render path. Only the latter is part of
the deterministic cleanup contract. Rare buffers that are queued for delivery
but never accepted by an `ImageLayer` are left to the `Cleaner` fallback.

This keeps the cache cleanup path deliberately narrow. `ImageBufferCache` owns
cache membership and retired-buffer cleanup; it does not try to model every
temporary reference held by worker threads or the EDT.

## Ownership Contract

- `ImageBufferCache.get()` and `ImageBufferCache.put()` are plain cache
  operations. Cache access does not imply UI ownership.
- Cache eviction retires an `ImageBuffer`; it does not immediately free the
  native memory.
- `J2KView` and `URIView` protect an `ImageBuffer` from explicit cleanup only
  when handing it toward `View.DataHandler.handleData()`.
- `ImageLayer.handleData()` clears that protection only after accepting the
  buffer into layer state.
- Non-layer handlers that copy the image data and do not retain the
  `ImageBuffer`, such as `RadioJ2KData`, must clear that protection after the
  copy.
- `Layers` collects buffers currently retained by `ImageLayer` and by uploaded
  `GLImage` state.
- `ImageBufferCache.reap()` may explicitly free retired buffers only when they
  are not protected and not in that retained set.
- Retired buffers are tracked weakly, so failed or dropped handoffs do not keep
  buffers alive just because they passed through the cache cleanup machinery.

## Fragile Points

The following changes look local, but alter ownership semantics:

- Treating `get()` or `put()` as UI ownership would conflate cache access with
  publishing a buffer to a layer.
- Freeing directly from the Caffeine removal listener would be too early,
  because the buffer may be queued for EDT delivery or uploaded in `GLImage`.
- Grace periods are deliberately avoided. A fixed number of render passes is a
  guess, not an ownership rule.
- Failed handoffs are not tracked exhaustively. They are rare strays and are
  left to the weak reference plus `Cleaner` fallback by design.

Changes to this contract should be reviewed across the whole decode-to-render
path, not just in the class being edited. In particular, check `J2KView`,
`URIView`, `ImageLayer`, `Layers`, `GLImage`, and any non-layer
`View.DataHandler` implementation that receives `ImageBuffer`.
