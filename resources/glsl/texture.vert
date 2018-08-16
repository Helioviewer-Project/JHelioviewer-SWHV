#version 140

uniform mat4 ModelViewProjectionMatrix;

uniform samplerBuffer vertexBuffer;
uniform samplerBuffer coordBuffer;

out vec2 frag_coord;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * texelFetch(vertexBuffer, gl_VertexID);
    frag_coord = texelFetch(coordBuffer, gl_VertexID).xy;
}
