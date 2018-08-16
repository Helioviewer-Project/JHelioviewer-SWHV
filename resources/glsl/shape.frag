#version 140

precision mediump float;

out vec4 FragColor;

in vec4 frag_color;

void main(void) {
    FragColor = frag_color;
}
