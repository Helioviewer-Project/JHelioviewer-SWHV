#version 120
attribute vec4 position;
attribute vec4 color;

varying vec4 frag_color;

uniform float factor;

uniform mat4 projection;
uniform mat4 view;

void main(void) {
    mat4 ModelViewProjectionMatrix = projection * view;
    gl_Position = ModelViewProjectionMatrix * position;
    frag_color = color;
}
