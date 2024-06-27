#version 330 core

//precision mediump float;

in vec2 fragCoord;
out vec4 outColor;

uniform vec4 color;
uniform sampler2D image;

void main(void) {
    outColor = color * texture(image, fragCoord);
    if (length(outColor) < 0.1) // hollow letters
        discard;
}
