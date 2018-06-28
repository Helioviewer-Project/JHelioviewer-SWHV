#version 150 core

precision mediump float;

uniform vec4 color;
uniform sampler2D image;

in vec2 texCoord;
out vec4 FragColor;

void main(void) {
    FragColor = color * texture(image, texCoord);
}
