#version 330 core

layout(location = 0) in vec4 Vertex;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec4 NextVertex;
layout(location = 3) in vec4 NextColor;
out vec4 fragColor;

uniform mat4 ModelViewProjectionMatrix;
uniform float iaspect;
uniform float thickness;

const float[] dir = float[](1, -1);
vec4[2] pos, col;

// https://developer.apple.com/forums/thread/86098
void main(void) {
/*  if (Vertex == NextVertex) {
        gl_Position = ModelViewProjectionMatrix * Vertex;
        fragColor = Color;
        return;
    } */

    vec4 curr = ModelViewProjectionMatrix * Vertex;
    vec4 next = ModelViewProjectionMatrix * NextVertex;

    vec4 d = normalize(next - curr);
    vec4 off = thickness * vec4(-d.y * iaspect, d.x, 0, 0);

    pos[0] = curr;
    pos[1] = next;

    col[0] = Color;
    col[1] = NextColor;

    int idx = (gl_VertexID >> 1) & 0x1;
    gl_Position = pos[idx] + dir[gl_VertexID & 0x1] * off;
    fragColor = col[idx];
}
