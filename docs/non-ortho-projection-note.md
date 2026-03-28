# Non-Ortho Projection Note

This note documents the convention used by the non-orthographic display modes (`HPC`, `Latitudinal`, `Polar`, `LogPolar`).

## Scope

- Java projection-related code is split across:
  - `src/org/helioviewer/jhv/display/ProjectionMode.java`
  - `src/org/helioviewer/jhv/display/NonOrthoProjection.java`
  - `src/org/helioviewer/jhv/display/OrthoProjection.java`
  - `src/org/helioviewer/jhv/display/DisplayMapBounds.java`
  - `src/org/helioviewer/jhv/wcs/WcsHeader.java`
  - `src/org/helioviewer/jhv/wcs/WcsProjection.java`
  - `src/org/helioviewer/jhv/wcs/ImageBounds.java`
- Image reprojection lives in:
  - `resources/glsl/solarHpc.frag`
  - `resources/glsl/solarLati.frag`
  - `resources/glsl/solarPolar.frag`
  - `resources/glsl/solarLogPolar.frag`

## Projection Model

- `Orthographic` is a separate path. It renders directly in 3D and should not
  be used as the reference for non-ortho sign conventions.
- Java ownership is:
  - `ProjectionMode`: mode selection, shader/scale selection, and dispatch
  - `NonOrthoProjection`: non-ortho projection math, point/line emission, mouse/grid helpers, and surface unprojection
  - `OrthoProjection`: orthographic point/line emission and picking helpers
  - `WcsHeader` / `WcsProjection`: shared image-WCS bundle and plane-to-helioprojective conversion
  - `ImageBounds`: intrinsic image-side `HPC` footprint bounds
  - `DisplayMapBounds`: display-map bounds used for non-ortho `1:1` sizing
- Non-ortho modes use an explicit map-basis rotation on the Java side. For
  `GridType.Viewpoint`, that rotation uses positive latitude in
  `GridType.mapRotation(...)`.
- Java picking distinguishes between:
  - solar-point unprojection
  - current-view sphere/plane picking for transformed display annotations such as line/FOV
- `Orthographic` and non-ortho modes share the current-view sphere/plane pick
  path for those transformed annotations.

### Mode notes

- `HPC` is observer-centered, not origin-centered spherical coordinates.
  - display coordinates are `Tx,Ty` in degrees
  - Java overlays project with:
    - `Tx = atan2(x, D - z)`
    - `Ty = atan2(y, sqrt(x^2 + (D - z)^2))`
  - Java overlays first rotate world-space points into the viewpoint frame
    before applying those formulas
  - the visible scale is derived from enabled image-layer bounds, then centered
    on `(Tx, Ty) = (0, 0)` and padded for aspect ratio
  - GLSL `solarHpc.frag` starts from displayed `Tx,Ty`, intersects the
    observer ray with the unit sphere for on-disk differential rotation, and
    otherwise samples the source-image WCS directly from the same
    helioprojective angles
  - Java overlay emission clips to the visible hemisphere and does not wrap
    horizontally
- `Latitudinal` uses `x = longitude`, `y = latitude`.
  - the GLSL latitudinal shader uses explicit latitude internally as well, with
    `0` at the equator and positive northward
  - `CRLN-CAR / CRLT-CAR` surface maps are handled here as direct solar
    longitude/latitude maps, not as observer-image reprojections
- `Polar` and `LogPolar` use polar angle with `0°` at north and increasing
  anti-clockwise.
  - the Java polar projection expresses that convention directly
  - the GLSL polar/log-polar shaders keep the legacy internal
    `theta = -(...) - HALFPI` basis because it matches the image reprojection
    path after `apply_center(..., vec3(pos.x, -pos.y, 0.), ...)`
- Remaining sign flips in GLSL, such as final texture-space Y inversion, belong
  to image/WCS sampling space and should not be confused with the map
  convention itself.

## Consequences and Limits

- Java overlays and GLSL image reprojection are two implementations of the same
  non-ortho convention. Changing only one side usually causes one or more of:
  - incorrect `HPC` aspect or scale
  - north/south mirroring
  - reversed polar-angle direction
  - overlay/image misalignment
  - incorrect mouse picking
- `HPC` render sampling is validated against Astropy for the supported
  source-observer `TAN`, `AZP`, and six-term primary-branch `ZPN` image/WCS
  paths.
- That validation does **not** cover Java-side overlay behavior such as
  viewpoint-space projection of Carrington surface coordinates or
  visible-hemisphere clipping.
- Direct screen comparison also shows that `HPC` and `Orthographic` are not the
  same display geometry, even with the same observer viewpoint and
  `dragRotation = 0`.
- `HPC` mouse unprojection remains incomplete off-disk: the Java path only
  tries to intersect the unit solar sphere, and off-disk rays return `null`.
- `HPC` line/FOV annotation persistence is not yet stable because dragging uses
  the shared current-view sphere/plane pick path, while persistence still stores
  points as spherical `lon/lat`.
- `ProjectionMode` exposes mode predicates (`isOrthographic()`, `isHpc()`,
  `isLatitudinal()`, `isPolar()`, `isLogPolar()`) so call sites can express
  mode distinctions through helpers rather than raw enum equality checks.
- `HPC` extent inversion is exact for:
  - `TAN`
  - `AZP`
  - six-term `ZPN` on its primary monotonic branch
- Unsupported projection codes fall back to `TAN` on both the Java and GLSL
  sides.
- `ZPN` support uses only `PV2_0..PV2_5`, which matches the solar test files.
- `CRLN-CAR / CRLT-CAR` surface maps are currently supported only in:
  - `Latitudinal`
  - `Orthographic`
- They are not supported in:
  - `HPC`, because `HPC` is observer-centered helioprojective angle space
  - `Polar` / `LogPolar`, because those paths still assume observer-image
    reprojection rather than a pure surface lon/lat map
- In `Orthographic`, `CAR` wraps only the visible solar sphere and has no
  off-limb representation.

## HPC Viewpoint Findings

- Commit `2d5ef3435` changed Java `HPC` overlay semantics by rotating
  world-space points into viewpoint space before projection and clipping
  overlays to the visible hemisphere.
- The GLSL image path in `resources/glsl/solarHpc.frag` did not receive the same
  semantic update at that time, so Java overlays and GLSL image reprojection
  drifted apart.
- A narrow on-disk GLSL sync patch is possible and can improve image/overlay
  agreement for rotated `TAN` cases.
- In the supported source-observer case, that patch is effectively a no-op:
  old and new rendering are visually indistinguishable because the display and
  source observer frames already coincide.
- This does **not** make user-selected non-observer viewpoints in `HPC`
  supported. `HPC` should be treated as sane only for the source observer
  orientation.
- Viewpoints selected through the Viewpoint layer may look better or worse
  after local fixes, but they remain outside the supported `HPC` contract.
- `observerAt1au` could not be made stable by local scalar renormalization of
  the existing `HPC` image path. The current shader and metadata bundle are too
  tightly coupled to the source observer geometry for that kind of patch.

## Practical rule

When changing non-ortho projection behavior, always review Java and GLSL
together, and treat Astropy validation as evidence for the supported image/WCS
path only, not for unsupported `HPC` viewpoint modes.
