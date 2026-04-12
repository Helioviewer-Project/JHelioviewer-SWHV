#version 300 es

layout(location = 0) in vec4 Vertex;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec4 NextVertex;
layout(location = 3) in vec4 NextColor;
out vec4 fragColor;

uniform mat4 ModelViewProjectionMatrix;
uniform float iaspect;
uniform float thickness;

const float dir[2] = float[2](1.0, -1.0);

// https://developer.apple.com/forums/thread/86098
void main(void) {
    vec4 curr = ModelViewProjectionMatrix * Vertex;
    vec4 next = ModelViewProjectionMatrix * NextVertex;

    vec2 delta = next.xy - curr.xy;
    float len = length(delta);
    vec2 ortho = len > 0.0 ? vec2(-delta.y * iaspect, delta.x) / len : vec2(0.0);
    vec4 off = vec4(thickness * ortho, 0.0, 0.0);

    int idx = (gl_VertexID >> 1) & 0x1;
    vec4 pos = idx == 0 ? curr : next;
    vec4 color = idx == 0 ? Color : NextColor;

    gl_Position = pos + dir[gl_VertexID & 0x1] * off;
    fragColor = color;
}
