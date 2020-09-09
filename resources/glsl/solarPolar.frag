void get_polar_texcoord(const float cr, const vec2 scrpos, const vec4 rect, out vec2 texcoord, out float radius, vec4 crota) {
    float interpolated = polarRadii[0] + scrpos.y * (polarRadii[1] - polarRadii[0]);
    if (interpolated > radii[1] || interpolated < radii[0])
        discard;

    float theta = -(scrpos.x * TWOPI + HALFPI);
    vec3 pos = vec3(cos(theta) * interpolated ,sin(theta) * interpolated, 0);
    if (interpolated<1.)
       pos.z = interpolated;

    if (cutOffValue >= 0.) {
        vec2 dpos = pos.yx;
        vec2 cutOffDirectionAlt = vec2(-cutOffDirection.y, cutOffDirection.x);
        float geometryFlatDist = abs(dot(dpos, cutOffDirection));
        float geometryFlatDistAlt = abs(dot(dpos, cutOffDirectionAlt));
        if (geometryFlatDist > cutOffValue || geometryFlatDistAlt > cutOffValue)
            discard;
    }

    vec3 centeredHitPoint = apply_center(pos, crval[0], crota);
    texcoord = rect.zw * vec2(centeredHitPoint.x - rect.x, -centeredHitPoint.y - rect.y);
    clamp_coord(texcoord);

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
    get_polar_texcoord(crota[0], scrpos, rect, texcoord, radius, crotaQuat[0]);
    if (isdifference == NODIFFERENCE) {
        color = getColor(texcoord, texcoord, radius);
    } else {
        vec2 difftexcoord;
        float diffradius;
        get_polar_texcoord(crotaDiff[0], scrpos, differencerect, difftexcoord, diffradius, crotaQuat[1]);
        color = getColor(texcoord, difftexcoord, radius);
    }
    outColor = color;
}
