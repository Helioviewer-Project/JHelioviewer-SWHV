#version 120
varying vec4 frag_color;

void main(void) {
    if (frag_color.a == 0.)
        discard;
    gl_FragColor = frag_color;
}
