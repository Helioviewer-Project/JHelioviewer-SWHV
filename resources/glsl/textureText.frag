#version 300 es

precision highp float;

in vec2 fragCoord;
out vec4 outColor;

uniform vec4 color;
uniform sampler2D image;

void main(void) {
    float coverage = texture(image, fragCoord).r;
    if (coverage <= 0.0)
        discard;
    outColor = color * coverage;
}
