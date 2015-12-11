#version 110
#define NODIFFERENCE 0
#define RUNNINGDIFFERENCE_NO_ROT 1
#define RUNNINGDIFFERENCE_ROT 2
#define BASEDIFFERENCE_NO_ROT 3
#define BASEDIFFERENCE_ROT 4
#define PI 3.141592654
#define TWOPI 6.2832

uniform sampler2D image;
uniform float truncationValue;
uniform int isdifference;
uniform sampler2D differenceImage;
uniform vec3 pixelSizeWeighting;
//rect=(llx, lly, 1/w, 1/h)
uniform vec4 rect;
uniform vec4 differencerect;
uniform float gamma;
uniform float contrast;
uniform sampler1D lut;
uniform float alpha;
uniform float cutOffRadius;
uniform float outerCutOffRadius;
uniform float hgln;
uniform float hglt;
uniform float unsharpMaskingKernel[9];
uniform mat4 cameraTransformationInverse;
uniform vec4 cameraDifferenceRotationQuat;
uniform vec4 diffcameraDifferenceRotationQuat;
uniform float physicalImageWidth;
uniform vec2 viewport;
uniform vec2 viewportOffset;
uniform vec3 cutOffDirection;
uniform float cutOffValue;

void main(void)
{  
    float tmpConvolutionSum = 0.;
    vec2 scrpos= (gl_FragCoord.xy-viewportOffset)/viewport;
    float scale = 2.*(rect.y + 1.0/rect.w);
    vec2 sphpos = 0.5 + vec2(scrpos.y * cos(scrpos.x*TWOPI), scrpos.y *sin(scrpos.x*TWOPI))/scale;
    vec4 color;
    vec2 texcoord; 
    vec2 difftexcoord; 
    difftexcoord = sphpos;
    texcoord = sphpos;
    color = texture2D(image, texcoord);
    if(isdifference != NODIFFERENCE){
        vec4 diffcolor;
        for(int i=0; i<3; i++)
        {
            for(int j=0; j<3; j++)
            {
                diffcolor.r = texture2D(image, texcoord + vec2(i-1, j-1)*pixelSizeWeighting.xy).r 
                            - texture2D(differenceImage, difftexcoord + vec2(i-1, j-1)*pixelSizeWeighting.xy).r;
                diffcolor.r = clamp(diffcolor.r,-truncationValue,truncationValue)/truncationValue;
                diffcolor.r = (diffcolor.r + 1.0)/2.0;
                tmpConvolutionSum += diffcolor.r * unsharpMaskingKernel[3*i+j]; 
            }
        }
    }
    else{
        for(int i=0; i<3; i++)
        {
            for(int j=0; j<3; j++)
            {
                tmpConvolutionSum += texture2D(image, difftexcoord + vec2(i-1, j-1)*pixelSizeWeighting.xy).r * unsharpMaskingKernel[3*i+j];
            }
        }
    }
    
    color.r = (1. + pixelSizeWeighting.z) * color.r - pixelSizeWeighting.z * tmpConvolutionSum / 16.0;
    color.r = pow(color.r, gamma);
    color.r = 0.5 * sign(2.0 * color.r - 1.0) * pow(abs(2.0 * color.r - 1.0), pow(1.5, -contrast)) + 0.5;
    color.rgb = texture1D(lut, color.r).rgb;
    color.a = alpha;

    //gl_FragData[0] = color;
    gl_FragColor = color;
}
