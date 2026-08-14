vec3 latitudinalWorld(const vec2 mapPos, const vec2 origin) {
    float longitude = radians(mix(screen.xStart, screen.xStop, mapPos.x)) - origin.x;
    float latitude = radians(mix(screen.yStart, screen.yStop, mapPos.y)) + origin.y;
    clamp_value(latitude, -HALFPI, HALFPI);
    float cosLatitude = cos(latitude);
    return vec3(
        cosLatitude * sin(longitude),
        sin(latitude),
        cosLatitude * cos(longitude));
}

vec2 sampleLatiCarTexcoord(const vec2 mapPos, const WCS wcs, const ProjectionParams projection) {
    vec3 world = latitudinalWorld(mapPos, projection.latiOrigin);
    vec2 plane = projectCarToWcsPlane(world, wcs.crval, projection.planeUnitsPerRadian);
    return wcsPlaneToWrappedXTexcoord(plane, wcs);
}

vec2 sampleLatiCeaTexcoord(const vec2 mapPos, const WCS wcs, const ProjectionParams projection, const float[6] PV) {
    vec3 world = latitudinalWorld(mapPos, projection.latiOrigin);
    vec2 plane = projectCeaToWcsPlane(world, wcs.crval, projection.planeUnitsPerRadian, PV);
    return wcsPlaneToWrappedXTexcoord(plane, wcs);
}

vec2 sampleLatiZenithalTexcoord(const vec2 mapPos, const WCS wcs,
                                const ProjectionParams projection, const float[6] PV) {
    vec3 world = latitudinalWorld(mapPos, projection.latiOrigin);
    if (wcs.deltaT != 0.)
        world = differential(wcs.deltaT, world);

    vec3 sourceWorld = rotate_vector(projection.sourceViewQuat, world);
    if (sourceWorld.z < 0.)
        discard;

    vec2 helioprojective = worldToHelioprojective(sourceWorld, projection.observerDistance);
    vec2 plane = projectHelioprojectiveToWcsPlane(helioprojective, wcs, projection, PV);
    return wcsPlaneToTexcoord(plane, wcs);
}

void main(void) {
    if (display.radii.x > 1.) // coronagraphs
        discard;

    vec2 mapPos = getNormalizedMapPos();
    bool diffMode = display.isDiff != NODIFFERENCE;

    vec2 texCoord = projection[0].projectionCode == WCS_PROJECTION_CAR
            ? sampleLatiCarTexcoord(mapPos, wcs[0], projection[0])
            : projection[0].projectionCode == WCS_PROJECTION_CEA
            ? sampleLatiCeaTexcoord(mapPos, wcs[0], projection[0], pv0)
            : sampleLatiZenithalTexcoord(mapPos, wcs[0], projection[0], pv0);
    vec2 diffTexCoord = texCoord;
    if (diffMode)
        diffTexCoord = projection[1].projectionCode == WCS_PROJECTION_CAR
                ? sampleLatiCarTexcoord(mapPos, wcs[1], projection[1])
                : projection[1].projectionCode == WCS_PROJECTION_CEA
                ? sampleLatiCeaTexcoord(mapPos, wcs[1], projection[1], pv1)
                : sampleLatiZenithalTexcoord(mapPos, wcs[1], projection[1], pv1);
    outColor = getColor(texCoord, diffTexCoord, 1.0);
}
