#version 120
attribute vec4 position;

uniform int isdisc;
uniform mat4 ModelViewProjectionMatrix;

void main(void) {
    vec4 v = position;
    if (isdisc == 1) {
        v = ModelViewProjectionMatrix * v;
    }
    gl_Position = v;
}