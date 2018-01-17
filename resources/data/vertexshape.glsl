#version 120
attribute vec4 position;
attribute vec4 color;
varying vec4 frag_color;

uniform float factor;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * position;
    frag_color = color;
}
