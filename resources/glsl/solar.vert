#version 150 core

uniform mat4 ModelViewProjectionMatrix;
uniform int isdisc;

in vec4 position;

void main(void) {
    vec4 v = position;
    if (isdisc == 1) {
        v = ModelViewProjectionMatrix * v;
    }
    gl_Position = v;
}
