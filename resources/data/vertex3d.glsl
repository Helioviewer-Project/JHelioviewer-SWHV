#version 110
uniform int isdisc;

void main(void)
{
    vec4 v = gl_Vertex;
    if(isdisc == 1){
        v = gl_ModelViewProjectionMatrix * v;
    }
    gl_Position = v ;
}
