#version 300 es

precision highp float;

in vec4 fragColor;
out vec4 outColor;

void main(void) {
    if (fragColor.a == 0.)
        discard;
    outColor = fragColor;
}
