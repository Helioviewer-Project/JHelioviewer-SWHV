bool latitudinalWorld(const vec2 mapPos, out vec3 world) {
    vec2 angles = radians(mix(screen.mapBounds.xz, screen.mapBounds.yw, mapPos)) + vec2(-screen.latiOrigin.x, screen.latiOrigin.y);
    if (angles.y < -HALFPI || angles.y > HALFPI)
        return false;
    vec2 lon = sinCos(angles.x);
    vec2 lat = sinCos(angles.y);
    world = vec3(lat.y * lon.x, lat.x, lat.y * lon.y);
    return true;
}

bool latiSample(vec3 world, const Image img, const float[6] PV, out vec2 texcoord) {
    bool surface = isSurfaceMap(img);
    if (!surface && img.deltaT != 0.)
        world = differential(img.deltaT, world);
    vec3 sourcePoint = rotate_vector(img.sourceViewQuat, world);
    vec3 direction = world;
    if (!surface) {
        if (sourcePoint.z < 1. / img.observerDistance)
            return false;
        direction = vec3(sourcePoint.xy, img.observerDistance - sourcePoint.z);
    }
    return imageTexcoord(direction, img, PV, texcoord) && insidePlanarMasks(sourcePoint.xy);
}

void main(void) {
    vec2 mapPos = getNormalizedMapPos();
    if (!insideMap(mapPos))
        discard;
    vec3 world;
    if (!latitudinalWorld(mapPos, world))
        discard;
    vec2 texCoord;
    if (!latiSample(world, images[0], pv0, texCoord))
        discard;
    vec2 diffTexCoord = texCoord;
    if (display.isDiff != NODIFFERENCE) {
        if (!latiSample(world, images[1], pv1, diffTexCoord))
            discard;
    }
    renderImage(texCoord, diffTexCoord, 1.);
}
