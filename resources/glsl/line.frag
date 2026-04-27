#version 300 es

precision highp float;

in vec4 fragColor;
in vec2 segmentStart;
in vec2 segmentEnd;
in float halfWidthPixels;
out vec4 outColor;

// Match the fringe reserved by the vertex shader. The ramp is centered on the
// requested line edge, so AA softens the edge without making the line thicker.
const float aaPixels = 1.5;

// Use distance to the finite segment, not the infinite supporting line. This
// gives the same coverage rule for long sides and end caps, including the small
// join overlap injected by the vertex shader.
float distanceToSegment(vec2 p, vec2 a, vec2 b) {
    vec2 ab = b - a;
    float len2 = dot(ab, ab);
    if (len2 <= 0.0)
        return length(p - a);

    float t = clamp(dot(p - a, ab) / len2, 0.0, 1.0);
    return length(p - (a + t * ab));
}

void main(void) {
    if (fragColor.a == 0.)
        discard;

    // gl_FragCoord and segmentStart/End are both framebuffer pixels. Keeping the
    // distance calculation in this space makes AA independent of aspect ratio,
    // perspective interpolation, and segment angle.
    float distance = distanceToSegment(gl_FragCoord.xy, segmentStart, segmentEnd);

    float coverage = 1.0 - smoothstep(halfWidthPixels - 0.5 * aaPixels, halfWidthPixels + 0.5 * aaPixels, distance);
    if (coverage <= 0.0)
        discard;
    outColor = fragColor * coverage;
}
