#version 140

uniform mat4 ModelViewProjectionMatrix;

in vec4 Vertex;
in vec4 Color;

out vec4 frag_color;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * Vertex;
    frag_color = Color;
}
