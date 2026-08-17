#version 300 es

layout(location = 0) in vec3 Vertex;

out vec3 texturePosition;

uniform mat4 ModelViewProjectionMatrix;
uniform vec3 corner;
uniform vec3 axisX;
uniform vec3 axisY;
uniform vec3 axisZ;

void main(void) {
    texturePosition = Vertex;
    vec3 worldPosition = corner + Vertex.x * axisX + Vertex.y * axisY + Vertex.z * axisZ;
    gl_Position = ModelViewProjectionMatrix * vec4(worldPosition, 1.0);
}
