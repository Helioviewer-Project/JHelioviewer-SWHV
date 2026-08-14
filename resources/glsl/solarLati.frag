vec3 latitudinalWorld(const vec2 mapPos) {
    float longitude = radians(mix(screen.mapBounds.x, screen.mapBounds.y, mapPos.x)) - screen.latiOrigin.x;
    float latitude = radians(mix(screen.mapBounds.z, screen.mapBounds.w, mapPos.y)) + screen.latiOrigin.y;
    if (latitude < -HALFPI || latitude > HALFPI)
        discard;
    float cosLatitude = cos(latitude);
    return vec3(
        cosLatitude * sin(longitude),
        sin(latitude),
        cosLatitude * cos(longitude));
}

vec2 sampleLatiTexcoord(vec3 world, const Image img, const float[6] PV) {
    if (isSurfaceMap(img))
        return sampleSurfaceMapTexcoord(world, img, PV);

    if (img.deltaT != 0.)
        world = differential(img.deltaT, world);

    vec3 sourceWorld = rotate_vector(img.sourceViewQuat, world);
    if (sourceWorld.z < 0.)
        discard;

    vec2 helioprojective = worldToHelioprojective(sourceWorld, img.observerDistance);
    return helioprojectiveToTexcoord(helioprojective, img, PV);
}

void main(void) {
    if (display.radii.x > 1.) // coronagraphs
        discard;

    vec2 mapPos = getNormalizedMapPos();
    bool diffMode = display.isDiff != NODIFFERENCE;
    vec3 world = latitudinalWorld(mapPos);

    vec2 texCoord = sampleLatiTexcoord(world, images[0], pv0);
    vec2 diffTexCoord = diffMode ? sampleLatiTexcoord(world, images[1], pv1) : texCoord;
    outColor = getColor(texCoord, diffTexCoord, 1.0);
}
