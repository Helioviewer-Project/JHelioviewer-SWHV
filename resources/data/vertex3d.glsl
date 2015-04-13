#version 110
uniform mat4 layerLocalRotation;
void main(void)
{
    vec4 v = gl_ModelViewProjectionMatrix * (vec4(gl_Vertex)*layerLocalRotation);
    gl_Position = v ;
}
