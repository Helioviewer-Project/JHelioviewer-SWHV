#version 120
varying vec4 frag_color;

void main(void) {
    vec2 coord = 2. * gl_PointCoord - vec2(1.);
    float radius = length(coord);
    if (radius > 1.)
        discard;

    float delta = fwidth(radius);
    float alpha = smoothstep(1 - delta, 1, radius);
    gl_FragColor = mix(frag_color, vec4(0.), alpha);
}
