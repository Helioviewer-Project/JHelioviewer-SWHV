#version 330 core

/* \brief Fragment GLSL shader that demonstrates how perform pass through fragment shader.
 * This file is a part of shader-3dcurve example (https://github.com/vicrucann/shader-3dcurve).

 * \author Victoria Rudakova
 * \date January 2017
 * \copyright MIT license
*/

out vec4 FragColor;

in VertexData{
    vec2 mTexCoord;
    vec4 mColor;
} VertexIn;

void main(void)
{
    if (VertexIn.mColor.a == 0.)
        discard;
    FragColor = VertexIn.mColor;
}
