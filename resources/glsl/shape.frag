#version 330 core

precision mediump float;

layout(location = 0) out vec4 FragColor;

in vec4 frag_color;

void main(void) {
    FragColor = frag_color;
}
