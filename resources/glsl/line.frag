#version 140

in vec4 frag_color;
out vec4 FragColor;

void main(void) {
    if (frag_color.a == 0.)
        discard;
     FragColor = frag_color;
}
