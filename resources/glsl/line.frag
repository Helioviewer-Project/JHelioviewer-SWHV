#version 330 core

layout(location = 0) out vec4 FragColor;

in vec4 fragColor;

void main(void) {
    if (fragColor.a == 0.)
        discard;
    FragColor = fragColor;
}
