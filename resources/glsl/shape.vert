#version 140

uniform mat4 ModelViewProjectionMatrix;

uniform samplerBuffer vertexBuffer;
uniform samplerBuffer colorBuffer;

out vec4 fragColor;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * texelFetch(vertexBuffer, gl_VertexID);
    fragColor = texelFetch(colorBuffer, gl_VertexID);
}
