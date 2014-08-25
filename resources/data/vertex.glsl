#version 330

layout (location = 0) in vec4 position;
layout (location = 1) in vec2 vertexUV;
uniform mat4 mvmmatrix;


out vec2 fragmentUV;

void main()
{
    gl_Position = mvmmatrix*position;   
    fragmentUV = vertexUV;
}

