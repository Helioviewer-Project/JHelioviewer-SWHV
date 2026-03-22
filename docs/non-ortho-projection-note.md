# Non-Ortho Projection Note

This note documents the current convention used by the non-orthographic display modes (`HPC`, `Latitudinal`, `Polar`, `LogPolar`).

## Scope

- Java projection-related code is split across:
  - `src/org/helioviewer/jhv/display/ProjectionMode.java`
  - `src/org/helioviewer/jhv/display/NonOrthoProjection.java`
  - `src/org/helioviewer/jhv/display/OrthoProjection.java`
- Image reprojection lives in:
  - `resources/glsl/solarHpc.frag`
  - `resources/glsl/solarLati.frag`
  - `resources/glsl/solarPolar.frag`
  - `resources/glsl/solarLogPolar.frag`

## Current convention

- `Orthographic` is a separate path. It renders directly in 3D and should not be used as a reference for non-ortho sign conventions.
- Current Java ownership is:
  - `ProjectionMode`: mode selection, shader/scale selection, small mode queries, and dispatch
  - `NonOrthoProjection`: non-ortho projection math, non-ortho point/line emission, non-ortho mouse/grid helpers, non-ortho display/surface unprojection
  - `OrthoProjection`: orthographic point/line emission, orthographic mouse/grid helpers, orthographic display/surface unprojection
- Non-ortho modes use an explicit map-basis rotation on the Java side.
- For `GridType.Viewpoint`, non-ortho maps use a positive latitude rotation in `NonOrthoProjection.mapRotation(...)`, so they intentionally do not reuse `GridType.toGrid(...)`.
- `HPC` is observer-centered, not origin-centered spherical coordinates.
- The current `HPC` display convention is:
  - `x = Tx` (helioprojective longitude / west-east angular offset)
  - `y = Ty` (helioprojective latitude / south-north angular offset)
- The current `HPC` visible scale is derived from the actual image footprint in
  `Tx,Ty`, but the displayed bounds stay centered on `(Tx, Ty) = (0, 0)`.
- Aspect-ratio padding is added around `(0, 0)` so angular scale stays
  isotropic on screen.
- During playback, the `HPC` scale is sticky for the current enabled image-layer
  set instead of being recomputed from every frame, so the Sun-centered grid
  remains visually stable.
- In Java, `HPC` projection is expressed with the observer-distance-dependent formulas:
  - `Tx = atan2(x, D - z)`
  - `Ty = atan2(y, sqrt(x^2 + (D - z)^2))`
- For Java overlays, external solar/world points are first rotated into the
  current observer/viewpoint frame before those `HPC` formulas are applied.
- In GLSL, `solarHpc.frag` starts from the displayed `Tx,Ty` map coordinates, optionally intersects the observer ray with the unit solar sphere for on-disk differential rotation, and then reprojects through the source-image WCS.
- `HPC` display coordinates are angular coordinates in degrees. They are not orthographic scene coordinates, and they are not guaranteed to match `Orthographic` at the same on-screen radius.
- Because `HPC` is angular, its visible grid scale changes with observer distance:
  - a closer observer sees the same solar structure under a larger angle
  - a more distant observer sees it under a smaller angle
  - this is expected behavior for the current `HPC` mode, not a rendering bug
- Java picking now distinguishes between:
  - solar-point unprojection
  - current-view sphere/plane picking for transformed display annotations such as line/FOV
- `Orthographic` and non-ortho modes now share the same current-view sphere/plane pick path for those transformed annotations.
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

## Current validation boundary

- `HPC` render sampling is validated against Astropy for the current `TAN`,
  non-slanted `AZP`, and six-term primary-branch `ZPN` test files.
- Direct screen comparison also shows that `HPC` and `Orthographic` are not identical display geometries, even with the same observer viewpoint and `dragRotation = 0`.
- `HPC` mouse unprojection is still incomplete off-disk, because the current Java path intersects only the unit solar sphere.
- `HPC` display-surface picking is defined separately from solar-point picking and is intended for flat display annotations such as line/FOV.
- `HPC` line/FOV annotation persistence is not fully correct yet:
  - those tools use flat display-plane points while dragging/drawing
  - but `AbstractAnnotateable` still serializes points as spherical `lon/lat`
  - so saving and reloading those `HPC` annotations is not yet a stable operation
- Callers now increasingly use mode predicates on `ProjectionMode` (`isOrthographic()`, `isHpc()`, `isLatitudinal()`, `isPolar()`, `isLogPolar()`) instead of raw enum equality checks.
- `HPC` extent inversion is currently exact for:
  - `TAN`
  - non-slanted `AZP`
  - six-term `ZPN` on its primary monotonic branch
- slanted `AZP` is still not documented as supported
- current `ZPN` support uses only `PV2_0..PV2_5`, which matches the current
  solar test files

## Practical rule

When changing non-ortho projection behavior, always review Java and GLSL together.
