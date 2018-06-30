#version 330 core

uniform mat4 ModelViewProjectionMatrix;

layout(location = 0) in vec4 Vertex;
layout(location = 1) in vec4 Color;

out vec4 frag_color;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * Vertex;
    frag_color = Color;
}
