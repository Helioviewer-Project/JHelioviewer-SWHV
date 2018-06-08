#version 120
attribute vec4 position;
attribute vec4 color;

varying vec4 frag_color;

uniform float factor;
uniform mat4 ModelViewProjectionMatrix;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * vec4(position.xyz, 1.);
    gl_PointSize = position.w * factor;
    frag_color = color;
}
