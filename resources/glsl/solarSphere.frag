#version 300 es

precision highp float;

out vec4 outColor;
in vec2 normalizedScreenpos;

layout(std140) uniform ScreenBlock {
    mat4 inverseMVP;
} screen;

const vec4 black = vec4(0, 0, 0, 1);

float getDepth(const float viewZ) {
    return 0.5 * (1. + viewZ / screen.inverseMVP[2][2]);
}

void main(void) {
    vec2 viewPosition = (screen.inverseMVP * vec4(normalizedScreenpos, -1., 1.)).xy;

    float radius2 = dot(viewPosition, viewPosition);
    if (radius2 > 1.)
        discard;
    gl_FragDepth = getDepth(sqrt(1. - radius2));

    outColor = black;
}
