#version 140

uniform mat4 ModelViewProjectionMatrix;
uniform float factor;

in vec4 Vertex;
in vec4 Color;

out vec4 frag_color;

void main(void) {
    gl_Position = ModelViewProjectionMatrix * vec4(Vertex.xyz, 1.);
    gl_PointSize = Vertex.w * factor;
    frag_color = Color;
}
