#version 300 es

precision highp float;

out vec4 outColor;
in vec2 normalizedScreenpos;

layout(std140) uniform ScreenBlock {
    mat4 inverseMVP;
} screen;

const vec4 black = vec4(0, 0, 0, 1);
const float CLIP_SCALE_NARROW = 1. / (2. * 32.);

void main(void) {
    vec2 viewPosition = (screen.inverseMVP * vec4(normalizedScreenpos, -1., 1.)).xy;

    float radius2 = dot(viewPosition, viewPosition);
    if (radius2 > 1.)
        discard;
    gl_FragDepth = 0.5 - sqrt(1. - radius2) * CLIP_SCALE_NARROW;

    outColor = black;
}
