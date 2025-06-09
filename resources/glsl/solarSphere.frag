
const vec4 black = vec4(0, 0, 0, 1);

void main(void) {
    vec2 normalizedScreenpos = 2. * (gl_FragCoord.xy - screen.viewportOffset) / screen.viewport.xy - 1.;
    vec4 up1 = screen.cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);

    if (dot(up1.xy, up1.xy) > 1.)
        discard;
    outColor = black;
}
