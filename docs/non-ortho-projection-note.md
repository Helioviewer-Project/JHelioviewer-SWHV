# Non-Ortho Projection Note

This note documents the current convention used by the non-orthographic display modes (`Latitudinal`, `Polar`, `LogPolar`).

## Scope

- Java overlay and mouse-projection code lives in [`src/org/helioviewer/jhv/display/ProjectionMode.java`](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/display/ProjectionMode.java) and [`src/org/helioviewer/jhv/display/NonOrthoProjection.java`](/Users/bogdan/jhv/JHelioviewer-SWHV/src/org/helioviewer/jhv/display/NonOrthoProjection.java).
- Image reprojection lives in:
  - [`resources/glsl/solarLati.frag`](/Users/bogdan/jhv/JHelioviewer-SWHV/resources/glsl/solarLati.frag)
  - [`resources/glsl/solarPolar.frag`](/Users/bogdan/jhv/JHelioviewer-SWHV/resources/glsl/solarPolar.frag)
  - [`resources/glsl/solarLogPolar.frag`](/Users/bogdan/jhv/JHelioviewer-SWHV/resources/glsl/solarLogPolar.frag)

## Current convention

- `Orthographic` is a separate path. It renders directly in 3D and should not be used as a reference for non-ortho sign conventions.
- Non-ortho modes use an explicit map-basis rotation on the Java side.
- For `GridType.Viewpoint`, the effective non-ortho latitude rotation is positive. This is why the Java non-ortho path does not simply reuse `GridType.toGrid(...)`.
- Latitudinal projection is expressed as:
  - `x = longitude`
  - `y = latitude`
- The GLSL latitudinal shader still parameterizes the source sphere using colatitude internally (`0` at north pole, `π` at south pole), but the exposed map convention remains positive latitude up.
- Polar and log-polar projection use a polar angle with `0°` at north and increasing anti-clockwise.

## Important consequence

The Java overlay path and the GLSL image path must be kept in sync as two implementations of the same convention.

Changing only one side will usually produce one or more of:

- north/south mirroring
- reversed polar-angle direction
- overlay/image misalignment
- incorrect mouse picking in non-ortho modes

## Practical rule

When changing non-ortho projection behavior, always review Java and GLSL together.
