#version 150 core

in vec3 position;
in vec2 coord;

out vec2 texCoord;

uniform mat4 projection;
uniform mat4 view;

void main(void) {
    mat4 ModelViewProjectionMatrix = projection * view;
    gl_Position = ModelViewProjectionMatrix * vec4(position, 1.);
    texCoord = coord;
}
