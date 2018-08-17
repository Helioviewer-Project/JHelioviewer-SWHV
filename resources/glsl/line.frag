#version 140

in vec4 fragColor;
out vec4 FragColor;

void main(void) {
    if (fragColor.a == 0.)
        discard;
    FragColor = fragColor;
}
