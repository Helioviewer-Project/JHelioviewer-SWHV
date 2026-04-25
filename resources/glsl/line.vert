#version 300 es

layout(location = 0) in vec4 Vertex;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec4 NextVertex;
layout(location = 3) in vec4 NextColor;
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
// Must cover at least the fragment shader AA ramp width.
const float aaPixels = 1.0;

// ANGLE on macOS does not expose wide/smooth native GL lines reliably. Each
// logical line segment is rendered as one instanced four-vertex strip instead:
// two vertices at the current endpoint and two at the next endpoint.
// https://developer.apple.com/forums/thread/86098
void main(void) {
    vec2 viewportOrigin = screen.viewport.xy;
    vec2 viewportSize = screen.viewport.zw;

    // Project both segment endpoints.
    vec4 curr = screen.mvp * Vertex;
    vec4 next = screen.mvp * NextVertex;

    // Convert clip space to NDC [-1, 1], then to framebuffer pixels. The
    // fragment shader compares against gl_FragCoord, so these endpoints must be
    // in framebuffer coordinates, including the viewport offset.
    vec2 currNdc = curr.xy / curr.w;
    vec2 nextNdc = next.xy / next.w;
    segmentStart = viewportOrigin + (currNdc * 0.5 + 0.5) * viewportSize;
    segmentEnd = viewportOrigin + (nextNdc * 0.5 + 0.5) * viewportSize;

    // Compute the line normal in framebuffer pixels. At this point pixels are square,
    // so no aspect correction is needed.
    vec2 delta = segmentEnd - segmentStart;
    float len = length(delta);
    vec2 normal = len > 0.0 ? vec2(-delta.y, delta.x) / len : vec2(0.0);

    // Java passes a full width, GLSLLineShader halves it. Convert that NDC half-width
    // to pixels; NDC height 2.0 maps to viewport height in pixels.
    halfWidthPixels = screen.thickness * viewportSize.y * 0.5;

    // Vertex IDs 0/1 use the current endpoint, 2/3 use the next endpoint.
    // Even/odd IDs select the two sides of the expanded rectangle.
    int idx = (gl_VertexID >> 1) & 0x1;
    vec4 pos = idx == 0 ? curr : next;
    vec4 color = idx == 0 ? Color : NextColor;
    float side = dir[gl_VertexID & 0x1];

    // Expand the segment rectangle by the line half-width plus one pixel of AA
    // fringe. This gives the fragment shader real geometry to shade outside
    // the nominal line edge. Multiplying by pos.w converts the NDC offset back
    // to clip space.
    vec2 offsetNdc = 2.0 * normal * (halfWidthPixels + aaPixels) / viewportSize;

    gl_Position = vec4(pos.xy + side * offsetNdc * pos.w, pos.zw);
    fragColor = color;
}
