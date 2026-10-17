# Image coordinates from source to screen

JHV positions every decoded image through one pixel-coordinate model. Metadata maps original image
pixels to the WCS projection plane. A decoder grid says where the decoded samples lie in those original
pixels. The two compose into the geometry that filtering and texture sampling use.

The image footprint does not depend on the decode level. A coarser decode moves the sample positions
and shrinks the texture. It does not move detector masks, slits or cutout bounds.

## Pixel and texture conventions

Original image coordinates start at the upper-left edge. X increases along a row and Y follows the buffer
rows. An image of `W × H` pixels occupies `[0,W] × [0,H]`. The center of pixel `(i,j)` is `(i+0.5, j+0.5)`.
These coordinates are relative to the image, whatever its origin on a JPEG 2000 canvas.

Decoded buffers use the same convention on their own grid. Normalized texture coordinates divide buffer
coordinates by the buffer size, so decoded sample `(i,j)` is read at `((i+0.5)/Wr, (j+0.5)/Hr)`.

Texture row zero sits at texture coordinate `v = 0`. Where that row appears on screen follows from the
image geometry alone; the texture-coordinate path contains no vertical flip. `FITSData` writes FITS rows
into this top-down order, and `FitsMetaData` converts the reference pixel to match. Framebuffer coordinates
have their origin at the lower left and belong to the viewport and screen transforms, not to this model.

## Metadata to original image geometry

[`WcsInterpreter`](../src/org/helioviewer/jhv/metadata/WcsInterpreter.java) selects the two image axes;
SPICE and PUNCH JP2 headers can retain additional WCS axes from their original FITS cubes. It adapts
missing units, the known legacy axis pairs `Solar-X`/`Solar-Y` (also underscore spellings), `GONGX`/`GONGY`,
`arcsec`/`arcsec` and LASCO's `0`/`0` to `HPLN-TAN`/`HPLT-TAN`, ignoring case. Unit aliases include
`degree(s)` and LASCO's `0` (arcseconds). Historical CEA rows with missing `CUNIT2`
are normalized Y coordinates; their celestial `CRVAL2` remains in degrees. Observer CROTA fallbacks
are also adapted into FITS cards. Missing CDELT scales still require a CD matrix, preserving the loader's
`BasicMetaData` fallback for images without calibrated geometry. WCSLIB interprets the
linear transform, projection parameters and celestial/native frame. PC with CDELT takes precedence over
CD, and CD over CDELT with CROTA. Rotation and signed scales remain in the full matrix.

[`ImageGeometry`](../src/org/helioviewer/jhv/wcs/ImageGeometry.java) converts the FITS convention when
constructed from a WCS solution: pixel centers are integers from one and Y increases upward. For image
height `H` the reference pixel in buffer coordinates is:

```text
c.x = CRPIX1 - 0.5
c.y = H - (CRPIX2 - 0.5)
```

The native solution supplies the matrix in plane units: solar radii for observer images, radians of
longitude and latitude for CAR maps, and longitude with the equal-area latitude coordinate for CEA maps.
`ImageGeometry` changes the second matrix column's sign for downward Y. LASCO keeps its rule that drops
the metadata rotation, because its pixels are already rotated. `FitsMetaData` selects that policy;
`ImageGeometry` performs the conversion.

[`ImageGeometry`](../src/org/helioviewer/jhv/wcs/ImageGeometry.java) holds the complete linear mapping.
With pixel coordinate `P`, reference pixel `c`, matrix `A = pixelToPlane` and plane point `p`:

```text
p = A · (P - c)
P = inverse(A) · p + c
```

[`WcsSolution`](../src/org/helioviewer/jhv/wcs/WcsSolution.java) owns a prepared native WCSLIB solution.
Its exported coefficient and frame arrays are read-only by convention.
Its FITS linear transform and reference pixel remain package-private for `ImageGeometry` to consume.
It exposes the celestial reference in radians, effective float PV coefficients,
native-frame basis, projection-plane origin offset and plane units per radian. CPU `worldToPlane()` and
batched `planeToWorld()` use that same owner. `unitsPerArcsec()` derives the arcsecond scale from it.
CRVAL describes a celestial reference; it is separate from both pixel reference `c` and native plane origin.

