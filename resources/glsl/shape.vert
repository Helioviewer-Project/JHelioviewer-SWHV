#version 140

in vec4 Vertex;
in vec4 Color;
out vec4 fragColor;

uniform mat4 ModelViewProjectionMatrix;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * Vertex;
    fragColor = Color;
}
