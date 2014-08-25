#version 330

in vec2 fragmentUV;

out vec4 outputColor;

uniform sampler2D solarTexture[10];
uniform sampler1D lut[10];
float kernel[9];
float step_w = 1.0/1024.0;
float step_h = 1.0/1024.0;
vec2 offset[9];


void main()
{
    vec3 color_s = vec3(texture(solarTexture[1], fragmentUV));
    vec3 color_w = vec3(texture(solarTexture[0], fragmentUV));

    vec3 sharpenColor = mix(color_s, color_w, 0.5);
    outputColor.rgb = sharpenColor;
    
    outputColor.rgba = clamp(texture(lut[0], outputColor.r), 0.0f, 1.0f);
    outputColor.a = 1.0f;
}


