#version 330 core

layout(location = 0) in vec4 Vertex;
layout(location = 1) in vec2 Coord;
out vec2 fragCoord;

uniform mat4 ModelViewProjectionMatrix;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * Vertex;
    fragCoord = Coord;
}
