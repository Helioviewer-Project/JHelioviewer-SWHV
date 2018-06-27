#version 150 core

uniform mat4 ModelViewProjectionMatrix;

in vec3 position;
in vec2 coord;

out vec2 texCoord;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * vec4(position, 1.);
    texCoord = coord;
}
