# Map Projection Architecture

This note documents the coordinate conventions and code ownership of JHV's five display modes:

- `Orthographic`
- `HPC`
- `Latitudinal`
- `RadialWarp`
- `RectWarp`

Native build instructions are in [the WCSLIB guide](../extra/wcs/README.md).

## Code ownership

Java display geometry is organized as follows:

- `MapMode` selects the display mode and image shader.
- `MapView` captures the current camera, viewpoint, grid frame, angular origins, and per-viewport scales. It dispatches
  operations to either the orthographic or projected implementation.
- `OrthographicView` handles 3D orthographic overlay emission and picking.
- `ProjectedView` handles projection, unprojection, overlay emission, and picking for `HPC`, `Latitudinal`,
  `RadialWarp`, and `RectWarp`.
- `MapScale` converts between map coordinates and normalized display coordinates. Linear scales are used for `HPC`
  and `Latitudinal`; the two warp modes share a limb-anchored Box-Cox radial scale.
- `GLRenderer` constructs the per-viewport scales and chooses between 3D scene rendering and flat projected rendering.
- `ImageBounds` derives image-side HPC and radial bounds through the parsed WCS.
- `WcsInterpreter` adapts Helioviewer metadata defaults and legacy units to FITS cards.
- `WcsSolution` owns a native WCSLIB solution and exposes the same prepared native frame to Java and GLSL.
  WCSLIB owns the linear FITS transform, native poles, fiducial offsets, domains and CPU projection conversions.
- `ObserverGeometry` implements JHV's directed helioprojective ray, Sun-plane and solar-surface conversions.

Image reprojection is split across:

- `resources/glsl/imageCommon.frag`
- `resources/glsl/imageGeometry.frag`
- `resources/glsl/imageOrtho.frag`
- `resources/glsl/imageHpc.frag`
- `resources/glsl/imageLati.frag`
- `resources/glsl/imageRadialWarp.frag`
- `resources/glsl/imageRectWarp.frag`

`imageCommon.frag` contains shared declarations and texture/color processing. `imageGeometry.frag` contains
the shared observer geometry, WCS projections, image-coordinate conversion, masks, and the common HPC-derived
sampling path used by `HPC`, `RadialWarp`, and `RectWarp`. Java and both test harnesses concatenate these
two files in that order, followed by the mode fragment, without adding separators.

## Coordinate spaces

The implementation crosses several coordinate spaces. Keeping them distinct is essential.

1. Solar world space
   - Unit-sphere coordinates used by solar features and annotations.
   - Longitudes and latitudes describe positions on the Sun, independently of a particular image observer.

2. Viewpoint space
   - Solar world space rotated into the displayed observer's frame.
   - The observer lies on the positive Z axis at distance `D`.

3. Helioprojective angular space
   - Observer-centered angles `(Tx, Ty)`.
   - For a viewpoint-space point `(x, y, z)`:

     ```text
     Tx = atan2(x, D - z)
     Ty = atan2(y, sqrt(x² + (D - z)²))
     ```

4. HPC plane space
   - The observer ray projected onto the plane through Sun center, expressed in solar-radius units.
   - `RadialWarp` and `RectWarp` operate on this plane rather than directly on angular `(Tx, Ty)`.

5. WCS plane and image space
   - Projection coordinates defined by `TAN`, `ARC`, `AZP`, `ZPN`, `CAR`, or `CEA`.
   - `ImageGeometry.pixelToPlane` includes the FITS matrix, internal plane units and buffer Y convention;
     `planeToPixel` is its inverse. The original footprint remains independent of decode level.

6. Normalized map and screen space
   - `MapScale` maps display coordinates to the normalized domain used by flat shaders and Java overlays.
   - The viewport aspect correction is part of screen placement, not part of the physical map convention.

## Java to GLSL interface

The projection-related uniform blocks separate per-image mapping, screen, and display state.

`ImageBlock` contains two per-image records, one for the primary image and one for the difference image. Each record
contains the fixed-layout state used to map rendered solar coordinates into that image:

- two affine rows from the raw native projection plane to original normalized image pixels
- original-to-decoded sampling scale and offset
- projection code and plane units per radian
- three native-frame rows, with AZP constants and the WCSLIB ZPN branch limit in their unused W components
- WCSLIB's exact native-pole direction conventions
- source-observer distance
- relative display/source-observer rotation (`cameraDiff`)
- differential-rotation interval (`deltaT`)
- source-view quaternion

Each std140 image record occupies 160 bytes, so the two-record block occupies 320 bytes.
The six `PV2` coefficients remain ordinary uniforms for direct indexed access rather than becoming padded std140
scalar arrays.

The binder folds native plane-origin subtraction and user plane rotation into the affine rows, in double precision
before upload. User CRVAL adjustments instead create a cached WCSLIB solution with resolved pole defaults.
The shader projects homogeneous directions through that solution's basis; it does not reconstruct celestial
rotations from CRVAL per fragment. Observer points and warp rays avoid angle roundtrips. HPC and Latitudinal
inputs and RectWarp's angular direction use `sinCos()`. `GLSLShader` defines `JHV_SWIFTSHADER` when
compiling fragment shaders for a renderer whose name contains `SwiftShader`; the helper then uses split
quadrant reduction and polynomials. Other renderers use built-in sine/cosine. The choice is compiled into
the program, with no additional uniform or per-fragment backend test. Dense exact-float probes cover
both rendering backends and independently retain the polynomial and intrinsic controls.