Decoded geometries share the native owner with their original geometry. An automatic FFM arena releases
the native solution after its last Java owner becomes unreachable. User CRVAL adjustments make a native
copy, recompute pole defaults and cache the last adjustment; ordinary frames reuse the solution.
[`ObserverGeometry`](../src/org/helioviewer/jhv/wcs/ObserverGeometry.java) retains only JHV's directed observer
ray, sphere intersection and Sun-plane conversions. HPC supplies an angular ray; warped-view picking
supplies its Sun-plane ray directly to the same intersection, without an angular round trip. Native builds
and the supported FITS subset are documented in [the WCSLIB guide](../extra/wcs/README.md).

`BasicMetaData` supplies a centered geometry with uniform pixel spacing when metadata is missing.
`ImageBounds` maps the image corners and edge midpoints through the geometry and projection to obtain the
camera bounds. `ImageGeometry.bounds` is the original image's axis-aligned rectangle in plane units used
for sizing; decoded geometries share that same footprint. The matrix and projection, not that rectangle,
decide where the image lies in the scene. CAR/CEA radial bounds use the apparent limb projected onto the
Sun-center plane: `D / sqrt((D-1)*(D+1))`, with displayed observer distance `D`.
Observer images use the projected boundary's largest radius,
falling back to rectangle corners relative to the Sun when no projected radius is usable. HPC bounds
use the projected boundary for observer images. These sampled fitting extents are approximate. Invalid
WCS samples are omitted; if no perimeter sample is valid, HPC fitting uses the full celestial angular
domain rather than failing on an image whose valid region may lie inside its perimeter.
CAR/CEA maps instead contribute the solar disk's angular
extent, `asin(1 / D)`, with that same displayed distance. `GLRenderer` passes the displayed distance through
`ImageLayers` for HPC/warp scales, and `DisplayController.zoomOneToOne()` uses it for the surface angular
height. Observer-image bounds retain each source's metadata distance. Surface longitude and latitude coverage
is not an observer-image field of view.

Orthographic fit/reset uses diameter 2 for CAR/CEA, including partial maps. Observer images
contribute their original plane height; enabled layers contribute their maximum height.

## Decoder output to original pixels

[`ImageGrid`](../src/org/helioviewer/jhv/wcs/ImageGrid.java) describes a decoded buffer: its level, its
actual width and height, and the position of its first sample center in original pixels. Level `r` has
spacing `s = 2^r` on both axes.

Let `F` be that first sample center and `Q` a decoded buffer coordinate. On each axis:

```text
P = F + s · (Q - 0.5)
Q = (P - F) / s + 0.5
```

For a zero-origin JPEG 2000 image `F = 0.5`, so the reduced samples sit at `0.5`, `s+0.5`, `2s+0.5`, …
This is where the samples are, not how they are computed. A reduced wavelet reconstruction is not a copy
of every `s`th pixel.

The actual dimensions matter. For a canvas interval `[X0,X1)` the decoder grid is:

```text
Xr  = ceil(X0 / s)
Wr  = ceil(X1 / s) - Xr
F.x = s · Xr - X0 + 0.5
```

Y follows the same rule. The native bridge reports the full-resolution origin and each level's dimensions.
[`J2KNative.frame`](../src/org/helioviewer/jhv/source/J2KNative.java) turns them into `ImageGrid` values
at the binding boundary, so Kakadu's absolute coordinates stop there. Decode requests are relative to the
level; the bridge adds the level origin.

Example: a zero-origin image 129 pixels wide has 17 samples at level 3, centered at `0.5, 8.5, …, 128.5`.
Stretching 17 texels between the edges 0 and 129 gives other centers and another spacing. Even with
divisible sizes the stretch is wrong: 4096 pixels stretched into 2048 texels put the first center at 1
instead of 0.5.

