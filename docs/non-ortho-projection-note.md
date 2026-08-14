# Map Projection Architecture

This note documents the coordinate conventions and code ownership of JHV's five display modes:

- `Orthographic`
- `HPC`
- `Latitudinal`
- `RadialWarp`
- `RectWarp`

It is an implementation guide. Detailed FITS WCS derivations, numerical results, and the distinction between
`simple-TAN` and `formal-TAN` are documented in `docs/wcs-validation/jhv_wcs_hpc_validation_note.md`.

## Code ownership

Java display geometry is organized as follows:

- `MapMode` selects the display mode and image shader.
- `MapView` captures the current camera, viewpoint, grid frame, angular origins, and per-viewport scales. It dispatches
  operations to either the orthographic or projected implementation.
- `OrthographicMap` handles 3D orthographic overlay emission and picking.
- `ProjectedMap` handles projection, unprojection, overlay emission, and picking for `HPC`, `Latitudinal`,
  `RadialWarp`, and `RectWarp`.
- `MapScale` converts between map coordinates and normalized display coordinates. Linear scales are used for `HPC`
  and `Latitudinal`; the two warp modes share a limb-anchored Box-Cox radial scale.
- `GLRenderer` constructs the per-viewport scales and chooses between 3D scene rendering and flat projected rendering.
- `ImageBounds` derives image-side HPC and radial bounds through the parsed WCS.
- `WcsHeader` stores the projection-specific WCS quantities consumed by Java and GLSL.
- `WcsProjection` implements the Java forward and inverse `TAN`, `ARC`, `AZP`, and `ZPN` mappings used by
  `ImageBounds`, plus the helioprojective ray and solar-surface conversions used by projected maps.

Image reprojection is split across:

- `resources/glsl/solarCommon.frag`
- `resources/glsl/solarOrtho.frag`
- `resources/glsl/solarHpc.frag`
- `resources/glsl/solarLati.frag`
- `resources/glsl/solarRadialWarp.frag`
- `resources/glsl/solarRectWarp.frag`

`solarCommon.frag` owns the shared observer geometry, WCS projections, image-coordinate conversion, masks, texture
sampling, and the common HPC-derived sampling path used by `HPC`, `RadialWarp`, and `RectWarp`.

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
   - `imageToPlane` is the effective FITS linear transform: either `CD`, or `CDELT` combined with `PC`/`CROTA`.
     `planeToImage` is its inverse.
   - Texture coordinates additionally apply the FITS-to-texture Y inversion.

6. Normalized map and screen space
   - `MapScale` maps display coordinates to the normalized domain used by flat shaders and Java overlays.
   - The viewport aspect correction is part of screen placement, not part of the physical map convention.

## Java to GLSL interface

The projection-related uniform blocks separate per-image mapping, screen, and display state.

`ImageBlock` contains two per-image records, one for the primary image and one for the difference image. Each record
contains the fixed-layout state used to map rendered solar coordinates into that image:

- image rectangle
- row-major `planeToImage` matrix
- `crval` in the internal WCS plane units
- projection code and plane units per radian
- upper limit of the primary monotonic `ZPN` branch
- source-observer distance
- relative display/source-observer rotation (`cameraDiff`)
- differential-rotation interval (`deltaT`)
- source-view quaternion

Each std140 image record occupies 96 bytes, including one padding float, so the two-record block occupies 192 bytes.
The six `PV2` coefficients remain ordinary uniforms for direct indexed access rather than becoming padded std140
scalar arrays.

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

The Electron validation runner checks the reflected block sizes and active-member offsets against its WebGL buffer
mirror.

The meaning of `sourceViewQuat` depends on the sampling path. For observer images in `Latitudinal`, it rotates a
solar-world point into that image's observer frame. For surface maps, `ImageLayer` uploads the displayed view rotation;
`Orthographic` uses it to keep the map attached to the visible sphere, while direct Latitudinal surface-map sampling
does not need it. The `HPC` and warp image paths do not currently rotate their display coordinates through
`sourceViewQuat`; that is part of their viewpoint limitation, described below.

## Display modes

### Orthographic

`Orthographic` is the only 3D display path. It intersects the view with the unit sphere on-disk and uses an image-view
plane for off-limb observer images.

For source images marked `TAN`, the current shader intentionally uses `simple-TAN`: the orthographic point's planar
`(x, y)` coordinates are passed directly to the linear image transform. Other zenithal projections use the formal
world-to-helioprojective-to-WCS path.

This distinction explains the apparent geometry when switching between `Orthographic` and `HPC`:

- formal-TAN in Orthographic and HPC differ visibly because they are different display geometries;
- for the tested TAN files, the current simple-TAN Orthographic path is extremely close to HPC and produces no visible
  bulge.

