# Non-Ortho Projection Note

This note documents the convention used by the non-orthographic display modes (`HPC`, `Latitudinal`, `Polar`, `LogPolar`).

## Scope

- Java projection-related code is split across:
  - `src/org/helioviewer/jhv/display/ProjectionMode.java`
  - `src/org/helioviewer/jhv/display/NonOrthoProjection.java`
  - `src/org/helioviewer/jhv/display/OrthoProjection.java`
  - `src/org/helioviewer/jhv/wcs/WcsProjection.java`
  - `src/org/helioviewer/jhv/wcs/WcsInverse.java`
  - `src/org/helioviewer/jhv/wcs/ImageBounds.java`
  - `src/org/helioviewer/jhv/wcs/DisplayMapBounds.java`
- Image reprojection lives in:
  - `resources/glsl/solarHpc.frag`
  - `resources/glsl/solarLati.frag`
  - `resources/glsl/solarPolar.frag`
  - `resources/glsl/solarLogPolar.frag`

## Convention

- `Orthographic` is a separate path. It renders directly in 3D and should not be used as a reference for non-ortho sign conventions.
- Java ownership is:
  - `ProjectionMode`: mode selection, shader/scale selection, small mode queries, and dispatch
  - `NonOrthoProjection`: non-ortho projection math, non-ortho point/line emission, non-ortho mouse/grid helpers, and non-ortho surface unprojection
  - `OrthoProjection`: orthographic point/line emission, orthographic mouse/grid helpers, and orthographic surface unprojection
  - `WcsProjection` / `WcsInverse`: shared image-WCS plane to helioprojective conversion used by bounds and `1:1` sizing helpers
  - `ImageBounds`: intrinsic image-side `HPC` footprint bounds
  - `DisplayMapBounds`: display-map bounds used for non-ortho `1:1` sizing
- Non-ortho modes use an explicit map-basis rotation on the Java side.
- For `GridType.Viewpoint`, non-ortho maps use a positive latitude rotation in `NonOrthoProjection.mapRotation(...)`, so they intentionally do not reuse `GridType.toGrid(...)`.
- `HPC` is observer-centered, not origin-centered spherical coordinates.
- The `HPC` display convention is:
  - `x = Tx` (helioprojective longitude / west-east angular offset)
  - `y = Ty` (helioprojective latitude / south-north angular offset)
- The `HPC` visible scale is derived from the actual image footprint in
  `Tx,Ty`, but the displayed bounds stay centered on `(Tx, Ty) = (0, 0)`.
- Aspect-ratio padding is added around `(0, 0)` so angular scale stays
  isotropic on screen.
- For a given rendered frame, the `HPC` scale is derived from the enabled
  image-layer bounds and then centered on `(Tx, Ty) = (0, 0)` before
  aspect-ratio padding is applied.
- In Java, `HPC` projection is expressed with the observer-distance-dependent formulas:
  - `Tx = atan2(x, D - z)`
  - `Ty = atan2(y, sqrt(x^2 + (D - z)^2))`
- For Java overlays, external solar/world points are first rotated into the
  observer/viewpoint frame before those `HPC` formulas are applied.
- In GLSL, `solarHpc.frag` starts from the displayed `Tx,Ty` map coordinates, optionally intersects the observer ray with the unit solar sphere for on-disk differential rotation, and then reprojects through the source-image WCS.
- `HPC` display coordinates are angular coordinates in degrees. They are not orthographic scene coordinates, and they are not guaranteed to match `Orthographic` at the same on-screen radius.
- Because `HPC` is angular, its visible grid scale changes with observer distance:
  - a closer observer sees the same solar structure under a larger angle
  - a more distant observer sees it under a smaller angle
  - this is expected behavior for `HPC`, not a rendering bug
- Java picking distinguishes between:
  - solar-point unprojection
  - current-view sphere/plane picking for transformed display annotations such as line/FOV
- `Orthographic` and non-ortho modes share the same current-view sphere/plane pick path for those transformed annotations.
- Latitudinal projection is expressed as:
  - `x = longitude`
  - `y = latitude`
- The GLSL latitudinal shader uses explicit latitude internally as well: `0` at the equator, positive toward solar north.
- Polar and log-polar projection use a polar angle with `0°` at north and increasing anti-clockwise.
- The Java polar projection expresses that convention directly.
- The GLSL polar/log-polar shaders intentionally keep the legacy internal `theta = -(...) - HALFPI` style basis expression, because that is the form that matches the image reprojection path after `apply_center(..., vec3(pos.x, -pos.y, 0.), ...)`.
- Remaining sign flips in the GLSL code, such as final texture-space Y inversion, belong to image/WCS sampling space and should not be confused with the map convention itself.
- Java projected polyline emission differs by mode:
  - `HPC` emits continuous projected segments and does not wrap horizontally
  - `Latitudinal`, `Polar`, and `LogPolar` use the shared wrapped projected-line path
- In Java overlay emission, `HPC` is also clipped to the visible hemisphere:
  - back-side surface points are skipped instead of being projected through the map
  - clipped polyline segments terminate and restart as separate strips

## Important consequence

The Java overlay path and the GLSL image path must be kept in sync as two implementations of the same convention.

Changing only one side will usually produce one or more of:

- incorrect helioprojective aspect or scale in `HPC`
- north/south mirroring
- reversed polar-angle direction
- overlay/image misalignment
- incorrect mouse picking in non-ortho modes

## Validation boundary

- `HPC` render sampling is validated against Astropy for the tested `TAN`,
  `AZP`, and six-term primary-branch `ZPN` test files.
- The validator also reports the raw image-footprint `HPC` bounds and the centered display bounds used by the `HPC` screen mapping.
- Direct screen comparison also shows that `HPC` and `Orthographic` are not identical display geometries, even with the same observer viewpoint and `dragRotation = 0`.
- `HPC` mouse unprojection remains incomplete off-disk, because the Java path intersects only the unit solar sphere.
- `HPC` line/FOV annotation persistence is not fully correct yet:
  - those tools use the shared current-view sphere/plane pick path while dragging/drawing
  - but `AbstractAnnotateable` serializes points as spherical `lon/lat`
  - so saving and reloading those `HPC` annotations is not yet a stable operation
- The Astropy validation script covers the image/WCS `HPC` path, not Java-side overlay behavior such as viewpoint-space external-point projection or visible-hemisphere clipping.
- `ProjectionMode` exposes mode predicates (`isOrthographic()`, `isHpc()`, `isLatitudinal()`, `isPolar()`, `isLogPolar()`) for call sites that express mode distinctions through helpers rather than raw enum equality checks.
- `HPC` extent inversion is exact for:
  - `TAN`
  - `AZP`
  - six-term `ZPN` on its primary monotonic branch
- `ZPN` support uses only `PV2_0..PV2_5`, which matches the
  solar test files

## Practical rule

When changing non-ortho projection behavior, always review Java and GLSL together.
