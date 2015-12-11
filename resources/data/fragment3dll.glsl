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
    vec2 screenpos = gl_FragCoord.xy;
    vec4 color = texture2D(image, screenpos);
    color.a = alpha;
    gl_FragData[0] = color;
}
