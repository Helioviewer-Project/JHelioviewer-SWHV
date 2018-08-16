#version 140

uniform samplerBuffer attribBuffer;

void main(void) {
    gl_Position = texelFetch(attribBuffer, gl_VertexID);
}
