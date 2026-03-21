
// grid = (map longitude offset, map latitude offset, heliographic latitude)
vec2 sampleLatiTexcoord(const vec2 crval, const vec4 crota, const vec4 rect, const vec2 scrpos, const float dt, const vec3 grid) {
    float longitude = grid.x + scrpos.x * TWOPI;
    float latitude = grid.y + (scrpos.y - 0.5) * PI;

    if (dt != 0.) {
        longitude -= differentialRotation(dt, latitude); // difference from rigid rotation
    }

    clamp_value(latitude, -HALFPI, HALFPI);

    float cosLatitude = cos(latitude);
    float sinLatitude = sin(latitude);
    float cosLongitude = cos(longitude);
    float sinLongitude = sin(longitude);

    float sinGridLatitude = -sin(grid.z);
    float cosGridLatitude = cos(grid.z);
    vec3 rotatedSpherical = vec3(
        cosGridLatitude * cosLatitude * cosLongitude + sinGridLatitude * sinLatitude,
        cosLatitude * sinLongitude,
        -sinGridLatitude * cosLatitude * cosLongitude + cosGridLatitude * sinLatitude);
    if (rotatedSpherical.x < 0.)
        discard;

    // The map uses latitude directly; texture-space still uses -centered.y.
    vec3 centered = apply_center(vec3(rotatedSpherical.y, rotatedSpherical.z, 0.), crval, crota);
    vec2 texcoord = rect.zw * vec2(centered.x - rect.x, -centered.y - rect.y);
    clamp_texture(texcoord);

    return texcoord;
}

void main(void) {
    if (display.radii.x > 1.) // coronagraphs
        discard;

    vec4 color;

    vec2 scrpos = getScrPos();
    bool diffMode = display.isDiff != NODIFFERENCE;
    vec2 texcoord = sampleLatiTexcoord(wcs[0].crval, wcs[0].crota, wcs[0].rect, scrpos, wcs[0].deltaT, grid[0]);
    if (!diffMode) {
        color = getColor(texcoord, texcoord, 1);
    } else {
        vec2 difftexcoord = sampleLatiTexcoord(wcs[1].crval, wcs[1].crota, wcs[1].rect, scrpos, wcs[1].deltaT, grid[1]);
        color = getColor(texcoord, difftexcoord, 1);
    }
    outColor = color;
}