`ScreenBlock` contains display-map state shared by both image slots:

- inverse model-view-projection matrix
- viewport aspect correction
- map X and Y bounds
- Box-Cox lambda
- Latitudinal longitude and latitude origins

`ScreenBlock` occupies 96 bytes.

`DisplayBlock` contains the shared rendering controls, including two independent angular openings (`userSector` and
`metadataSector`), radial limits, the planar cut-off, and normalized X slit limits. These are display masks, not WCS
parameters.

`DisplayBlock` has 25 meaningful floats and three trailing padding floats. Its std140 backing buffer therefore occupies
112 bytes; Java and the WebGL runner must allocate all 28 floats.

The meaning of `sourceViewQuat` depends on the sampling path. For observer images in `Latitudinal`, it rotates a
solar-world point into that image's observer frame. For surface maps, `ImageLayer` uploads the displayed viewpoint
rotation, including trackball rotation only in `Orthographic`, and the displayed observer distance;
`Orthographic`, `HPC`, and the warp modes inverse-rotate their sphere intersection through it to keep the map attached
to the visible sphere. Direct Latitudinal surface-map sampling does not need it. Observer images in `HPC` and the
warp modes use `sourceViewQuat` for differential rotation through the solar frame, but not for reprojection
between displayed and source viewpoints; that limitation is described below.

## Display modes

### Orthographic

`Orthographic` is the only 3D display path. It intersects the view with the unit sphere on-disk and uses an image-view
plane for off-limb observer images.

TAN retains the intentional simple-TAN display policy: the source point's XY coordinates minus celestial
CRVAL scaled by `unitsPerRad` form the plane input. The existing affine mapping then applies user rotation
and the inverse pixel transform. `GLSLImageShader.putImage()` folds this subtraction into the affine
offset for Orthographic TAN. WCSLIB still supplies its metadata, angular scale and pixel matrix.

