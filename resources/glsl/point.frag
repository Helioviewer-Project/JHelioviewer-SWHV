#version 330 core

//precision mediump float;

in vec4 fragColor;
out vec4 outColor;

const vec2 one = vec2(1);
const vec4 black = vec4(0);

void main(void) {
    vec2 coord = 2. * gl_PointCoord - one;
    float radius = length(coord);
    if (radius > 1.)
        discard;

    float delta = fwidth(radius);
    float alpha = smoothstep(1. - delta, 1., radius);
    outColor = mix(fragColor, black, alpha);
}
