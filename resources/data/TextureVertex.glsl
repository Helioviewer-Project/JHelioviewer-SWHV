#version 120
attribute vec3 position;
attribute vec2 coord;

varying vec2 texCoord;

uniform mat4 ModelViewProjectionMatrix;

void main() {
    gl_Position = ModelViewProjectionMatrix * vec4(position, 1.);
    texCoord = coord;
}
