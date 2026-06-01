#version 300 es

precision highp float;

in vec4 fragColor;
in vec2 prevPixel;
in vec2 currPixel;
in vec2 nextPixel;
in float hasPrev;
in float hasFollow;
in float halfWidthPixels;
out vec4 outColor;

const float aaPixels = 1.5;

// Shade in framebuffer pixels. Each instance owns one segment body; at the
// start vertex it may also fill the missing round join area. Shallow joins avoid
// double-blending the overlap between neighboring segment strips, while hard
// corners keep that overlap to avoid notches.
float edgeCoverage(float distance) {
    return 1.0 - smoothstep(halfWidthPixels - 0.5 * aaPixels, halfWidthPixels + 0.5 * aaPixels, distance);
}

float cross2(vec2 a, vec2 b) {
    return a.x * b.y - a.y * b.x;
}

float stripCoverage(vec2 p, vec2 start, vec2 tangent, float lengthPixels) {
    vec2 delta = p - start;
    float along = dot(delta, tangent);
    if (along < 0.0 || along > lengthPixels)
        return 0.0;

    return edgeCoverage(abs(cross2(tangent, delta)));
}

bool insideDirectedStrip(vec2 p, vec2 start, vec2 tangent, float direction) {
    vec2 delta = p - start;
    float along = direction * dot(delta, tangent);
    float across = abs(cross2(tangent, delta));
    return along >= 0.0 && across <= halfWidthPixels + aaPixels;
}

void main(void) {
    if (fragColor.a == 0.)
        discard;

    vec2 p = gl_FragCoord.xy;
    vec2 segmentDelta = nextPixel - currPixel;
    float segmentLength = length(segmentDelta);
    if (segmentLength <= 0.0)
        discard;

    vec2 segmentTangent = segmentDelta / segmentLength;
    float coverage = stripCoverage(p, currPixel, segmentTangent, segmentLength);

    if (hasPrev > 0.5) {
        vec2 prevDelta = currPixel - prevPixel;
        float prevLength = length(prevDelta);
        vec2 prevTangent = prevLength > 0.0 ? prevDelta / prevLength : segmentTangent;

        bool shallowJoin = dot(prevTangent, segmentTangent) > 0.5;
        bool inPrevStrip = insideDirectedStrip(p, currPixel, prevTangent, -1.0);
        bool inCurrentStrip = insideDirectedStrip(p, currPixel, segmentTangent, 1.0);

        if (shallowJoin && inPrevStrip)
            coverage = 0.0;

        if (!inCurrentStrip && !inPrevStrip)
            coverage = max(coverage, edgeCoverage(length(p - currPixel)));
    } else {
        coverage = max(coverage, edgeCoverage(length(p - currPixel)));
    }

    if (hasFollow <= 0.5)
        coverage = max(coverage, edgeCoverage(length(p - nextPixel)));

    if (coverage <= 0.0)
        discard;
    outColor = fragColor * coverage;
}
