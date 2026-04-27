#version 300 es

precision highp float;

in vec4 fragColor;
in vec2 segmentStart;
in vec2 segmentEnd;
in float halfWidthPixels;
out vec4 outColor;

// Must not exceed the geometry fringe reserved by the vertex shader.
const float aaPixels = 1.5;

// Distance to the finite segment is used instead of distance to the infinite
// supporting line. That keeps the same AA treatment at the long sides and at
// the short end caps of the generated rectangle.
float distanceToSegment(vec2 p, vec2 a, vec2 b) {
    vec2 ab = b - a;
    float len2 = dot(ab, ab);
    if (len2 <= 0.0)
        return length(p - a);

    // Closest point on the finite segment, not on the infinite line.
    float t = clamp(dot(p - a, ab) / len2, 0.0, 1.0);
    return length(p - (a + t * ab));
}

void main(void) {
    if (fragColor.a == 0.)
        discard;

    // Use true framebuffer-pixel distance so the long line edges get stable AA,
    // independent of perspective interpolation and line angle.
    float distance = distanceToSegment(gl_FragCoord.xy, segmentStart, segmentEnd);

    // Center the one-pixel AA ramp on the original edge: half inside the line,
    // half in the expanded fringe. This avoids making lines visibly thicker.
    float coverage = 1.0 - smoothstep(halfWidthPixels - 0.5 * aaPixels, halfWidthPixels + 0.5 * aaPixels, distance);
    if (coverage <= 0.0)
        discard;
    outColor = fragColor * coverage;
}
