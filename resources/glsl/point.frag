#version 300 es

precision highp float;

in vec4 fragColor;
out vec4 outColor;

void main(void) {
    vec2 coord = 2.0 * gl_PointCoord - vec2(1.0);
    float radius2 = dot(coord, coord);
    if (radius2 > 1.0)
      discard;

    float radius = sqrt(radius2);
    float delta = fwidth(radius);
    outColor = vec4(fragColor.rgb, fragColor.a * (1.0 - smoothstep(1.0 - delta, 1.0, radius)));
}
