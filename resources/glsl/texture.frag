#version 150 core

in vec2 texCoord;
out vec4 FragColor;

uniform vec4 color;
uniform sampler2D image;

void main(void) {
    FragColor = color * texture(image, texCoord);
}
