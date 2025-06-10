#version 330 core

//precision mediump float;

out vec4 outColor;

struct Screen {
    mat4 cameraTransformationInverse;
    vec2 viewport;
    float iaspect;
    float padding;
    vec2 viewportOffset;
};

layout(std140) uniform ScreenBlock {
    Screen screen;
};

const vec4 black = vec4(0, 0, 0, 1);

void main(void) {
    vec2 normalizedScreenpos = 2. * (gl_FragCoord.xy - screen.viewportOffset) / screen.viewport.xy - 1.;
    vec4 up1 = screen.cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);

    if (dot(up1.xy, up1.xy) > 1.)
        discard;
    outColor = black;
}
