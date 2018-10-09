#version 140

in vec4 Vertex;
in vec4 Color;
in vec4 NextVertex;
in vec4 NextColor;
out vec4 fragColor;

uniform float aspect;
uniform float thickness;

uniform mat4 ModelViewProjectionMatrix;

// https://forums.developer.apple.com/thread/86098
void main(void) {
    vec4 curr = ModelViewProjectionMatrix * Vertex;
    vec4 next = ModelViewProjectionMatrix * NextVertex;

    if (curr.xy == next.xy) {
        gl_Position = curr;
        fragColor = Color;
        return;
    }

    vec2 d = normalize((next - curr).xy * vec2(1 / aspect, 1));
    vec4 off = thickness * vec4(-d.y, d.x * aspect, 0, 0);

    float dir[2];
    dir[0] = 1;
    dir[1] = -1;

    vec4 pos[2];
    pos[0] = curr;
    pos[1] = next;

    vec4 col[2];
    col[0] = Color;
    col[1] = NextColor;

    int idx = (gl_VertexID >> 1) & 0x1;
    gl_Position = pos[idx] + dir[gl_VertexID & 0x1] * off;
    fragColor = col[idx];
}
