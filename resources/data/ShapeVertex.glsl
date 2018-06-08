#version 120
attribute vec4 position;
attribute vec4 color;

varying vec4 frag_color;

uniform float factor;
uniform mat4 ModelViewProjectionMatrix;

void main() {
    gl_Position = ModelViewProjectionMatrix * position;
    frag_color = color;
}
