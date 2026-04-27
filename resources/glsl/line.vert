#version 300 es

layout(location = 0) in vec4 PrevVertex;
layout(location = 1) in vec4 PrevColor;
layout(location = 2) in vec4 CurrVertex;
layout(location = 3) in vec4 CurrColor;
layout(location = 4) in vec4 NextVertex;
layout(location = 5) in vec4 NextColor;
layout(location = 6) in vec4 FollowVertex;
layout(location = 7) in vec4 FollowColor;
out vec4 fragColor;
out vec2 segmentStart;
out vec2 segmentEnd;
out float halfWidthPixels;

struct Screen {
    mat4 mvp;
    vec4 viewport;
    float thickness;
};

layout(std140) uniform ScreenBlock {
    Screen screen;
};

const float dir[2] = float[2](1.0, -1.0);

// Each logical line segment is rendered as an instanced rectangle in
// framebuffer space. The fragment shader then computes distance to the segment
// in pixels, giving stable AA without relying on native wide/smooth GL lines.
const float aaPixels = 1.5;

vec3 safeDirection(vec3 from, vec3 to, vec3 fallback) {
    vec3 delta = to - from;
    float len = length(delta);
    return len > 0.0 ? delta / len : fallback;
}

float joinOverlap(vec3 incoming, vec3 outgoing, float radius) {
    float cosAngle = clamp(dot(incoming, outgoing), -1.0, 1.0);
    float sinAngle = length(cross(incoming, outgoing));

    // Extend neighboring segment rectangles until their visible stroke edges
    // meet. This is r * tan(theta / 2), written as r * sin(theta)/(1+cos(theta)).
    return radius * sinAngle / max(1.0 + cosAngle, 1.0e-4);
}

void main(void) {
    vec2 viewportOrigin = screen.viewport.xy;
    vec2 viewportSize = screen.viewport.zw;
    vec2 ndcToPixel = 0.5 * viewportSize;
    vec2 pixelToNdc = 1.0 / ndcToPixel;

    vec4 curr = screen.mvp * CurrVertex;
    vec4 next = screen.mvp * NextVertex;

    vec2 currPixel = viewportOrigin + (curr.xy / curr.w + 1.0) * ndcToPixel;
    vec2 nextPixel = viewportOrigin + (next.xy / next.w + 1.0) * ndcToPixel;

    vec2 segmentDelta = nextPixel - currPixel;
    float segmentLength = length(segmentDelta);
    vec2 segmentTangent = segmentLength > 0.0 ? segmentDelta / segmentLength : vec2(0.0);
    vec2 segmentNormal = vec2(-segmentTangent.y, segmentTangent.x);

    // GLSLLineShader passes half the requested line width in NDC. Convert it
    // to framebuffer pixels so the fragment shader can use gl_FragCoord.
    halfWidthPixels = screen.thickness * viewportSize.y * 0.5;

    // The AA shader shades finite segments. Without a small geometric overlap
    // at polyline joins, tiny cracks can appear between independently rendered
    // segment rectangles. Compute the join angle in the line's geometry space,
    // not screen space: any 3D arc can become edge-on, making projected
    // neighbor directions collapse even though the curve is well behaved.
    bool hasPrev = PrevColor.a > 0.0;
    bool hasFollow = FollowColor.a > 0.0;
    vec3 segmentDirection = safeDirection(CurrVertex.xyz, NextVertex.xyz, vec3(0.0));
    vec3 prevDirection = safeDirection(PrevVertex.xyz, CurrVertex.xyz, segmentDirection);
    vec3 followDirection = safeDirection(NextVertex.xyz, FollowVertex.xyz, segmentDirection);
    float visibleRadius = halfWidthPixels + 0.5 * aaPixels;
    float startOverlap = hasPrev ? joinOverlap(prevDirection, segmentDirection, visibleRadius) : 0.0;
    float endOverlap = hasFollow ? joinOverlap(segmentDirection, followDirection, visibleRadius) : 0.0;

    segmentStart = currPixel - segmentTangent * startOverlap;
    segmentEnd = nextPixel + segmentTangent * endOverlap;

    int idx = (gl_VertexID >> 1) & 0x1;
    vec4 pos = idx == 0 ? curr : next;
    vec4 color = idx == 0 ? CurrColor : NextColor;
    float side = dir[gl_VertexID & 0x1];
    float endpointOverlap = idx == 0 ? -startOverlap : endOverlap;

    // Reserve geometry for both the nominal stroke and the AA fringe. The
    // fragment shader decides final coverage; this only guarantees pixels exist
    // for it to shade.
    vec2 offsetNdc = (side * segmentNormal * (halfWidthPixels + aaPixels) + segmentTangent * endpointOverlap) * pixelToNdc;
    gl_Position = vec4(pos.xy + offsetNdc * pos.w, pos.zw);
    fragColor = color;
}
