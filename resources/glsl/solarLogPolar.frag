
void get_polar_texcoord(const vec2 CRVAL, const vec4 CROTA, const vec4 rect, const vec2 scrpos, out vec2 texcoord, out float radius) {
    float interpolated = exp(screen.yStart + scrpos.y * (screen.yStop - screen.yStart));
    if (interpolated > display.radii.y || interpolated < display.radii.x)
        discard;

    float theta = -(scrpos.x * TWOPI + HALFPI /* - cr TBD */);
    vec3 pos = vec3(cos(theta), sin(theta), 0.) * interpolated;
    // if (interpolated < 1.)
    //     pos.z = interpolated;

    if (cutOff.z >= 0.) {
        vec2 dpos = pos.yx;
        vec2 cutOffAlt = vec2(-cutOff.y, cutOff.x);
        float geometryFlatDist = abs(dot(dpos, cutOff.xy));
        float geometryFlatDistAlt = abs(dot(dpos, cutOffAlt));
        if (geometryFlatDist > cutOff.z || geometryFlatDistAlt > cutOff.z)
            discard;
    }

    vec3 centered = apply_center(vec3(pos.x, -pos.y, 0.), CRVAL, CROTA);
    texcoord = rect.zw * vec2(centered.x - rect.x, -centered.y - rect.y);
    clamp_texture(texcoord);

    radius = 1.;
    if (interpolated > 1.) {
        radius = interpolated;
    }
}

void main(void) {
    vec4 color;
    vec2 texcoord;
    float radius;

    vec2 scrpos = getScrPos();
    get_polar_texcoord(wcs[0].crval, wcs[0].crota, wcs[0].rect, scrpos, texcoord, radius);
    if (display.isDiff == NODIFFERENCE) {
        color = getColor(texcoord, texcoord, radius);
    } else {
        vec2 difftexcoord;
        float diffradius;
        get_polar_texcoord(wcs[1].crval, wcs[1].crota, wcs[1].rect, scrpos, difftexcoord, diffradius);
        color = getColor(texcoord, difftexcoord, radius);
    }
    outColor = color;
}
