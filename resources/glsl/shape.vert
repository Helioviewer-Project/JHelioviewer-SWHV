#version 330 core

uniform mat4 ModelViewProjectionMatrix;

layout(location = 0) in vec4 Vertex;
layout(location = 1) in vec4 Color;

out vec4 fragColor;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * Vertex;
    fragColor = Color;
}
