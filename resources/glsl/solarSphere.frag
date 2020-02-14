#version 330 core

//precision mediump float;

out vec4 outColor;

uniform mat4 cameraTransformationInverse;
uniform vec3 viewport;
uniform vec2 viewportOffset;

void main(void) {
    vec2 normalizedScreenpos = 2. * (gl_FragCoord.xy - viewportOffset) / viewport.xy - 1.;
    vec4 up1 = cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);

    if (dot(up1.xy, up1.xy) > 1)
        discard;
    outColor = vec4(0, 0, 0, 1);
}
