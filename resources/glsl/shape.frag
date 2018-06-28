#version 150 core

precision mediump float;

in vec4 frag_color;
out vec4 FragColor;

void main(void) {
    FragColor = frag_color;
}
