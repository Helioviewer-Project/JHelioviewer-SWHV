#version 110
void main(void)
{
    vec4 v = vec4(gl_Vertex);
    v.z = 0.;
    v.w = 1.;
    gl_Position = v;//gl_ModelViewProjectionMatrix * v;
    gl_TexCoord[0] = gl_MultiTexCoord0;
}
