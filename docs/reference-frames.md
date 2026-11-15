# Reference frames and placement

This note describes how JHV places things in 3D. Code references name classes and
methods. Image pixels and WCS are covered in [image coordinates](image-coordinates.md).

## World frame

The scene uses the SPICE frame `SOLO_IAU_SUN_2009`, the Carrington frame. Its origin is
the Sun center and its unit is the solar radius. `Spice.position` divides SPICE kilometers
by `Sun.RadiusKMeter`, and `PositionResponse` does the same for server responses.

JHV's Cartesian axes differ from SPICE's. `SphericalCoords.vec3` gives
x = r cos(lat) sin(lon), y = r sin(lat), z = r cos(lat) cos(lon). The y axis is solar
north. The longitude of a world vector, `SphericalCoords.longitude` = atan2(x, z), is its
Carrington longitude.

The frames `SOLO_IAU_SUN_2009`, `SOLO_HCI`, `SOLO_HEEQ` and `SOLO_HEE` are defined in
`resources/kernels/solo_ANC_soc-sci-fk_V09.tf`. `AppInit` loads the built-in kernels and
then the user kernels from `Directories.KERNELS`, sorted by path.

## Position

`Position(time, distance, lon, lat)` describes a viewpoint. Its `lon` is the negated
Carrington longitude. `Spice.getCarrington`, `PositionResponse.interpolateCarrington` and
`FitsMetaData` all store it this way.

`Position.toQuat` is `Quat.createXY(lat, lon)`. It is built on the first call and kept. It
rotates world vectors into the view of that position. `ObserverGeometry.observerRayToWorld`
applies its inverse to go from the view back to the world.

Each view draws at its viewpoint time, `MapView.viewpoint().time`.

## View

`MapView.viewRotation` turns the world into the view. In the orthographic mode it is the
camera's drag rotation applied after `viewpoint.toQuat()`. In the projected modes it is
`viewpoint.toQuat()` alone. `GLRenderer` sets it as the base view with `Transform.ortho`.

The projected modes map view or world vectors to the plane:
- HPC rotates the world with `viewpoint.toQuat()` and takes helioprojective angles
  from the viewpoint, atan2(x, d - z) and atan2(y, sqrt(x^2 + (d - z)^2)), where d is
  the viewpoint distance. `ObserverGeometry.helioprojectiveToWorld` is the inverse.
- The polar and log-polar views rotate the same way, scale x and y by d / (d - z), and
  take the angle with `PolarBasis.angle`. The inverse goes through
  `ObserverGeometry.observerRayToWorld`.
- The latitudinal map takes the world longitude and latitude and shifts them by
  `GridType.toLongitude` and `GridType.toLatitude` of the viewpoint. See
  [non-orthographic projection](non-ortho-projection-note.md).

Mouse positions:
- `mouseToSurface` hits the sphere and returns a world point. The orthographic mode
  removes the drag rotation and `viewpoint.toQuat()`.
- In the projected modes, `mouseToSurface` inverts the map projection.
- `mouseToSky` hits the near side of the sphere, else the viewpoint's plane of the sky,
  and returns a point in view coordinates, with only the drag rotation removed.
- `ViewportMath.computeUpX` and `computeUpY` give the mouse on the screen plane, in the
  coordinates of `MapView.viewRotation`, with the camera translation and zoom removed.

## Sources of positions

Earth:
- `Sun.getEarth(time)` is Earth in Carrington, from `Spice.getCarrington`.
- `Sun.getEarthHCI(time)` is Earth in `SOLO_HCI`, from `Spice.getPositionLat`. Its
  longitude is not negated.
- Both are Caffeine caches of 10000 entries keyed by `JHVTime`. If SPICE fails, both
  return the distance from `Sun.getEarthDistance` at longitude and latitude 0.
- `Sun.getEarthDistance` is an analytic formula, not SPICE. `PropagationModel` uses it
  with `Sun.L1Factor` for the L1 distance.

