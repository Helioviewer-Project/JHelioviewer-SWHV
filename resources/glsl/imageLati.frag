bool latitudinalWorld(const vec2 mapPos, out vec3 world) {
    vec2 angles = radians(mix(screen.mapBounds.xz, screen.mapBounds.yw, mapPos)) + vec2(-screen.latiOrigin.x, screen.latiOrigin.y);
    if (angles.y < -HALFPI || angles.y > HALFPI)
        return false;
    vec2 lon = sinCos(angles.x);
    vec2 lat = sinCos(angles.y);
    world = vec3(lat.y * lon.x, lat.x, lat.y * lon.y);
    return true;
}

bool latiSample(const vec3 world, const Image img, out vec2 texcoord) {
    vec3 point = rotate_vector(img.solarToImage, differential(img.deltaT, world));
    return sampleImagePoint(img, point, texcoord) && insidePlanarMasks(point.xy);
}

void main(void) {
    vec2 mapPos = getNormalizedMapPos();
    if (!insideMap(mapPos))
        discard;
    vec3 world;
    if (!latitudinalWorld(mapPos, world))
        discard;
    vec2 texCoord;
    if (!latiSample(world, images[0], texCoord))
        discard;
    vec2 diffTexCoord = texCoord;
    if (display.isDiff != NODIFFERENCE) {
        if (!latiSample(world, images[1], diffTexCoord))
            discard;
    }
    renderImage(texCoord, diffTexCoord, 1.);
}
