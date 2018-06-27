#version 150 core

uniform vec4 color;
uniform sampler2D image;

in vec2 texCoord;
out vec4 FragColor;

void main(void) {
    FragColor = color * texture(image, texCoord);
}
