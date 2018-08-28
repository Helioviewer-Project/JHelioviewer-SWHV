#version 140

in vec4 Vertex;
in vec4 Color;
in vec4 NextVertex;
in vec4 NextColor;
out vec4 fragColor;

uniform float iaspect;
uniform float thickness;

uniform mat4 ModelViewProjectionMatrix;

// https://forums.developer.apple.com/thread/86098
void main(void) {
    if (Vertex == NextVertex) {
        gl_Position = ModelViewProjectionMatrix * Vertex;
        fragColor = Color;
        return;
    }

    vec4 cpos = ModelViewProjectionMatrix * Vertex;
    vec4 npos = ModelViewProjectionMatrix * NextVertex;

    vec4 v0 = thickness * normalize(npos - cpos);
    vec4 v1 = vec4(-v0.y * iaspect, v0.x, 0, 0);

    vec4 pos[4];
    pos[0] = cpos + v1;
    pos[1] = cpos - v1;
    pos[2] = npos + v1;
    pos[3] = npos - v1;

    vec4 col[4];
    col[0] = Color;
    col[1] = Color;
    col[2] = NextColor;
    col[3] = NextColor;

    int idx = gl_VertexID; // & 0x3
    gl_Position = pos[idx];
    fragColor = col[idx];
}
