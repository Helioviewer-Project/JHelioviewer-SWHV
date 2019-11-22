#version 330 core

layout(location = 0) in vec4 Vertex;
layout(location = 1) in vec4 Color;
out vec4 fragColor;

uniform mat4 ModelViewProjectionMatrix;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * Vertex;
    fragColor = Color;
}
