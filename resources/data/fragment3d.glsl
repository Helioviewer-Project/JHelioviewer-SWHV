#version 110
#define NODIFFERENCE 0
#define RUNNINGDIFFERENCE_NO_ROT 1
#define RUNNINGDIFFERENCE_ROT 2
#define BASEDIFFERENCE_NO_ROT 3
#define BASEDIFFERENCE_ROT 4

uniform sampler2D image;
uniform float truncationValue;
uniform int isdifference;
uniform sampler2D differenceImage;
uniform vec4 pixelSizeWeighting;
uniform vec4 rect;
uniform vec4 differencerect;
uniform float gamma;
uniform float contrast;
uniform sampler1D lut;
uniform float alpha;
uniform vec4 cutOffRadius;
uniform vec4 outerCutOffRadius;
uniform float phi;
uniform float theta;
uniform mat4 cameraTransformationInverse;
uniform mat4 cameraDifferenceRotation;
uniform mat4 diffcameraDifferenceRotation;
uniform float physicalImageWidth;
uniform vec2 viewport;

float intersectPlane(vec4 vecin)
{   
    vec3 altnormal = (cameraDifferenceRotation * vec4(0., 0., 1., 1.)).xyz;
    if(altnormal.z <0.){
        discard;
    }
    return -dot(altnormal.xy,vecin.xy)/altnormal.z;
}
float intersectPlanediff(vec4 vecin)
{   
    vec3 altnormal = (diffcameraDifferenceRotation * vec4(0., 0., 1., 1.)).xyz;
    if(altnormal.z <0.){
        discard;
    }
    return -dot(altnormal.xy,vecin.xy)/altnormal.z;
}

void main(void)
{  
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
    vec2 normalizedScreenpos = 2.*((gl_FragCoord.xy/viewport)-0.5);
    vec4 up1 =  cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);
  
    vec4 color;
    vec2 texcoord; 
    vec2 difftexcoord; 
    vec3 hitPoint = vec3(up1.x, up1.y, sqrt(1.-dot(up1.xy, up1.xy)));
    vec4 rotatedHitPoint = vec4(hitPoint.x, hitPoint.y, hitPoint.z, 1.) * cameraDifferenceRotation;
    float radius2 = dot(up1.xy, up1.xy);
    if(radius2 > outerCutOffRadius.x * outerCutOffRadius.x || radius2 < cutOffRadius.x * cutOffRadius.x) {
        discard;
    }
    if(radius2<1. && dot(rotatedHitPoint.xyz, vec3(0.,0.,1.))>0.) {
        texcoord = vec2((rotatedHitPoint.x*rect.z - rect.x*rect.z), (rotatedHitPoint.y*rect.w*1.0-rect.y*rect.w));
    }
    else{
        hitPoint = vec3(up1.x, up1.y, intersectPlane(up1));
        rotatedHitPoint = vec4(hitPoint.x, hitPoint.y, hitPoint.z, 1.) *cameraDifferenceRotation;
        texcoord = vec2((rotatedHitPoint.x * rect.z - rect.x*rect.z), (rotatedHitPoint.y*rect.w*1.0-rect.y*rect.w));
    } 
    if(texcoord.x<0.||texcoord.y<0.||texcoord.x>1.|| texcoord.y>1.) {
        discard;
    }

    color = texture2D(image, texcoord);
    if(isdifference == BASEDIFFERENCE_NO_ROT || isdifference == RUNNINGDIFFERENCE_NO_ROT) {
        difftexcoord = vec2((rotatedHitPoint.x*differencerect.z - differencerect.x*differencerect.z), (rotatedHitPoint.y*differencerect.w*1.0-differencerect.y*differencerect.w));
        color.r = color.r - texture2D(differenceImage, difftexcoord).r;
        color.r = clamp(color.r,-truncationValue,truncationValue)/truncationValue;
        color.r = (color.r + 1.0)/2.0;
    } else if(isdifference == BASEDIFFERENCE_ROT || isdifference == RUNNINGDIFFERENCE_ROT) {
        vec4 diffrotatedHitPoint = vec4(hitPoint.x, hitPoint.y, hitPoint.z, 1.) * diffcameraDifferenceRotation;
        if(radius2<1. && dot(diffrotatedHitPoint.xyz, vec3(0.,0.,1.))>0.) {
            difftexcoord = vec2((diffrotatedHitPoint.x*differencerect.z - differencerect.x*differencerect.z), (diffrotatedHitPoint.y*differencerect.w*1.0-differencerect.y*differencerect.w));
        }
        else{
            hitPoint = vec3(up1.x, up1.y, intersectPlanediff(up1));
            diffrotatedHitPoint = vec4(hitPoint.x, hitPoint.y, hitPoint.z, 1.) *diffcameraDifferenceRotation;
            difftexcoord = vec2((diffrotatedHitPoint.x*differencerect.z - differencerect.x*differencerect.z), (diffrotatedHitPoint.y*differencerect.w*1.0-differencerect.y*differencerect.w));
        } 
        color.r = color.r - texture2D(differenceImage, difftexcoord).r;
        color.r = clamp(color.r,-truncationValue,truncationValue)/truncationValue;
        color.r = (color.r + 1.0)/2.0;
    }

    for(int i=0; i<3; i++)
    {
        for(int j=0; j<3; j++)
        {
            tmpConvolutionSum += texture2D(image, texcoord.xy + vec2(i-1, j-1)*pixelSizeWeighting.x).r * unsharpMaskingKernel[3*i+j];
        }
    }
    color.r = (1. + pixelSizeWeighting.z) * color.r - pixelSizeWeighting.z * tmpConvolutionSum / 16.0;
    color.r = pow(color.r, gamma);
    color.r = 0.5 * sign(2.0 * color.r - 1.0) * pow(abs(2.0 * color.r - 1.0), pow(1.5, -contrast)) + 0.5;
    color.rgb = texture1D(lut, color.r).rgb;
    color.a = alpha;
    gl_FragColor = color;
}
