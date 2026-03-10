# Non-Ortho Projection Note

This note documents the current convention used by the non-orthographic display modes (`Latitudinal`, `Polar`, `LogPolar`).

## Scope

- Java overlay and mouse-projection code lives in `src/org/helioviewer/jhv/display/ProjectionMode.java` and `src/org/helioviewer/jhv/display/NonOrthoProjection.java`.
- Image reprojection lives in:
  - `resources/glsl/solarLati.frag`
  - `resources/glsl/solarPolar.frag`
  - `resources/glsl/solarLogPolar.frag`

## Current convention

- `Orthographic` is a separate path. It renders directly in 3D and should not be used as a reference for non-ortho sign conventions.
- Non-ortho modes use an explicit map-basis rotation on the Java side.
- For `GridType.Viewpoint`, non-ortho maps use a positive latitude rotation in `NonOrthoProjection.mapRotation(...)`, so they intentionally do not reuse `GridType.toGrid(...)`.
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

- north/south mirroring
- reversed polar-angle direction
- overlay/image misalignment
- incorrect mouse picking in non-ortho modes

## Practical rule

When changing non-ortho projection behavior, always review Java and GLSL together.