ARC, AZP and ZPN use the formal WCS from the scene point's observer direction. TAN also uses formal WCS
in every other display mode. Orthographic and HPC select different display geometry; the simple-TAN
policy preserves their close visual agreement on the validated solar images, as explained in the
[WCS validation note](wcs-validation/jhv_wcs_hpc_validation_note.md#summary).

`CAR` and `CEA` surface maps are sampled directly from longitude and latitude on the visible sphere. They wrap in
longitude and have no off-limb representation.

For CEA, an explicit `CUNIT2` selects the angular FITS WCS convention used by wcslib/Astropy. Historical normalized
maps with no `CUNIT2` retain JHV's compatibility convention in which the second plane coordinate is stored directly as
`sin(latitude) / PV2_1`. `WcsInterpreter` converts that normalized row to angular FITS values for WCSLIB.

WCSLIB resolves CRVAL, LONPOLE, LATPOLE and native fiducial parameters together. CAR/CEA longitude equivalence
is selected in that native plane against both original affine coordinates and the source slit, including rotated,
sheared and partial footprints. Longitude equivalence is not an image-width wrap.

### HPC

`HPC` is a flat observer-centered angular map:

- map X is `Tx` in degrees;
- map Y is `Ty` in degrees;
- enabled image-layer footprints determine the displayed bounds;
- those bounds are centered on `(0, 0)` and padded to the viewport aspect ratio.

The GLSL path converts every displayed `(Tx, Ty)` to an observer ray. For observer images, on-disk rays intersect the
unit sphere so that differential rotation can be applied to a physical solar point. Off-disk rays proceed directly to
the source-image WCS.

For CAR/CEA maps, the sphere intersection is inverse-rotated by the displayed view rotation and sampled through the
same longitude/latitude mapping used by Orthographic. Longitude wraps; off-disk rays are discarded. The intersection
uses the displayed observer distance, matching Java mouse unprojection. Surface angular bounds and one-to-one zoom
use that distance as well; observer-image bounds retain source distance.

Java overlays are rotated into the displayed viewpoint frame and projected to `(Tx, Ty)`.
Surface points are visible when `D*z >= 1`; elevated loop points also remain visible when their
observer ray clears the sphere. Hidden vertices terminate line strips.

Mouse unprojection returns the unit-sphere intersection. It therefore returns `null` off-disk; the current HPC mouse
path does not invent a solar surface point for an off-limb ray.

### Latitudinal

`Latitudinal` is a flat solar longitude/latitude map:

- map X is longitude in degrees;
- map Y is latitude in degrees;
- longitude wraps horizontally;
- latitude is restricted to `[-90°, 90°]`.

The displayed map frame is defined by explicit longitude and latitude origins. Display projection and unprojection use
the inverse pair:

```text
display longitude = solar longitude + longitude origin
display latitude  = solar latitude  - latitude origin
```

For observer images, GLSL reconstructs the solar-world point from the displayed map and rotates it with that image
slot's `sourceViewQuat` before applying observer-image geometry and WCS. Differential rotation is also applied per
image slot. Source visibility requires `sourceWorld.z >= 1 / observerDistance`. The source quaternion maps into
an image observer's frame; it does not define the displayed map origin.

For `CRLN-CAR / CRLT-CAR` and `CRLN-CEA / CRLT-CEA` surface maps, GLSL samples longitude and latitude directly and
selects a native-longitude equivalent admitted by the original image footprint. These are surface coordinates,
not observer-image helioprojective coordinates.

### RadialWarp

`RadialWarp` is a circular remapping of the HPC plane.

- Polar angle is zero at north and increases anti-clockwise.
- Radius is measured in solar radii on the HPC plane.
- The disk is linear.
- Only radii beyond the limb use the Box-Cox mapping.
- The mapping is anchored so radius `1` occupies the same normalized position for every lambda.

Java overlays first project into the displayed viewpoint's HPC plane and then apply the radial scale. The shader performs
the inverse screen warp to recover an HPC-plane point and samples through the common HPC path, including its
surface-map branch.

### RectWarp

`RectWarp` unwraps the same HPC polar coordinates into a rectangle:

- map X is polar angle from `0°` to `360°`, with zero at north and increasing anti-clockwise;
- map Y is the same limb-anchored Box-Cox radial coordinate as `RadialWarp`.

Java's `PolarBasis` and both warp shaders use the same convention. Horizontal wrapping is required for lines crossing
the `0°/360°` seam.

## Planar masks

`Orthographic`, `HPC`, `RadialWarp`, and `RectWarp` use the shared `clipPlanarMasks` predicate:

- `userSector` and `metadataSector` are independent `(center, half-width)` angular openings;
- a point is discarded when it lies inside either opening;
- angular distance wraps across `-π/π`;
- inner and outer radius limits are applied in solar-radius plane coordinates;
- the optional cut-off limits distance along two perpendicular planar axes.

The point supplied to the common predicate is the image-aligned plane point in `Orthographic` and the HPC-plane point
in the three HPC-derived modes. The two sector tuples must remain independent: metadata cropping and the user's mask
are separate constraints. `Latitudinal` does not apply these planar masks; it discards the layer when the effective
inner radius exceeds `1`.

## Supported source WCS

The shared formal observer-image WCS path supports:

- formal `TAN`
- `ARC`
- `AZP`
- six-term `ZPN` on its primary monotonic branch

An absent CTYPE defaults to HPLN-TAN/HPLT-TAN. The legacy SOHO SOLAR-X/SOLAR-Y pair is normalized to
HPLN-TAN/HPLT-TAN before WCSLIB. Unsupported explicit projection codes are rejected.
The native adapter retains a primary two-axis affine celestial WCS. Nonzero ZPN terms above PV2_5 are rejected,
and CEA lambda must be in `(0,1]` after float encoding. The same effective coefficients feed CPU and GLSL.
WCSLIB's ZPN derivative scan can miss a constructed narrow fold; this integration retains that known library
limitation rather than adding another independent branch solver.

Surface-map sampling supports:

- `CRLN-CAR / CRLT-CAR`
- `CRLN-CEA / CRLT-CEA`

All five modes support surface maps. `Latitudinal` samples longitude and latitude directly. The other modes intersect
the visible sphere, undo the displayed view rotation, and sample the map at the resulting solar longitude and latitude.

## Viewpoint limitations

There are two different observer roles:

- the displayed viewpoint used by Java map geometry and overlays;
- each source image's observer used to interpret that image's WCS.

For observer images, `Latitudinal` explicitly bridges those frames with each image slot's `sourceViewQuat`. The `HPC`,
`RadialWarp`, and `RectWarp` image shaders instead interpret their displayed HPC coordinates directly in the source
observer's frame.
Consequently, those image paths are coherent when the displayed viewpoint matches the source observer in orientation
and distance, but arbitrary user-selected viewpoints are not generally supported. Java overlays may then follow the
selected viewpoint while the image remains tied to its source observer.

Changing this requires a defined reprojection policy and a corresponding Java-to-GLSL transform. A local scale factor or
observer-distance normalization is insufficient.

Surface-map sphere intersections use the displayed distance and orientation. Orthographic includes trackball rotation;
HPC and warp sampling use the displayed viewpoint without it. Latitudinal uses its explicit angular origins.
Surface angular/radial bounds use displayed distance, including the apparent limb's perspective plane radius.
Orthographic fit/reset uses the unit-sphere diameter for surface maps, including partial maps,
and retains observer-image heights in mixed-layer maxima.

HPC and warp annotations use finite-distance sphere occlusion. Unit-sphere points are visible
when `D*z >= 1` in observer coordinates; elevated loop vertices also remain visible when their
observer ray clears the sphere. Hidden vertices terminate line strips. RectWarp and Latitudinal
retain seam wrapping, and Latitudinal displays the whole surface. The annotation radius parameter
is applied only in Orthographic drawing.

Test commands and coverage are in [the test guide](../extra/test/README.md#graphics-and-wcs).
