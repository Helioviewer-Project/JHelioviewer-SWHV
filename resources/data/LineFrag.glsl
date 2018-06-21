#version 120
varying vec4 frag_linecolor;

void main() {
    if (frag_linecolor.a == 0.)
        discard;
    gl_FragColor = frag_linecolor;
}