Time series:
- `PositionLoad` asks for an observer, a target, a `Frame` and an aberration correction.
  It uses local SPICE (`Spice.getRange`) when both objects are internal, and the server
  `https://swhv.oma.be/position` or `/state` otherwise.
- The Location viewpoint loads in `SOLO_IAU_SUN_2009`. The panel offers no frame for it,
  but a saved state with a `frame` key overrides it.
- The Heliosphere view loads the Sun-to-object positions in the frame the user picks: HCI,
  HEEQ or HEE. HEEQ is the default.
- `ViewpointSky` loads three things in `J2000`. The Sun and planets as seen from the
  observer use `LT+S` (`submitApparent`). The observer state relative to the solar system
  barycenter (`submitBarycentricState`) and the observer relative to the Sun use no
  correction.

Images:
- `FitsMetaData` takes the observer longitude from `HGLN_OBS` (`earth.lon` minus it),
  else from `CRLN_OBS` (negated), else from Earth. Latitude comes from `HGLT_OBS`, else
  `CRLT_OBS`, else Earth. Earth is taken at the observation time.
- The distance is `DSUN_OBS`. Without it, SOHO is at Earth's distance times
  `Sun.L1Factor` and other observers at Earth's distance.
- SDO, and any observer with `DSUN_OBS` below 1 solar radius, is placed at Earth.
- The viewpoint time depends on `DisplaySettings.TimeMode`. Observer, the default, keeps
  `DATE-OBS`. Sun subtracts the light time from the observer distance. Earth subtracts
  the light time of the distance difference to Earth.
- `NullMetaData` uses Earth.
- `ImageLayer` passes `MapView.viewRotation`, the viewpoint and each image's metadata
  viewpoint to `GLSLImageShader`. The shader rotates the world into the image with the
  metadata viewpoint's `toQuat()`, or with the view rotation for surface maps.
  `ImageGeometry.tracingViewpoint` is the metadata viewpoint for observer images and the
  display viewpoint for surface maps.

3D products and files:
- Models (glTF and others loaded by `AssimpModelLoader`) with heliocentric-Cartesian
  metadata are in the observer's frame. `HeliocentricCartesianMetaData.observerRotation`
  gives `Quat.createXY(CRLT_OBS, -CRLN_OBS)`, and `AssimpModelLoader.coordinateTransform`
  applies its inverse to bring the vertices into the world. A model without that
  metadata is taken as already in the world. `GLSLModel` takes the light direction from
  `MapView.viewRotation`, so the light stays at the viewer.
- SunJSON coordinates are Carrington radius, longitude and latitude, converted with
  `SphericalCoords.vec3` in `SunJSONTypes`.
- Connectivity, HCS and footpoint files give Carrington longitude and latitude, converted
  in `ConnectUtils`.
- `PfssLoader` rotates the field-line points about the file's z axis, the solar pole, by
  Earth's Carrington longitude at the map's `DATE-OBS`. `PfssLine` then maps the file's
  x, y, z to JHV's x, z, -y.

## Events

The HEK handler places an event once, while parsing it, with Earth from
`Sun.getEarth` at the event start. The positions are never rotated afterward.

HEK, in `HEKHandler.parseEventJSON` and `HEKGeometry.position`:
- The central point is `hgs_coord`, else `hgs_x` and `hgs_y`.
- The outline is `hgs_boundcc`, else `hgs_bbox`. A bounding box wider than 160 degrees in
  both longitude and latitude is dropped.
- All are Stonyhurst and converted with `SphericalCoords.unit(lon - earth.lon, lat)`.
- For CACTus (`supplier.isArc()`), `readCMEParameters` takes the position angle from
  `event_coord1`, the width from `cme_angularwidth` and the speed from
  `cme_radiallinvel`, and keeps `earth.toQuat()` as `CMEParameters.earthView`.

Drawing, in `SWEKLayer`:
- Central points, icons and outlines are drawn in world coordinates. Outline edges are
  interpolated in Cartesian coordinates and projected back onto the sphere.
- A CACTus arc is built in Earth's plane of the sky at the event start
  with `PolarBasis.vec3(r, angle)` and brought into the world with
  `earthView.rotateInverseVector`.
