vec3 latitudinalWorld(const vec2 mapPos) {
    float longitude = radians(mix(screen.xStart, screen.xStop, mapPos.x)) - screen.latiOrigin.x;
    float latitude = radians(mix(screen.yStart, screen.yStop, mapPos.y)) + screen.latiOrigin.y;
    clamp_value(latitude, -HALFPI, HALFPI);
    float cosLatitude = cos(latitude);
    return vec3(
        cosLatitude * sin(longitude),
        sin(latitude),
        cosLatitude * cos(longitude));
}

vec2 sampleLatiTexcoord(vec3 world, const WCS wcs, const ProjectionParams projection, const float[6] PV) {
    if (projection.projectionCode == WCS_PROJECTION_CAR) {
        vec2 plane = projectCarToWcsPlane(world, wcs.crval, projection.planeUnitsPerRadian);
        return wcsPlaneToWrappedXTexcoord(plane, wcs);
    }
    if (projection.projectionCode == WCS_PROJECTION_CEA) {
        vec2 plane = projectCeaToWcsPlane(world, wcs.crval, projection.planeUnitsPerRadian, PV);
        return wcsPlaneToWrappedXTexcoord(plane, wcs);
    }

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
    vec3 world = latitudinalWorld(mapPos);

    vec2 texCoord = sampleLatiTexcoord(world, wcs[0], projection[0], pv0);
    vec2 diffTexCoord = diffMode ? sampleLatiTexcoord(world, wcs[1], projection[1], pv1) : texCoord;
    outColor = getColor(texCoord, diffTexCoord, 1.0);
}
