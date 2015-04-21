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
uniform vec3 pixelSizeWeighting;
uniform vec4 rect;
uniform vec4 differencerect;
uniform float gamma;
uniform float contrast;
uniform sampler1D lut;
uniform float alpha;
uniform float cutOffRadius;
uniform float outerCutOffRadius;
uniform float phi;
uniform float theta;
uniform mat4 cameraTransformationInverse;
uniform vec4 cameraDifferenceRotationQuat;
uniform vec4 diffcameraDifferenceRotationQuat;
uniform float physicalImageWidth;
uniform vec2 viewport;

vec3 rotate_vector_inverse( vec4 quat, vec3 vec )
{
    return vec + 2.0 * cross( cross( vec, quat.xyz ) + quat.w * vec, quat.xyz );
}
vec3 rotate_vector( vec4 quat, vec3 vec )
{
    return vec + 2.0 * cross( quat.xyz, cross( quat.xyz, vec ) + quat.w * vec );
}

float intersectPlane(vec4 vecin)
{   
    vec3 altnormal = rotate_vector(cameraDifferenceRotationQuat, vec3(0., 0., 1.));
    if(altnormal.z <0.){
        discard;
    }
    return -dot(altnormal.xy,vecin.xy)/altnormal.z;
}
float intersectPlanediff(vec4 vecin)
{   
    vec3 altnormal = rotate_vector(diffcameraDifferenceRotationQuat, vec3(0., 0., 1.));
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
    vec3 rotatedHitPoint = rotate_vector_inverse(cameraDifferenceRotationQuat, hitPoint);
    float radius2 = dot(up1.xy, up1.xy);
    if(radius2 > outerCutOffRadius * outerCutOffRadius || radius2 < cutOffRadius * cutOffRadius) {
        discard;
    }
    if(radius2>=1. || dot(rotatedHitPoint.xyz, vec3(0.,0.,1.))<=0.) {
        hitPoint = vec3(up1.x, up1.y, intersectPlane(up1));
        rotatedHitPoint = rotate_vector_inverse(cameraDifferenceRotationQuat, hitPoint);
    } 
    texcoord = vec2((rotatedHitPoint.x - rect.x) * rect.z, (-rotatedHitPoint.y - rect.y) * rect.w);
    if(texcoord.x<0.||texcoord.y<0.||texcoord.x>1.|| texcoord.y>1.) {
        discard;
    }

    color = texture2D(image, texcoord);
    if(isdifference == BASEDIFFERENCE_NO_ROT || isdifference == RUNNINGDIFFERENCE_NO_ROT) {
        difftexcoord = vec2((rotatedHitPoint.x*differencerect.z - differencerect.x*differencerect.z), (-rotatedHitPoint.y*differencerect.w*1.0-differencerect.y*differencerect.w));
        color.r = color.r - texture2D(differenceImage, difftexcoord).r;
        color.r = clamp(color.r,-truncationValue,truncationValue)/truncationValue;
        color.r = (color.r + 1.0)/2.0;
    } else if(isdifference == BASEDIFFERENCE_ROT || isdifference == RUNNINGDIFFERENCE_ROT) {
        vec3 diffrotatedHitPoint = rotate_vector_inverse(diffcameraDifferenceRotationQuat, hitPoint);
        if(radius2>=1. && dot(diffrotatedHitPoint.xyz, vec3(0.,0.,1.))<=0.) {
            hitPoint = vec3(up1.x, up1.y, intersectPlanediff(up1));
            diffrotatedHitPoint = rotate_vector_inverse(diffcameraDifferenceRotationQuat, hitPoint);
        } 
        difftexcoord = vec2((diffrotatedHitPoint.x - differencerect.x) * differencerect.z, (-diffrotatedHitPoint.y - differencerect.y ) * differencerect.w);
        color.r = color.r - texture2D(differenceImage, difftexcoord).r;
        color.r = clamp(color.r,-truncationValue,truncationValue)/truncationValue;
        color.r = (color.r + 1.0)/2.0;
    }
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
                tmpConvolutionSum += texture2D(image, texcoord.xy + vec2(i-1, j-1)*pixelSizeWeighting.xy).r * unsharpMaskingKernel[3*i+j];
            }
        }
    }

    color.r = (1. + pixelSizeWeighting.z) * color.r - pixelSizeWeighting.z * tmpConvolutionSum / 16.0;
    color.r = pow(color.r, gamma);
    color.r = 0.5 * sign(2.0 * color.r - 1.0) * pow(abs(2.0 * color.r - 1.0), pow(1.5, -contrast)) + 0.5;
    color.rgb = texture1D(lut, color.r).rgb;
    color.a = alpha;
    gl_FragColor = color;
}
