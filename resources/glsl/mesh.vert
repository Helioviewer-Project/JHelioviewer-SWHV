#version 300 es

layout(location = 0) in vec3 Vertex;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 TexCoord;

out vec4 vertexColor;
out vec2 texCoord;

uniform mat4 ModelViewProjectionMatrix;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * vec4(Vertex, 1.);
    vertexColor = Color;
    texCoord = TexCoord;
}
