#version 140

uniform samplerBuffer vertexBuffer;

void main(void) {
    gl_Position = texelFetch(vertexBuffer, gl_VertexID);
}
