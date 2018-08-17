#version 140

precision mediump float;

in vec4 fragColor;
out vec4 FragColor;

void main(void) {
    vec2 coord = 2. * gl_PointCoord - vec2(1.);
    float radius = length(coord);
    if (radius > 1.)
        discard;

    float delta = fwidth(radius);
    float alpha = smoothstep(1. - delta, 1., radius);
    FragColor = mix(fragColor, vec4(0.), alpha);
}
