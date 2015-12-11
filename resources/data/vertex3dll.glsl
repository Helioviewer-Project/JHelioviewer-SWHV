#version 110
uniform int isdisc;

void main(void)
{
    vec4 v = gl_Vertex;
    gl_Position = v ;
}
