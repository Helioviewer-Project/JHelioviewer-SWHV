#version 330 core

/* \brief Vertex GLSL shader that demonstrates how to draw basic thick and smooth lines in 3D.
 * This file is a part of shader-3dcurve example (https://github.com/vicrucann/shader-3dcurve).
 *
 * \author Victoria Rudakova
 * \date January 2017
 * \copyright MIT license
*/

uniform mat4 projection;
uniform mat4 view;

layout(location = 0) in vec4 Vertex;
layout(location = 1) in vec4 Color;

out VertexData{
    vec4 mColor;
} VertexOut;

void main(void)
{
    VertexOut.mColor = Color;

    mat4 ModelViewProjectionMatrix = projection * view;
    gl_Position = ModelViewProjectionMatrix * Vertex;
}
