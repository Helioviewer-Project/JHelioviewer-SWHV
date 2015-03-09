#version 110

struct outputStruct {
    vec4 _color2;
    vec4 _texcoord02;
    vec4 _position2;
    vec4 _texcoord4;
    vec4 _texcoord3;
};

uniform vec4 rect;
uniform float theta;
uniform float phi;
uniform float differencetheta;
uniform float differencephi;
uniform vec4 differenceRect;
varying vec4 outPosition;

 // main procedure, the original name was main
void main()
{
    gl_TexCoord[0] = vec4(gl_TexCoord[0].xy, 0, 1);
    outPosition = gl_ModelViewProjectionMatrix * gl_Vertex;

    if(abs(gl_Vertex.x)>1.1){
        gl_TexCoord[0].x = gl_Vertex.x - rect.x;
        gl_TexCoord[0].y = -gl_Vertex.y - rect.y;
        gl_TexCoord[0].x *= rect.z;
        gl_TexCoord[0].y *= rect.w;
        gl_TexCoord[4].x = gl_Vertex.x - differenceRect.x;
        gl_TexCoord[4].y = -gl_Vertex.y - differenceRect.y;
        gl_TexCoord[4].x *= differenceRect.z;
        gl_TexCoord[4].y *= differenceRect.w;
        gl_TexCoord[3] = gl_Vertex;
        mat3 mat = mat3(cos(phi), -sin(theta)*sin(phi),  -sin(phi)*cos(theta), 
                                0,        cos(theta),            -sin(theta), 
                                sin(phi), cos(phi)*sin(theta),   cos(theta)*cos(phi));
        outPosition.xyz = mat * gl_Vertex.xyz;
        outPosition = gl_ModelViewProjectionMatrix * outPosition;
    }
    else{
        gl_TexCoord[3] = gl_Vertex;
        mat3 mat = mat3(cos(phi),             0,          sin(phi), 
                            -sin(theta)*sin(phi), cos(theta), sin(theta)*cos(phi), 
                            -cos(theta)*sin(phi), -sin(theta), cos(theta)*cos(phi));
        vec3 rot = mat * gl_Vertex.xyz;
        gl_TexCoord[0].x = rot.x - rect.x;
        gl_TexCoord[0].y = -rot.y - rect.y;
        gl_TexCoord[0].x *= rect.z;
        gl_TexCoord[0].y *= rect.w;
        mat = mat3(cos(differencephi),                       0,                     sin(differencephi), 
                       -sin(differencetheta)*sin(differencephi), cos(differencetheta),  sin(differencetheta)*cos(differencephi), 
                       -cos(differencetheta)*sin(differencephi), -sin(differencetheta), cos(differencetheta)*cos(differencephi));
        rot = mat * gl_Vertex.xyz;
        gl_TexCoord[4].x = rot.x - differenceRect.x;
        gl_TexCoord[4].y = -rot.y - differenceRect.y;
        gl_TexCoord[4].x *= differenceRect.z;
        gl_TexCoord[4].y *= differenceRect.w;
    }
    gl_Position = outPosition;
    return;
}
