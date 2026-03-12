# Non-Ortho Projection Note

This note documents the current convention used by the non-orthographic display modes (`HPC`, `Latitudinal`, `Polar`, `LogPolar`).

## Scope

- Java overlay and mouse-projection code lives in `src/org/helioviewer/jhv/display/ProjectionMode.java` and `src/org/helioviewer/jhv/display/NonOrthoProjection.java`.
- Image reprojection lives in:
  - `resources/glsl/solarHpc.frag`
  - `resources/glsl/solarLati.frag`
  - `resources/glsl/solarPolar.frag`
  - `resources/glsl/solarLogPolar.frag`

## Current convention

- `Orthographic` is a separate path. It renders directly in 3D and should not be used as a reference for non-ortho sign conventions.
- Non-ortho modes use an explicit map-basis rotation on the Java side.
- For `GridType.Viewpoint`, non-ortho maps use a positive latitude rotation in `NonOrthoProjection.mapRotation(...)`, so they intentionally do not reuse `GridType.toGrid(...)`.
- `HPC` is observer-centered, not origin-centered spherical coordinates.
- The current `HPC` display convention is:
  - `x = Tx` (helioprojective longitude / west-east angular offset)
  - `y = Ty` (helioprojective latitude / south-north angular offset)
- In Java, `HPC` projection is expressed with the observer-distance-dependent formulas:
  - `Tx = atan2(x, D - z)`
  - `Ty = atan2(y, sqrt(x^2 + (D - z)^2))`
- In GLSL, `solarHpc.frag` starts from the displayed `Tx,Ty` map coordinates, optionally intersects the observer ray with the unit solar sphere for on-disk differential rotation, and then reprojects through the source-image WCS.
- `HPC` display coordinates are angular coordinates in degrees. They are not orthographic scene coordinates, and they are not guaranteed to match `Orthographic` at the same on-screen radius.
- Java picking now distinguishes between:
  - solar-point unprojection
  - display-surface unprojection
- In `Orthographic`, display-surface unprojection uses the existing current-view sphere/plane logic.
- In `HPC`, display-surface unprojection returns flat current-view plane coordinates for display annotations.
- Other non-ortho modes currently leave display-surface unprojection undefined.
- Latitudinal projection is expressed as:
  - `x = longitude`
  - `y = latitude`
- The GLSL latitudinal shader uses explicit latitude internally as well: `0` at the equator, positive toward solar north.
- Polar and log-polar projection use a polar angle with `0°` at north and increasing anti-clockwise.
- The Java polar projection expresses that convention directly.
- The GLSL polar/log-polar shaders intentionally keep the legacy internal `theta = -(...) - HALFPI` style basis expression, because that is the form that matches the image reprojection path after `apply_center(..., vec3(pos.x, -pos.y, 0.), ...)`.
- Remaining sign flips in the GLSL code, such as final texture-space Y inversion, belong to image/WCS sampling space and should not be confused with the map convention itself.

## Important consequence

The Java overlay path and the GLSL image path must be kept in sync as two implementations of the same convention.

Changing only one side will usually produce one or more of:

- incorrect helioprojective aspect or scale in `HPC`
- north/south mirroring
- reversed polar-angle direction
- overlay/image misalignment
- incorrect mouse picking in non-ortho modes

## Current validation boundary

- `HPC` render sampling is validated against Astropy for the current `TAN` and non-slanted `AZP` test files.
- Direct screen comparison also shows that `HPC` and `Orthographic` are not identical display geometries, even with the same observer viewpoint and `dragRotation = 0`.
- `HPC` mouse unprojection is still incomplete off-disk, because the current Java path intersects only the unit solar sphere.
- `HPC` display-surface picking is defined separately from solar-point picking and is intended for flat display annotations such as line/FOV.
- `HPC` line/FOV annotation persistence is not fully correct yet:
  - those tools use flat display-plane points while dragging/drawing
  - but `AbstractAnnotateable` still serializes points as spherical `lon/lat`
  - so saving and reloading those `HPC` annotations is not yet a stable operation
- `HPC` extent inversion is currently exact for `TAN` and non-slanted `AZP`; slanted `AZP` and `ZPN` are not documented as supported yet.

## Practical rule

When changing non-ortho projection behavior, always review Java and GLSL together.
