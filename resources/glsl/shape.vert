#version 150 core

uniform mat4 ModelViewProjectionMatrix;
uniform float factor;

in vec4 position;
in vec4 color;

out vec4 frag_color;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * position;
    frag_color = color;
}
