#version 110

uniform sampler2D image;
uniform float truncationValue;
uniform float isdifference;
uniform sampler2D differenceImage;
uniform vec4 pixelSizeWeighting;
uniform float gamma;
uniform float contrast;
uniform sampler1D lut;
uniform float alpha;
uniform vec4 cutOffRadius;
uniform vec4 outerCutOffRadius;
uniform float phi;
uniform float theta;
varying vec4 outPosition;

void main()
{
    vec4 color;
    float _tmpConvolutionSum;
    float unsharpMaskingKernel[9];
    unsharpMaskingKernel[0] = 1.;
    unsharpMaskingKernel[1] = 2.;
    unsharpMaskingKernel[2] = 1.;
    unsharpMaskingKernel[3] = 2.;
    unsharpMaskingKernel[4] = 4.;
    unsharpMaskingKernel[5] = 2.;
    unsharpMaskingKernel[6] = 1.;
    unsharpMaskingKernel[7] = 2.;
    unsharpMaskingKernel[8] = 1.;

    float tmpConvolutionSum = 0.;
    color = texture2D(image, gl_TexCoord[0].xy);
    if(isdifference>0.24 && isdifference<0.27){
        color.r = color.r - texture2D(differenceImage, gl_TexCoord[4].xy).r;
        color.r = clamp(color.r,-truncationValue,truncationValue)/truncationValue;
        color.r = (color.r + 1.0)/2.0;
    } else if(isdifference>0.98 && isdifference<1.01){
        color.r = color.r - texture2D(differenceImage, gl_TexCoord[4].xy).r;
        color.r = clamp(color.r,-truncationValue,truncationValue)/truncationValue;
        color.r = (color.r + 1.0)/2.0;
    }
    for(int i=0; i<3; i++)
    {
        for(int j=0; j<3; j++)
        {
            tmpConvolutionSum += texture2D(image, gl_TexCoord[0].xy + vec2(i-1, j-1)*pixelSizeWeighting.x).r * unsharpMaskingKernel[3*i+j];
        }
    }
    tmpConvolutionSum = (1. + pixelSizeWeighting.z) * color.r - pixelSizeWeighting.z * tmpConvolutionSum / 16.0;
    color.r = tmpConvolutionSum;
    color.r = pow(color.r, gamma);
    color.r = 0.5 * sign(2.0 * color.r - 1.0) * pow(abs(2.0 * color.r - 1.0), pow(1.5, -contrast)) + 0.5;
    color.rgb = texture1D(lut, color.r).rgb;
    color.a = alpha;
    if(gl_TexCoord[0].x<0.||gl_TexCoord[0].y<0.||gl_TexCoord[0].x>1.|| gl_TexCoord[0].y>1.){
        discard;
    }
    float dotpos = dot(gl_TexCoord[3].xyz, gl_TexCoord[3].xyz);
    mat3 mat = mat3( cos(phi), -sin(theta)*sin(phi), -cos(theta)*sin(phi), 
                             0.,       cos(theta),           -sin(theta), 
                             sin(phi), cos(phi)*sin(theta),  cos(theta)*cos(phi));
    vec3 zaxisrot = mat * vec3(0.,0.,1.);
    float projectionn = dot(gl_TexCoord[3].xyz, zaxisrot);
    if((gl_TexCoord[3].z!=0.0 && projectionn < -0.0)){     
        discard;
    }
    gl_FragColor = color;
    return;
}
