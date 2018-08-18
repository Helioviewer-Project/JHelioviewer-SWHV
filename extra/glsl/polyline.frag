#version 330 core

precision mediump float;

/* \brief Fragment GLSL shader that demonstrates how perform pass through fragment shader.
 * This file is a part of shader-3dcurve example (https://github.com/vicrucann/shader-3dcurve).

 * \author Victoria Rudakova
 * \date January 2017
 * \copyright MIT license
*/

layout(location = 0) out vec4 FragColor;

in VertexData {
//    vec2 mTexCoord;
    vec4 mColor;
} VertexIn;

void main(void) {
    FragColor = VertexIn.mColor;
}