- The arc distance is `SWEKData.cactusDistance`, which starts at `CACTUS_START_RADIUS`
  and grows at the event speed from the event start to the viewpoint time.
- In the polar and log-polar views, `drawCactusArcScale` uses the position angle as the
  view's own angle, without `earthView`.
- Picking: `findProjectedEvent` uses the same position angle in the polar views.
  `findOrthographicEvent` rotates the arc point into view coordinates with
  `MapView.viewRotation` and compares its x and y with the mouse from `computeUpX` and
  `computeUpY`.

## Viewpoints

`UpdateViewpoint` gives the viewpoint for each camera mode:
- `observer` is the active image's metadata viewpoint. `observerAt1au` is the same at
  1 AU.
- `earthAt1au` is Earth's longitude at latitude 0 and 1 AU.
- `Location` interpolates the chosen `PositionLoad` with `interpolateCarrington`, or uses
  Earth when there is no response.
- `Equatorial` is the Heliosphere view. Its latitude is pi/2. Its time is the playback
  time mapped through the control load's `interpolateTime`. Its longitude is Earth's
  longitude at that time, plus Earth's HCI longitude when the frame is HCI, minus the
  longitude of the control object in relative mode, plus pi/2.

## Drawing in another frame

Layers draw in another frame with `Transform.pushView`, `Transform.rotateViewInverse(q)`,
the drawing, and `Transform.popView`.
- `GridLayer` uses `GridType.toCarrington(viewpoint)` for the grid lines and labels.
  `GridType.toLongitude` is the viewpoint longitude for Viewpoint, Earth's longitude for
  Stonyhurst, 0 for Carrington, and Earth's longitude plus Earth's HCI longitude for HCI.
  The Earth circles use `Sun.getEarth(viewpoint.time).toQuat()`, and the radial grid uses
  the viewpoint. `OrthographicView.mouseToMap` uses the grid rotation for mouse
  coordinates.
- `ViewpointLayer` rotates by `Quat.createXY(viewpoint.lat, viewpoint.lon + relativeLon)`,
  which adds back the relative longitude of `Equatorial`. It draws the planet orbits and
  points with the raw x, y, z of the load frame from `interpolateRectangular`, SPICE axes
  with z north. The Parker spiral starts from the control object's position from
  `interpolateLatitudinal`.
- `ViewpointSky` rotates by `MapView.viewRotation()`, because its points are in screen
  coordinates.
- `FOVPlatform` uses the observer from `Spice.getCarrington` at the viewpoint time. For
  Solar Orbiter it adds a rotation about z from
  `Spice.getRotationEuler("SOLO_SRF", "SOLO_IAU_SUN_2009", time)`.
- Annotations. Circle, cross, loop and rectangle store world points from
  `mouseToSurface` and are drawn in the world. Line and FOV store view points from
  `mouseToSky` and are drawn by `drawTransformed` under
  `rotateViewInverse(viewpoint.toQuat())` in the orthographic view, so they stay with the
  viewpoint. Saved states write both kinds as r, lon and lat with `SphericalCoords`.

## Rotation over time

- Images: with differential rotation on, `GLSLImageShader` passes the time from the image's
  tracing viewpoint to the display time. For surface maps the tracing viewpoint is the
  display, so the time is 0 and they do not rotate. `imageGeometry.frag` rotates
  the sphere point about the pole by the Snodgrass magnetic rate minus 14.1844 deg/day,
  which is relative to Carrington.
- Parker spiral: `ViewpointLayer.renderSpiral` uses `Sun.RotationRate`, 14.1844 deg/day
  from `BODY10_PM`.
- `Carrington.time2CR` uses Earth's longitude for the fraction of the rotation number.

## Kernel values copied into code

- `Sun.RotationRate` copies the rate of `BODY10_PM`.
- `ViewpointSky.SOLAR_POLE` copies `BODY10_POLE_RA` and `BODY10_POLE_DEC` (286.13, 63.87).
- `imageGeometry.frag` copies 14.1844 deg/day into the differential rotation.
