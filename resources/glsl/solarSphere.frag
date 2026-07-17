#version 300 es

precision highp float;

out vec4 outColor;
in vec2 normalizedScreenpos;

layout(std140) uniform ScreenBlock {
    mat4 inverseMVP;
} screen;

const vec4 black = vec4(0, 0, 0, 1);

void main(void) {
    vec2 viewPosition = (screen.inverseMVP * vec4(normalizedScreenpos, -1., 1.)).xy;

    if (dot(viewPosition, viewPosition) > 1.)
        discard;
    outColor = black;
}
