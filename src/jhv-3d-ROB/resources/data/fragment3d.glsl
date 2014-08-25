#version 330

in vec3 fragmentUVPass;

out vec4 outputColor;

uniform sampler3D solarTexture[1];

void main()
{
     vec4 color = texture(solarTexture[0], clamp(fragmentUVPass, 0., 1.));

     color.g = 1.- clamp((color.x*color.x + color.y*color.y + color.z*color.z)/1.7, 0., 1.);
     if(color.x>0.5){
         color.r = 0;
     }
     else{
         color.b = 0;
     }
     outputColor = color;
}