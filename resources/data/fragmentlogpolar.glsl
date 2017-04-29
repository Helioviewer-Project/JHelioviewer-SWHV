vec3 get_polar_texcoord(vec4 rect) {
    vec2 normalizedScreenpos = 2.*((((gl_FragCoord.xy-viewportOffset)/viewport)-.5)*vec2(viewport.y/viewport.x, 1.));
    vec4 scrpos =  cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.) +0.5;
    clamp_texcoord(scrpos.xy);
    float theta = -(scrpos.x*TWOPI + PI/2. - crota);
    float start = polarRadii.x;
    float end = polarRadii.y;
    float interpolated = exp(start + scrpos.y * (end-start));
    if (cutOffValue >= 0.) {
        vec3 dpos = vec3(sin(theta), cos(theta), 0.)*interpolated;
        vec3 cutOffDirectionAlt = vec3(-cutOffDirection.y, cutOffDirection.x, 0.);
        float geometryFlatDist = abs(dot(dpos, cutOffDirection));
        float geometryFlatDistAlt = abs(dot(dpos, cutOffDirectionAlt));
        if (geometryFlatDist > cutOffValue || geometryFlatDistAlt > cutOffValue) {
            discard;
        }
    }

    if (interpolated > cutOffRadius.y || interpolated < cutOffRadius.x) {
        discard;
    }

    vec2 texcoord = rect.zw * (-rect.xy + vec2(cos(theta), sin(theta)) * interpolated);
    clamp_texcoord(texcoord);
    vec3 tex_coord_and_color = vec3(texcoord.x, texcoord.y, 1.);
    float factor = 1.;
    if (interpolated > 1.) {
        factor = interpolated * interpolated;
    }
    tex_coord_and_color.z = factor;
    return tex_coord_and_color;
}

void main(void)
{
    vec3 texcoord = get_polar_texcoord(rect);
    vec4 color;
    if (isdifference != NODIFFERENCE) {
        vec3 difftexcoord = get_polar_texcoord(differencerect);
        color = getColor(texcoord.xy, difftexcoord.xy, texcoord.z);
    } else {
        color = getColor(texcoord.xy, texcoord.xy, texcoord.z);
    }
    gl_FragColor = color;
}
