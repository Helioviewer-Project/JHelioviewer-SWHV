#version 330

layout(points) in;
layout(line_strip, max_vertices=128) out;
//in vec2 fragmentUV[];
uniform mat4 mvmmatrix;
uniform sampler3D solarTexture[1];
smooth out vec3 fragmentUVPass;


mat4 projMat(float angle_of_view, float aspect_ratio, float z_near, float z_far) 
{
    return mat4(
        vec4(1.0/tan(angle_of_view),           0.0, 0.0, 0.0),
        vec4(0.0, aspect_ratio/tan(angle_of_view),  0.0, 0.0),
        vec4(0.0, 0.0,    (z_far+z_near)/(z_far-z_near), 1.0),
        vec4(0.0, 0.0, -2.0*z_far*z_near/(z_far-z_near), 0.0)
    );
}

mat4 scale(float x, float y, float z)
{
    return mat4(
        vec4(x,   0.0, 0.0, 0.0),
        vec4(0.0, y,   0.0, 0.0),
        vec4(0.0, 0.0, z,   0.0),
        vec4(0.0, 0.0, 0.0, 1.0)
    );
}

mat4 translate(float x, float y, float z)
{
    return mat4(
        vec4(1.0, 0.0, 0.0, 0.0),
        vec4(0.0, 1.0, 0.0, 0.0),
        vec4(0.0, 0.0, 1.0, 0.0),
        vec4(x,   y,   z,   1.0)
    );
}

mat4 rotate_x(float theta)
{
    return mat4(
        vec4(1.0,         0.0,         0.0, 0.0),
        vec4(0.0,  cos(theta),  sin(theta), 0.0),
        vec4(0.0, -sin(theta),  cos(theta), 0.0),
        vec4(0.0,         0.0,         0.0, 1.0)
    );
}


void main()
{  
    for(int i=0; i<1; i++){
      vec4 pos = gl_in[i].gl_Position;
      for(int j=0; j<128; j++){    
               
              gl_Position = mvmmatrix
                * pos;
              
              //gl_Position = pos;
              fragmentUVPass = pos.xyz;
              pos = pos + (texture(solarTexture[0], pos.xyz))/200.;
              float vv = pos.x+pos.y+pos.z;
              if(vv>0.||vv<1.){
                  EmitVertex();
              }
      }  
  }
        EndPrimitive();      
  
}
