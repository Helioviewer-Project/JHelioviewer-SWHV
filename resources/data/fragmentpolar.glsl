void get_polar_texcoord(const in vec4 rect, out vec2 texcoord, out float radius) {
    vec2 normalizedScreenpos = 2.*((((gl_FragCoord.xy-viewportOffset)/viewport)-.5)*vec2(viewport.y/viewport.x, 1.));
    vec4 scrpos =  cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.) +0.5;
    clamp_texcoord(scrpos.xy);
    float theta = -(scrpos.x*TWOPI + PI/2. - crota);
    float start = polarRadii.x;
    float end = polarRadii.y;
    float interpolated = start + scrpos.y * (end-start);
    if (cutOffValue>=0.) {
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

    texcoord = rect.zw * (-rect.xy + vec2(cos(theta), sin(theta)) * interpolated);
    clamp_texcoord(texcoord);
    radius = 1.;
    if (interpolated > 1.) {
        radius = interpolated * interpolated;
    }
}

void main(void)
{
    vec2 texcoord;
    float radius;
    get_polar_texcoord(rect, texcoord, radius);
    vec4 color;
    if (isdifference != NODIFFERENCE) {
        vec2 difftexcoord;
        float diffradius;
        get_polar_texcoord(differencerect, difftexcoord, diffradius);
        color = getColor(texcoord, difftexcoord, radius);
    } else {
        color = getColor(texcoord, texcoord, radius);
    }
    gl_FragColor = color;
}
