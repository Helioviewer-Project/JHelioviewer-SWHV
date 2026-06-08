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
flat out vec2 prevPixel;
flat out vec2 currPixel;
flat out vec2 nextPixel;
flat out float hasPrev;
flat out float hasFollow;
flat out float halfWidthPixels;

struct Screen {
    mat4 mvp;
    vec4 viewport;
    float thickness;
};

layout(std140) uniform ScreenBlock {
    Screen screen;
};

const float dir[2] = float[2](1.0, -1.0);

// The fragment shader computes the exact stroke and join coverage. This stage
// only reserves enough screen-space geometry around the current segment.
const float aaPixels = 1.5;

void main(void) {
    vec2 viewportOrigin = screen.viewport.xy;
    vec2 viewportSize = screen.viewport.zw;
    vec2 ndcToPixel = 0.5 * viewportSize;
    vec2 pixelToNdc = 1.0 / ndcToPixel;

    vec4 prev = screen.mvp * PrevVertex;
    vec4 curr = screen.mvp * CurrVertex;
    vec4 next = screen.mvp * NextVertex;

    prevPixel = viewportOrigin + (prev.xy / prev.w + 1.0) * ndcToPixel;
    currPixel = viewportOrigin + (curr.xy / curr.w + 1.0) * ndcToPixel;
    nextPixel = viewportOrigin + (next.xy / next.w + 1.0) * ndcToPixel;

    vec2 segmentDelta = nextPixel - currPixel;
    float segmentLength = length(segmentDelta);
    vec2 segmentTangent = segmentLength > 0.0 ? segmentDelta / segmentLength : vec2(0.0);
    vec2 segmentNormal = vec2(-segmentTangent.y, segmentTangent.x);

    halfWidthPixels = screen.thickness * viewportSize.y * 0.5;
    hasPrev = PrevColor.a > 0.0 ? 1.0 : 0.0;
    hasFollow = FollowColor.a > 0.0 ? 1.0 : 0.0;

    int idx = (gl_VertexID >> 1) & 0x1;
    vec4 pos = idx == 0 ? curr : next;
    vec4 color = idx == 0 ? CurrColor : NextColor;
    float side = dir[gl_VertexID & 0x1];
    float radius = halfWidthPixels + aaPixels;
    float endpointOverlap = idx == 0 ? -radius : radius;

    vec2 offsetNdc = (side * segmentNormal * radius + segmentTangent * endpointOverlap) * pixelToNdc;
    gl_Position = vec4(pos.xy + offsetNdc * pos.w, pos.zw);
    fragColor = color;
}
