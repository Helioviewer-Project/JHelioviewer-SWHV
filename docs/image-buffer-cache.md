# Image Buffer Cache

`ImageBufferCache` holds decoded solar images for every image layer. Each entry is a
`DecodedImage`: an `ImageBuffer` with native pixels and a `Region` giving their physical
extent. A cache hit during playback or scrubbing skips decoding and filtering.

## Keys and budget

Each key implements `ImageBufferCache.Key` and names its owner. `FrameDecoder` makes one
key record for every source kind: the timeline's installation serial, the frame's index in
the timeline, the resolution level, the filter, the FITS processing parameters, and the
clip range. The last two are null for sources that have no use for them, so the key is as
narrow as the source needs. The owner is the installation serial. No `Source` object is in
a key, so cached pixels never keep a native client reachable.

The Guava cache has an 8 GiB byte budget, weighted by the native pixel storage. Eviction is
least-recently-used with one shared budget and no frequency-based admission. An image
larger than the budget cannot remain cached.

The budget covers cached pixels only. Images retained by a layer or by a pending delivery
stay allocated after eviction, and running decodes allocate outside the budget.

## Owners

Every `Frames` draws a serial at construction and reports it as the owner of its images.
Clearing a timeline's cache is one pass over the keys with that owner. `Frames.close()`
performs that pass, then closes the sources on a thread; a timeline over a thousand local
files still costs one scan. A processing change on a layer clears the same owner through
`Frames.clearCache()` before the layer decodes again.

A `Cleaner` registered on `Frames` runs the same two steps for a timeline dropped without
`close()`. Its action holds the serial and the sources and nothing else; sources hold their
listener weakly, so the action cannot reach the timeline it is meant to retire.

Replacing a layer's timeline resets the decoder to the new serial before the old timeline
is closed. A result that was decoding for the old serial is released on arrival instead of
cached or delivered.

## Reference ownership

`DecodedImage` has explicit reference ownership. The decoder creates it with one reference.
Once published, references are retained and released on the EDT only.

1. `LatestWorker` runs the decode off the EDT and posts the callback to the EDT. A repeated
   cache miss for the same key reuses the outstanding work with the latest callback,
   including its viewpoint. Different work can decode while earlier results await
   publication.
2. The callback in `FrameDecoder.redecode` releases a result whose timeline or processing
   settings have changed since the request. Otherwise it transfers the decoder's reference
   to the cache. A valid superseded result is cached without being delivered.
3. `ImageBufferCache.get` returns a borrowed image for the current EDT turn. Keeping it
   beyond that turn requires `retain()`.
4. `FrameDecoder.deliver` retains a reference for the queued delivery and releases it in
   `finally`, even when the layer has moved on before the delivery runs.
5. `ImageLayer` retains a reference for each occupied current, previous and base image
   slot. Replacing a slot releases its previous image. Detaching the layer clears all three.
6. Cache replacement, eviction and explicit invalidation post `release()` to the EDT, which
   preserves images borrowed earlier in the same turn.
7. The last `release()` frees the native pixels. Retaining or releasing an already released
   image throws `IllegalStateException`, with assertions disabled too.

CPU pixel cleanup needs no repaint. GL resource disposal still needs a render context and is
separate from pixel ownership. `GLSLImage` keeps buffer identity to avoid redundant uploads;
the layer slots own the pixels it uploads.

`ImageBuffer` also registers a `Cleaner` action on its direct buffer as a fallback for
unreachable allocations. Explicit release runs that action directly. Normal cache and layer
cleanup relies on reference ownership, not on garbage collection.

## Decode paths

The decoder computes the image region from the frame's metadata and the source's full
level, builds the filter, and calls `Source.decode(frame, level, filter, fits, clip)` on the
worker. Each source produces an `ImageBuffer`:

- `J2KSource` acquires the native decode job before allocating output pixels and writes an
  unfiltered gray or RGBA image straight into the final native buffer. Gray filtering reads
  that buffer into Java arrays and produces a half-float output; `WriteBuffer.finish(filter)`
  frees the input even when filtering fails. RGBA data bypasses gray filtering.
- `FITSSource` reads the file again for each decode and writes display pixels through a
  `WriteBuffer`, with the same filtering and ownership transfer at `finish`.
- `RasterSource` uses raster arrays for gray and indexed images. Other images are converted
  to premultiplied RGBA native storage, copied into the final write buffer, and the
  conversion storage is freed in `finally`.

An unfinished `WriteBuffer` frees its pixels on `close()`. After `finish()` the returned
image owns those pixels and closing the writer does not free them.

Radio spectrograms use a separate path: `J2KSource.decodeRegion` returns scalar heap bytes,
freeing its temporary native buffer in `finally`. `RadioData` caches those crops with soft
values and a byte budget, while the displayed `BufferedImage` holds its current crop.
Colormap changes reuse the scalar pixels. Radio takes no part in `DecodedImage` ownership or
in `ImageBufferCache`.
