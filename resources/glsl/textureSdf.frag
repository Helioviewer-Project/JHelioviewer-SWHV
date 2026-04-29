#version 300 es

precision highp float;

in vec2 texCoord;
out vec4 outColor;

uniform vec4 color;
uniform sampler2D image;
uniform vec2 unitRange;

void main(void) {
    float signedDistance = texture(image, texCoord).r - 0.5;

    vec2 screenTexSize = 1.0 / fwidth(texCoord);
    float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
    float coverage = clamp(screenPxRange * signedDistance + 0.5, 0.0, 1.0);
    if (coverage <= 0.0)
        discard;
    outColor = color * coverage;
}