The native geometry reader accepts components with unit sampling, zero registration and a common grid.
Components that would need resampling are rejected. FITS and raster sources expose an identity grid at
level 0.

## Composition, decoding and caching

`Frames.Frame.geometry()` reconciles the metadata size with the source's actual full-resolution size.
When they differ, `withSize` keeps the matrix and reference pixel and changes the extent. `FrameDecoder`
picks the level from the display size and decodes the finest complete level at that level or coarser.

On a cache miss `FrameDecoder.decodeImage` composes the frame geometry with the chosen grid through
`ImageGeometry.forGrid`. Substituting the grid equation into the pixel mapping gives:

```text
Adecoded = s · A
cdecoded = (c - F) / s + 0.5
p = Adecoded · (Q - cdecoded)
```

The composed geometry keeps a reference to its original through `original`. The identity grid returns the
original object. Pixels and composed geometry are cached together as one `DecodedImage`.

```mermaid
flowchart TD
    M[Metadata] --> O[Original ImageGeometry]
    N[Source size and sample grid] --> G[ImageGrid]
    O --> D[FrameDecoder composes]
    G --> D
    D --> F[Decode and filter]
    F --> C[Cached DecodedImage]
    C --> R[ImageLayer and GLSLImageShader]
    R --> S[Scene point to WCS plane]
    S --> P[Original then decoded texture coordinates]
    P --> T[Clip, sample, sharpen, LUT]
```

A cache hit reuses pixels and geometry. It does not decode, filter, compose or read native geometry again.
Viewpoint and display adjustments apply at render time. Buffer ownership and cache lifetime are described
in [Image Buffer Cache](image-buffer-cache.md).

## RHEF sample positions

RHEF runs on the decode worker before upload, with the composed geometry its output pixels will carry.
MGN and WOW do not use geometry.

The original geometry locates the Sun in original pixels by projecting helioprojective `(0,0)` to the
plane and applying the inverse mapping. Composition carries that position onto the decoded grid:

```text
sunDecoded = (sunOriginal - F) / s + 0.5
```

[`FilterRHEF`](../src/org/helioviewer/jhv/image/FilterRHEF.java) takes the X and Y spacings from the
lengths of the composed matrix columns. For decoded sample `(i,j)`:

```text
dx = (i + 0.5 - sunDecoded.x) · pixelSizeX
dy = (j + 0.5 - sunDecoded.y) · pixelSizeY
radius = sqrt(dx² + dy²)
```

The annuli therefore follow the real reduced grid, offset from the image edges included. Dividing the
original extent by the decoded size would place the samples elsewhere.

## Scene coordinates to texture reads

[`ImageLayer.render`](../src/org/helioviewer/jhv/layers/ImageLayer.java) passes both image geometries,
the observer orientations, the time differences and the display adjustments to `GLSLImageShader.render()`.
It uploads both image records, binds the matching WCS coefficients and draws the selected mode in one
operation. A user CROTA adjustment rotates the plane point before the inverse mapping. CRVAL adjustments change the
celestial reference, or the scaled reference subtraction for Orthographic TAN. Neither rewrites cached geometry.

The mapping mode decides how a fragment reaches the source plane:

- **Orthographic** starts on the visible solar sphere or the image plane. Observer rotation and
  differential rotation pick the source point. TAN deliberately uses simple-TAN: subtract the celestial
  CRVAL scaled by `unitsPerRad` from the point's X and Y directly. ARC, AZP and ZPN project the observer
  direction through the formal WCS.
- **HPC** starts from helioprojective longitude and latitude inside the displayed angular bounds.
- **Latitudinal** starts from longitude and latitude on the sphere, rotated into the image observer's frame.
- **RadialWarp and RectWarp** build a warped HPC plane point and derive the angles from it.

CAR and CEA sources use the point's solar longitude and latitude directly, CEA with its equal-area
latitude. In HPC and both warp modes, the point comes from the ray's intersection with the solar sphere,
inverse-rotated by the displayed view rotation; off-limb rays have no map data and are discarded.
The display mode and the source projection are independent choices.

