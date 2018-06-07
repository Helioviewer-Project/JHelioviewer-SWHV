#version 120
varying vec4 frag_color;

void main() {
    vec2 coord = 2. * gl_PointCoord - vec2(1.);
    if (dot(coord, coord) > 1.)
        discard;
    gl_FragColor = frag_color;
}
