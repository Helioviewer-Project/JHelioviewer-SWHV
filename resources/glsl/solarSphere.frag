#version 330 core

//precision mediump float;

out vec4 outColor;

struct Screen {
    mat4 inverseMVP;
    vec4 viewport;
};

layout(std140) uniform ScreenBlock {
    Screen screen;
};

const vec4 black = vec4(0, 0, 0, 1);

void main(void) {
    vec2 normalizedScreenpos = 2. * (gl_FragCoord.xy - screen.viewport.xy) / screen.viewport.zw - 1.;
    vec4 up1 = screen.inverseMVP * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);

    if (dot(up1.xy, up1.xy) > 1.)
        discard;
    outColor = black;
}
