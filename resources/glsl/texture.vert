#version 330 core

uniform mat4 ModelViewProjectionMatrix;

layout(location = 0) in vec3 Vertex;
layout(location = 1) in vec2 Coord;

out vec2 frag_coord;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * vec4(Vertex, 1.);
    frag_coord = Coord;
}
