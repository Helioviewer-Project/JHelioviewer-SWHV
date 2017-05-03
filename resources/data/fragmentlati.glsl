void get_lati_texcoord(const in vec4 rect, out vec2 texcoord, out float radius) {
    vec2 normalizedScreenpos = (gl_FragCoord.xy - viewportOffset) / viewport - .5;
    normalizedScreenpos *= 2. * vec2(viewport.y / viewport.x, 1.);
    vec4 scrpos = cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.) + .5;
    clamp_texcoord(scrpos.xy);

    float theta = scrpos.y * PI;
    float phi = PI + hgln + scrpos.x * TWOPI;
    while (phi > TWOPI)
        phi -= TWOPI;

    vec3 xcart;
    xcart.x = sin(theta) * cos(phi);
    xcart.y = sin(theta) * sin(phi);
    xcart.z = cos(theta);
    /*
    mat3 crot = mat3(
         1.,  0.,         0.,
         0.,  cos(crota), sin(crota),
         0., -sin(crota), cos(crota)
    );
    mat3 rot = mat3(
          cos(hglt), 0., sin(hglt),
          0.,        1., 0.,
         -sin(hglt), 0., cos(hglt)
    );
    mat3 crotm = crot * rot;
    */
    mat3 crotm = mat3(
       cos(hglt),                0.,         sin(hglt),
       -sin(crota) * sin(hglt),  cos(crota), sin(crota) * cos(hglt),
       -cos(crota) * sin(hglt), -sin(crota), cos(crota) * cos(hglt) 
    );
    vec3 xcartrot = crotm * xcart;
    if (xcartrot.x < 0.)
        discard;
    float f = xcartrot.z * xcartrot.z + xcartrot.y * xcartrot.y;
    texcoord = vec2(rect.w * (xcartrot.y - rect.x), rect.w * (xcartrot.z - rect.y));
    radius = f*f;
    clamp_texcoord(texcoord);
}

void main(void) {
    if (cutOffRadius.x > 1.)
        discard;
    vec4 color;
    vec2 texcoord;
    float texcoord_radius;
    get_lati_texcoord(rect, texcoord, texcoord_radius); 
    if (isdifference != NODIFFERENCE) {
        vec2 difftexcoord;
        float difftexcoord_radius;
        get_lati_texcoord(differencerect, difftexcoord, difftexcoord_radius);
        color = getColor(texcoord, difftexcoord, texcoord_radius);
    } else {
        color = getColor(texcoord, texcoord, texcoord_radius);
    }
    gl_FragColor = color;
}
