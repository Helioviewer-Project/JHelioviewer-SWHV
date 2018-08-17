#version 330 core

precision mediump float;

layout(location = 0) out vec4 FragColor;

uniform vec4 color;
uniform sampler2D image;

in vec2 frag_coord;

void main(void) {
    FragColor = color * texture(image, frag_coord);
}