`CAR` and `CEA` surface maps are sampled directly from longitude and latitude on the visible sphere. They wrap in
longitude and have no off-limb representation.

### HPC

`HPC` is a flat observer-centered angular map:

- map X is `Tx` in degrees;
- map Y is `Ty` in degrees;
- enabled image-layer footprints determine the displayed bounds;
- those bounds are centered on `(0, 0)` and padded to the viewport aspect ratio.

The GLSL path converts every displayed `(Tx, Ty)` to an observer ray. On-disk rays intersect the unit sphere so that
differential rotation can be applied to a physical solar point. Off-disk rays proceed directly to the source-image WCS.

Java overlays are first rotated from solar world space into the displayed viewpoint frame, projected to `(Tx, Ty)`, and
clipped using the current `view.z >= 0` front-hemisphere test. Lines terminate at hidden segments instead of wrapping
through the back of the Sun.

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
image slot. The source quaternion maps into an image observer's frame; it does not define the displayed map origin.

For `CRLN-CAR / CRLT-CAR` and `CRLN-CEA / CRLT-CEA` surface maps, GLSL samples longitude and latitude directly and
wraps the source texture in X. These are surface coordinates, not observer-image helioprojective coordinates.

### RadialWarp

`RadialWarp` is a circular remapping of the HPC plane.

- Polar angle is zero at north and increases anti-clockwise.
- Radius is measured in solar radii on the HPC plane.
- The disk is linear.
- Only radii beyond the limb use the Box-Cox mapping.
- The mapping is anchored so radius `1` occupies the same normalized position for every lambda.

Java overlays first project into the displayed viewpoint's HPC plane and then apply the radial scale. The shader performs
the inverse screen warp to recover an HPC-plane point and samples through the common HPC observer-image path.

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

`WcsHeader.Projection.fromCtype` defaults unrecognized projection codes to `TAN`.

Surface-map sampling supports:

- `CRLN-CAR / CRLT-CAR`
- `CRLN-CEA / CRLT-CEA`

Surface maps have a defined display policy only in `Latitudinal` and `Orthographic`. `HPC`, `RadialWarp`, and
`RectWarp` are observer-centered modes and do not provide a meaningful direct display of solar longitude/latitude map
data.

## Viewpoint limitations

There are two different observer roles:

- the displayed viewpoint used by Java map geometry and overlays;
- each source image's observer used to interpret that image's WCS.

`Latitudinal` explicitly bridges those frames with each image slot's `sourceViewQuat`. The `HPC`, `RadialWarp`, and
`RectWarp` image shaders instead interpret their displayed HPC coordinates directly in the source observer's frame.
Consequently, those image paths are coherent when the displayed viewpoint matches the source observer in orientation
and distance, but arbitrary user-selected viewpoints are not generally supported. Java overlays may then follow the
selected viewpoint while the image remains tied to its source observer.

Changing this requires a defined reprojection policy and a corresponding Java-to-GLSL transform. A local scale factor or
observer-distance normalization is insufficient.

## Validation boundaries

The validation suite in `extra/test` separates several questions:

- Astropy validates the FITS WCS calculations and the modeled HPC source-sampling path for `TAN`, `ARC`, `AZP`, and
  primary-branch six-term `ZPN`.
- Astropy validates `CAR` and `CEA` source WCS and Latitudinal surface-map sampling.
- Java/Python metadata tests validate the projection parameters and linear WCS transforms that Java derives for GLSL.
- Electron/WebGL2 runs execute the production shaders on Metal/ANGLE and SwiftShader.
- Orthographic and warp modes are compared with their independent CPU mirrors because Astropy does not define JHV's
  display warps.
- Dedicated internal comparisons distinguish formal-TAN Orthographic geometry, simple-TAN Orthographic geometry, and
  HPC geometry.

Astropy agreement does not validate:

- Java overlay projection or visible-hemisphere clipping;
- mouse picking and annotation persistence;
- arbitrary displayed viewpoints in HPC-derived image modes;
- UI zoom/refit policy;
- blending, rasterization, or other full-renderer state.

## Change checklist

When changing projection behavior:

1. Identify the coordinate space being changed; do not treat map, observer, WCS-plane, and texture coordinates as
   interchangeable.
2. Review Java projection, unprojection, overlays, mouse mapping, GLSL sampling, and the Java-to-GLSL interface together.
3. Preserve the inverse relationship between `MapScale.toMap*` and `MapScale.toUnit*`.
4. Check both image slots, differential rotation, and source-view transforms.
5. Validate WCS-defined behavior against Astropy and JHV-specific display geometry against the CPU and Electron paths.
6. Do not infer support for arbitrary viewpoints or surface-map display modes from source-WCS agreement alone.
