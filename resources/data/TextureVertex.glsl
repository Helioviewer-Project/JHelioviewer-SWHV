#version 120
attribute vec3 position;
attribute vec2 coord;
varying vec2 texCoord;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * vec4(position, 1.);
    texCoord = coord;
}
