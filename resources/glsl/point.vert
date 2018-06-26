#version 150 core

in vec4 position;
in vec4 color;

out vec4 frag_color;

uniform float factor;

uniform mat4 projection;
uniform mat4 view;

void main(void) {
    mat4 ModelViewProjectionMatrix = projection * view;
    gl_Position = ModelViewProjectionMatrix * vec4(position.xyz, 1.);
    gl_PointSize = position.w * factor;
    frag_color = color;
}
