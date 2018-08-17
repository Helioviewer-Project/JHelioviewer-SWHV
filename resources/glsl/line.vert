#version 140

out vec4 fragColor;

uniform float thickness;
uniform vec4 viewport;

uniform mat4 ModelViewProjectionMatrix;

uniform samplerBuffer vertexBuffer;
uniform samplerBuffer colorBuffer;

// https://forums.developer.apple.com/thread/86098
void main(void) {
    vec4 cpos = ModelViewProjectionMatrix * texelFetch(vertexBuffer, gl_InstanceID);
    vec4 npos = ModelViewProjectionMatrix * texelFetch(vertexBuffer, gl_InstanceID + 1);

    vec4 ccol = texelFetch(colorBuffer, gl_InstanceID);
    vec4 ncol = texelFetch(colorBuffer, gl_InstanceID + 1);

    if (cpos == npos) {
        gl_Position = cpos;
        fragColor = ccol;
        return;
    }

    cpos *= viewport;
    npos *= viewport;

    vec4 v0 = thickness * normalize(npos - cpos);
    vec4 v1 = vec4(-v0.y, v0.x, 0, 0);

    vec4 pos[4];
    pos[0] = cpos + v1;
    pos[1] = cpos - v1;
    pos[2] = npos + v1;
    pos[3] = npos - v1;

    vec4 col[4];
    col[0] = ccol;
    col[1] = ccol;
    col[2] = ncol;
    col[3] = ncol;

    int idx = gl_VertexID & 0x3;
    gl_Position = pos[idx] / viewport;
    fragColor = col[idx];
}
