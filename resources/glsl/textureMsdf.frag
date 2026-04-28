#version 300 es

precision highp float;

in vec2 fragCoord;
out vec4 outColor;

uniform vec4 color;
uniform sampler2D image;
uniform vec2 unitRange;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main(void) {
    vec3 msd = texture(image, fragCoord).rgb;
    float signedDistance = median(msd.r, msd.g, msd.b) - 0.5;

    vec2 screenTexSize = 1.0 / fwidth(fragCoord);
    float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
    float alpha = clamp(screenPxRange * signedDistance + 0.5, 0.0, 1.0);
    if (alpha <= 0.0)
        discard;
    outColor = color * alpha;
}
