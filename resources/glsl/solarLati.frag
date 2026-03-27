vec3 displayLatitudinalWorld(const vec2 scrpos, const vec4 displayMapQuat) {
    float longitude = radians(mix(screen.xStart, screen.xStop, scrpos.x));
    float latitude = radians(mix(screen.yStart, screen.yStop, scrpos.y));
    clamp_value(latitude, -HALFPI, HALFPI);
    vec3 displaySurface = vec3(
        cos(latitude) * sin(longitude),
        sin(latitude),
        cos(latitude) * cos(longitude));
    return rotate_vector_inverse(displayMapQuat, displaySurface);
}

vec2 sampleLatiTexcoord(const vec2 scrpos, const WCS wcs, const ProjectionParams projection) {
    if (int(projection.projectionCode) == WCS_PROJECTION_CAR) {
        vec3 world = displayLatitudinalWorld(scrpos, projection.displayMapQuat);
        vec2 plane = projectCarToWcsPlane(world, wcs.crval, projection.planeUnitsPerRadian);
        vec2 texCoord = wcsPlaneToTexcoord(plane, wcs);
        clamp_texture(texCoord);
        return texCoord;
    }

    vec3 grid = projection.legacyGrid.xyz;
    float longitude = grid.x + scrpos.x * TWOPI;
    float latitude = grid.y + (scrpos.y - 0.5) * PI;

    if (wcs.deltaT != 0.)
        longitude -= differentialRotation(wcs.deltaT, latitude);

    clamp_value(latitude, -HALFPI, HALFPI);

    float cosLatitude = cos(latitude);
    vec3 spherical = vec3(
        cosLatitude * cos(longitude),
        cosLatitude * sin(longitude),
        sin(latitude));

    float sinGridLatitude = -sin(grid.z);
    float cosGridLatitude = cos(grid.z);
    mat3 rot = mat3(
        cosGridLatitude, 0., sinGridLatitude,
        0., 1., 0.,
        -sinGridLatitude, 0., cosGridLatitude);
    vec3 sourceView = rot * spherical;
    if (sourceView.x < 0.)
        discard;

    vec3 centered = apply_center(vec3(sourceView.y, sourceView.z, 0.), wcs.crval, wcs.crota);
    vec2 texCoord = wcs.rect.zw * vec2(centered.x - wcs.rect.x, -centered.y - wcs.rect.y);
    clamp_texture(texCoord);
    return texCoord;
}

void main(void) {
    if (display.radii.x > 1.) // coronagraphs
        discard;

    vec2 scrpos = getScrPos();
    bool diffMode = display.isDiff != NODIFFERENCE;
    vec2 texCoord = sampleLatiTexcoord(scrpos, wcs[0], projection[0]);
    vec2 diffTexCoord = texCoord;
    if (diffMode)
        diffTexCoord = sampleLatiTexcoord(scrpos, wcs[1], projection[1]);
    outColor = getColor(texCoord, diffTexCoord, 1);
}
