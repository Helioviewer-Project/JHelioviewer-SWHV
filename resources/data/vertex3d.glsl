#version 110
uniform mat4 layerLocalRotation;
void main(void)
{
    vec4 v = gl_Vertex;
//    if(v.z!=0.){
        v = gl_ModelViewProjectionMatrix * (v *layerLocalRotation);
//    }
    gl_Position = v ;
}
