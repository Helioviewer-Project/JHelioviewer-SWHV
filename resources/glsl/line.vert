#version 140

out vec4 frag_color;

uniform float thickness;
uniform vec2 viewport;

uniform mat4 ModelViewProjectionMatrix;

uniform samplerBuffer vertexBuffer;
uniform samplerBuffer colorBuffer;

void main(void) {
    vec4 current = ModelViewProjectionMatrix * texelFetch(vertexBuffer, gl_InstanceID);
    vec4 next = ModelViewProjectionMatrix * texelFetch(vertexBuffer, gl_InstanceID + 1);

    vec4 v = next - current;
    vec2 p0 = current.xy;
    vec2 v0 = v.xy;
    vec2 v1 = thickness * normalize(v0) * mat2(0, -1, 1, 0);

    vec2 pos[4];
    pos[0] = p0 + v1;
    pos[1] = p0 - v1;
    pos[2] = pos[0] + v0;
    pos[3] = pos[1] + v0;

    gl_Position = vec4(pos[gl_VertexID & 0x3], current.z, 1);
    frag_color = texelFetch(colorBuffer, gl_InstanceID);
}
