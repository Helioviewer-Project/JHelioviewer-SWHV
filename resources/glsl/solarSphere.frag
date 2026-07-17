#version 300 es

precision highp float;

out vec4 outColor;
in vec2 normalizedScreenpos;

struct Screen {
    mat4 inverseMVP;
};

layout(std140) uniform ScreenBlock {
    Screen screen;
};

const vec4 black = vec4(0, 0, 0, 1);

void main(void) {
    vec4 up1 = screen.inverseMVP * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);

    if (dot(up1.xy, up1.xy) > 1.)
        discard;
    outColor = black;
}
