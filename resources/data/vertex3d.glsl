#version 110
uniform mat4 layerLocalRotation;
uniform int isdisc;

void main(void)
{
    vec4 v = gl_Vertex;
    if(isdisc == 1){
        v = gl_ModelViewProjectionMatrix * (v *layerLocalRotation);
    }
    gl_Position = v ;
}
