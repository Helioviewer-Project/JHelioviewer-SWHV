
vec2 get_lati_texcoord(const vec2 CRVAL, const vec4 CROTA, const vec4 rect, const vec2 scrpos, const float dt, const vec3 grid) {
    float phi   = grid.x + scrpos.x * TWOPI;
    float theta = grid.y + scrpos.y * PI;

    if (dt != 0) {
        phi -= differentialRotation(dt, theta - HALFPI); // difference from rigid rotation
    }

    clamp_value(theta, 0, PI);

    vec3 xcart;
    xcart.x = sin(theta) * cos(phi);
    xcart.y = sin(theta) * sin(phi);
    xcart.z = cos(theta);

    float slt = sin(grid.z);
    float clt = cos(grid.z);
    mat3 rot = mat3(
          clt, 0., slt,
           0., 1.,  0.,
         -slt, 0., clt);

    vec3 xcartrot = rot * xcart;
    if (xcartrot.x < 0.)
        discard;

    vec3 centered = apply_center(vec3(xcartrot.y, -xcartrot.z, 0.), CRVAL, CROTA);
    vec2 texcoord = rect.zw * vec2(centered.x - rect.x, -centered.y - rect.y);
    clamp_texture(texcoord);

    return texcoord;
}

void main(void) {
    if (display.radii.x > 1.) // coronagraphs
        discard;

    vec4 color;

    vec2 scrpos = getScrPos();
    vec2 texcoord = get_lati_texcoord(wcs[0].crval, wcs[0].crota, wcs[0].rect, scrpos, wcs[0].deltaT, grid[0]);
    if (display.isDiff == NODIFFERENCE) {
        color = getColor(texcoord, texcoord, 1);
    } else {
        vec2 difftexcoord = get_lati_texcoord(wcs[1].crval, wcs[1].crota, wcs[1].rect, scrpos, wcs[1].deltaT, grid[1]);
        color = getColor(texcoord, difftexcoord, 1);
    }
    outColor = color;
}
