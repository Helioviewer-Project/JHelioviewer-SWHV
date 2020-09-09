
void get_polar_texcoord(const vec2 CRVAL, const vec4 CROTA, const vec2 scrpos, const vec4 rect, out vec2 texcoord, out float radius) {
    float interpolated = exp(polarRadii[0] + scrpos.y * (polarRadii[1] - polarRadii[0]));
    if (interpolated > radii[1] || interpolated < radii[0])
        discard;

    float theta = -(scrpos.x * TWOPI + HALFPI /* - cr TBD */);
    vec2 pos = vec2(cos(theta), sin(theta)) * interpolated;

    if (cutOffValue >= 0.) {
        vec2 dpos = pos.yx;
        vec2 cutOffDirectionAlt = vec2(-cutOffDirection.y, cutOffDirection.x);
        float geometryFlatDist = abs(dot(dpos, cutOffDirection));
        float geometryFlatDistAlt = abs(dot(dpos, cutOffDirectionAlt));
        if (geometryFlatDist > cutOffValue || geometryFlatDistAlt > cutOffValue)
            discard;
    }

    vec3 p = apply_center(vec3(pos, 1.), CRVAL, CROTA);
    texcoord = rect.zw * vec2(p.x - rect.x, -p.y - rect.y);
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
    get_polar_texcoord(crval[0], crotaQuat[0], scrpos, rect, texcoord, radius);
    if (isdifference == NODIFFERENCE) {
        color = getColor(texcoord, texcoord, radius);
    } else {
        vec2 difftexcoord;
        float diffradius;
        get_polar_texcoord(crval[1], crotaQuat[1], scrpos, differencerect, difftexcoord, diffradius);
        color = getColor(texcoord, difftexcoord, radius);
    }
    outColor = color;
}
