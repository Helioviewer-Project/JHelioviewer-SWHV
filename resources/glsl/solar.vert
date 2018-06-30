#version 330 core

uniform mat4 ModelViewProjectionMatrix;
uniform int isdisc;

layout(location = 0) in vec3 Vertex;

void main(void) {
    vec4 v = vec4(Vertex, 1);
    gl_Position = isdisc == 1 ? ModelViewProjectionMatrix * v : v;
}
