#version 110
attribute vec4 position;

uniform int isdisc;

void main(void)
{
    vec4 v = position;
    if(isdisc == 1){
        v = gl_ModelViewProjectionMatrix * v;
    }
    gl_Position = v ;
}
