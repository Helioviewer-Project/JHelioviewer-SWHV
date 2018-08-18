#version 330 core

uniform mat4 ModelViewProjectionMatrix;

layout(location = 0) in vec4 Vertex;
layout(location = 1) in vec2 Coord;

out vec2 fragCoord;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * Vertex;
    fragCoord = Coord;
}