From the plane point every mode computes original normalized coordinates:

```text
source = original pixel coordinate / original image size
```

`GLSLImageShader` builds this affine mapping from the original `planeToPixel` and `referencePixel`
divided by the original size. [`imageCommon.frag`](../resources/glsl/imageCommon.frag) stores its two rows
in `sourceX` and `sourceY`. Clipping and wrapping act on `source`. Right before the texture reads,
`getColor` converts it to decoded coordinates with one `sampling` vector per image:

```text
scale   = originalSize / (step · decodedSize)
offset  = (0.5 - F / step) / decodedSize
texture = source · scale + offset
```

The binder derives scale and offset from the grid; the shader keeps them in `sampling.xy` and
`sampling.zw`. At level 0 they are the identity. Each 160-byte image record also contains the native
frame, pole conventions, selected observer distance and observer transforms. `ImageLayer.render()` selects displayed
distance/orientation for surface maps, including trackball rotation only in Orthographic. Observer images retain
their source distance/orientation and existing camera-difference transform. The two-image uniform block is 320 bytes.

[`imageGeometry.frag`](../resources/glsl/imageGeometry.frag) projects homogeneous 3D directions through the
prepared native frame. Observer points and warp-plane inputs go directly to that frame without converting to angles and back. TAN uses the
formal WCS except in Orthographic, which keeps the simple-TAN display policy described above. The
binder receives the display mode and folds the appropriate offset and user plane rotation into
`sourceX/sourceY` before converting to float. For Orthographic TAN the offset is scaled CRVAL;
otherwise it is the native projection origin. No additional image uniforms are needed. HPC and Latitudinal
angular inputs and RectWarp's angular direction use `sinCos()`. It uses built-in sine/cosine on hardware
renderers and the accurate polynomial only on SwiftShader. `GLSLShader` selects this at fragment-shader
compilation from `GL.renderer`, adding `JHV_SWIFTSHADER` after the version directive; each new context
uses its own renderer name. There is no runtime shader branch or additional uniform.

Image shaders concatenate `imageCommon.frag` (declarations and color processing), `imageGeometry.frag`
(shared geometry), and the mode fragment in that order.

The current and comparison images have independent mappings. Difference rendering sends the same scene
point through each image's observer and WCS geometry, then subtracts the sampled values.

## Masks, slits, wrapping and sharpening

Detector masks, slits, edge clipping and dithering use `source`. They do not depend on the decoded size.

Radius masks, sectors and the LASCO linear cutoff act earlier, on the mode's scene or HPC plane point in
`clipPlanarMasks`. They never see decoded coordinates.

CAR and CEA longitude equivalence is resolved in the native projection plane. The shader intersects the
integer-turn intervals admitted by both original affine coordinates and the source slit. It prefers the
principal turn, then the nearest admitted turn. This handles partial maps, shear, rotated axes and
footprints several turns away without wrapping an image-width interval. The decoded sampling conversion
is applied only after that source position is selected.

Sharpening blurs nine taps around the decoded coordinate, with offsets in current-image texels. In
difference mode the comparison offsets are scaled by `comparison.sampling.xy / current.sampling.xy`, which
keeps the same displacement in original coordinates when the two textures have different sizes. Brightness,
enhancement, difference contrast, sharpening, gamma, dithering and the LUT then give the fragment color.

## Cutouts

`SDOCutout` and `SampClient` use `DecodedImage.geometry().original.bounds`. With reference pixel `c` and
pixel spacings `px, py`, that rectangle, in image-axis units with Y up, is:

```text
left   = -c.x · px              width  = W · px
bottom = -(H - c.y) · py        height = H · py
```

The callers convert to arcseconds and send the center and size. Nothing in it comes from the reduced
texture, so a resolution change cannot alter the requested cutout.

Native build instructions are in [JPEG 2000](../extra/jpeg2000/README.md) and
[WCSLIB](../extra/wcs/README.md). Test commands and coverage are in
[the test guide](../extra/test/README.md#graphics-and-wcs).
