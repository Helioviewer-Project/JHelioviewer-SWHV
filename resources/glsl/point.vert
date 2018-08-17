#version 140

uniform mat4 ModelViewProjectionMatrix;
uniform float factor;

uniform samplerBuffer vertexBuffer;
uniform samplerBuffer colorBuffer;

out vec4 fragColor;

void main(void) {
    vec4 texel = texelFetch(vertexBuffer, gl_VertexID);
    gl_Position = ModelViewProjectionMatrix * vec4(texel.xyz, 1.);
    gl_PointSize = texel.w * factor;
    fragColor = texelFetch(colorBuffer, gl_VertexID);
}
