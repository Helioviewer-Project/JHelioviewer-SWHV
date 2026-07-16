#version 300 es

layout(location = 0) in vec4 Vertex;

out vec2 normalizedScreenpos;

void main(void) {
    gl_Position = Vertex;
    normalizedScreenpos = Vertex.xy;
}
