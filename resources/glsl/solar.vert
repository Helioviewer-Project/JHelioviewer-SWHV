#version 150 core

in vec4 position;

uniform int isdisc;

uniform mat4 projection;
uniform mat4 view;

void main(void) {
    vec4 v = position;
    if (isdisc == 1) {
        mat4 ModelViewProjectionMatrix = projection * view;
        v = ModelViewProjectionMatrix * v;
    }
    gl_Position = v;
}
