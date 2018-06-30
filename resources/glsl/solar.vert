#version 330 core

uniform mat4 ModelViewProjectionMatrix;
uniform int isdisc;

layout(location = 0) in vec4 Vertex;

void main(void) {
    gl_Position = isdisc == 1 ? ModelViewProjectionMatrix * Vertex : Vertex;
}
