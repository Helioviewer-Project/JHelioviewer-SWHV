#version 140

uniform mat4 ModelViewProjectionMatrix;

in vec4 Vertex;
in vec2 Coord;

out vec2 frag_coord;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * Vertex;
    frag_coord = Coord;
}
