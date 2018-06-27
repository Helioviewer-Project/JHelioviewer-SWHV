#version 150 core

uniform mat4 ModelViewProjectionMatrix;
uniform float factor;

in vec4 position;
in vec4 color;

out vec4 frag_color;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * vec4(position.xyz, 1.);
    gl_PointSize = position.w * factor;
    frag_color = color;
}
