#version 330 core

layout(location = 0) in vec4 Vertex;

void main(void) {
    gl_Position = Vertex;
}
