#version 140

in vec4 Vertex;
in vec2 Coord;
out vec2 fragCoord;

uniform mat4 ModelViewProjectionMatrix;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * Vertex;
    fragCoord = Coord;
}
