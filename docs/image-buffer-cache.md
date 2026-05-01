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

The `Cleaner` registered by `ImageBuffer` remains the final safety net. It is not
the primary cache-eviction mechanism. Playback tests whose decoded working set is
larger than the cache can otherwise leave too much native memory alive while the
VM waits for GC to discover unreachable buffers.

## Historical Context

Before JHV 5, image upload went through JOGL APIs that accepted Java heap
buffers. If GL needed native memory, the copy from heap to native storage was
hidden inside the JOGL upload path. That made the application code simpler, but
it also meant that a large copy could happen on the EDT/render thread, and the
lifetime of that native upload storage was not represented in JHV code.

JHV 5 made the upload buffer explicit. Views publish `ImageBuffer` instances, and
`GLImage` uploads from those buffers. For the common unfiltered J2K path, the
Kakadu compositor output is gathered directly into the final native
`ImageBuffer`, avoiding both the old hidden EDT upload copy and an intermediate
heap-to-native copy in the decoder.

The cache was also centralized. Decoded image caches that had been local to
`J2KView` and `URIView` now share `ImageBufferCache`, which makes the memory
budget global and measurable. The price is that eviction has to respect data
that may already have left the cache path but not yet reached its final owner on
the EDT.

The cleanup contract exists for that handoff window. Eviction must be aggressive
enough to free native memory without waiting for GC, but it must not free a
buffer that is still queued for publication, retained by an `ImageLayer`, or
uploaded by `GLImage`.

## Decode Paths

The producers of `ImageBuffer` do not all have the same constraints.

Unfiltered J2K data is the critical playback path. `J2KDecoder` writes Kakadu
compositor output directly into the final native `ImageBuffer`, so the decoded
data is already in the form needed by GL upload.

Filtered J2K data still goes through a heap array. `ImageFilter` operates on Java
arrays, so the decoder builds the array first and constructs the final
`ImageBuffer` after filtering.

FITS decoding also builds heap arrays. The FITS reader performs scaling,
clipping, blank-pixel handling, and optional filtering in Java before producing
the display buffer, so heap storage is still the natural working representation.

Generic image loading goes through `ImageIO` and `BufferedImage`. Gray images can
reuse raster arrays. Other image formats are converted through
`NativeImageFactory.createRGBAPremultipliedImage()`, because Java does not
provide a standard byte-RGBA-premultiplied `BufferedImage` type. That RGBA path
currently copies native image data back to a heap array before creating
`ImageBuffer`; this is avoidable in principle, but it is not the critical AIA
playback path.

## Cleanup Policy

Cache eviction does not immediately close an `ImageBuffer`. Instead,
`ImageBufferCache` retires evicted buffers and later reaps retired buffers whose
native memory can be freed safely.

The important distinction is between a buffer that merely exists transiently and
a buffer that has entered the live image/render path. Only the latter is part of
the deterministic cleanup contract. Rare buffers that are queued for delivery
but never accepted by an `ImageLayer` are left to the normal `Cleaner` fallback.

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

## Maintenance Notes

Several tempting changes break the ownership model:

- Treating `get()` or `put()` as UI ownership would conflate cache access with
  publishing a buffer to a layer.
- Freeing directly from the Caffeine removal listener would be too early,
  because the buffer may still be queued for EDT delivery or uploaded in
  `GLImage`.
- Grace periods are deliberately avoided. A fixed number of render passes is a
  guess, not an ownership rule.
- Failed handoffs are not tracked exhaustively. They are rare strays and are
  left to the weak reference plus `Cleaner` fallback by design.

Changing this contract requires checking all decode-to-layer handoff paths and
all render-side references that can keep an `ImageBuffer` alive. In particular,
check `J2KView`, `URIView`, `ImageLayer`, `Layers`, and `GLImage` together.
