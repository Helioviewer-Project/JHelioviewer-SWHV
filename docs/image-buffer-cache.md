# Image Buffer Cache

`ImageBufferCache` shares decoded solar images between `J2KView` and `URIView`.
It stores `DecodedImage` instances, each containing an `ImageBuffer` with native
pixels and a `Region` describing their physical extent. Reusing these images
avoids decoding and filtering again during playback and scrubbing.

## Keys and eviction

Each key implements `ImageBufferCache.Key` and identifies its owning source:

- J2K keys contain the view's serial number, frame, resolution level, and filter.
- URI keys contain the data URI, filter, FITS processing parameters, and clip range.

The Guava cache has an 8 GiB byte budget, weighted by the native pixel storage.
It uses least-recently-used eviction with one shared budget. Newly decoded images
are admitted without a frequency-based admission rule. Images larger than the
budget cannot remain cached.

Clearing a view removes its keys. `ManyView` collects the owners of its children
and removes their entries in one pass, including when a layer contains thousands
of local files. Closing native sources is separate from removing decoded images.

The budget covers cached pixels. Images retained by a layer or a pending delivery
can remain allocated after eviction, and in-flight decodes also allocate outside
of the cache budget.

## Ownership and cleanup

`DecodedImage` has explicit reference ownership. The decoder creates it with one
reference. Once published, references are retained and released on the EDT.

1. `LatestWorker` runs decoding off the EDT and posts the result callback to the EDT.
2. `BaseView.decodeCallback` closes a result if the view is detached or its
   processing settings have changed. Otherwise it transfers the decoder's
   reference to the cache. A valid superseded result can be cached without being
   delivered.
3. `ImageBufferCache.get` returns a borrowed image for the current EDT turn.
   Keeping it beyond that turn requires `retain()`.
4. `BaseView.sendDataToHandler` retains a reference for its queued delivery and
   releases it in `finally`, even if the view has detached before delivery.
5. `ImageLayer` retains a reference for each occupied current, previous, or base
   image slot. Replacing a slot releases its previous image. Detaching the view
   clears all three slots immediately.
6. Cache replacement, eviction, and explicit invalidation post `release()` to the
   EDT. This preserves images borrowed earlier in that EDT turn.
7. The last `release()` frees the native pixels. Retaining or releasing an already
   released image throws `IllegalStateException`, including with assertions disabled.

CPU pixel cleanup does not require repainting. An invalidation's queued cache
release runs on the EDT; retained layer and delivery references keep an image
alive only until their own release. GL resource disposal still requires a render
context and is separate from CPU pixel ownership. `GLSLImage` keeps buffer
identity to avoid redundant uploads; the layer slots own the pixels it uploads.

`ImageBuffer` also registers a `Cleaner` action on its direct buffer as a fallback
for unreachable allocations. Explicit release invokes that action directly.
Normal cache and layer cleanup uses reference ownership rather than waiting for GC.

## Decode paths

- Unfiltered J2K decoding writes directly into the final native buffer. The view
  acquires the native decode job before allocating output pixels.
- Gray J2K filtering reads that buffer into Java arrays and creates a half-float
  output buffer. `ImageBuffer.WriteBuffer.finish(filter)` frees the input even if
  filtering fails. RGBA data bypasses gray-image filtering.
- FITS decoding writes display pixels through `ImageBuffer.WriteBuffer`, applying
  the same filtering and ownership transfer when the buffer is finished.
- Generic gray and indexed images use raster arrays. Other images are converted
  to premultiplied RGBA native storage, copied into the final write buffer, and
  the conversion storage is freed in `finally`.

An unfinished `WriteBuffer` frees its pixels on `close()`. After `finish()`, the
returned image owns those pixels and closing the writer does not free them.

Radio spectrograms use a separate path: `J2KSource.decode` returns scalar heap
bytes, freeing its temporary native buffer in `finally`. `RadioData` caches those
crops with soft values and a byte budget, while the displayed `BufferedImage`
holds its current crop. Colormap changes reuse the scalar pixels. Radio does not
participate in `DecodedImage` reference ownership or `ImageBufferCache`.
