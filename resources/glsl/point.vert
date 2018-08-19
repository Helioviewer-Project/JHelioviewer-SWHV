#version 140

in vec4 Vertex;
in vec4 Color;
out vec4 fragColor;

uniform mat4 ModelViewProjectionMatrix;
uniform float factor;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * vec4(Vertex.xyz, 1.);
    gl_PointSize = Vertex.w * factor;
    fragColor = Color;
}
