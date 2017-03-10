#version 110
#define NODIFFERENCE 0
#define RUNNINGDIFFERENCE_NO_ROT 1
#define RUNNINGDIFFERENCE_ROT 2
#define BASEDIFFERENCE_NO_ROT 3
#define BASEDIFFERENCE_ROT 4
#define PI 3.1415926535897932384626433832795
#define TWOPI 2.*3.1415926535897932384626433832795

#define BOOST 1. / (0.2 * 2.)
#define FSIZE 5
#define FDISP (FSIZE - 1) / 2

uniform sampler2D image;
uniform int isdifference;
uniform int enhanced;
uniform sampler2D differenceImage;
uniform vec3 pixelSizeWeighting;
//rect=(llx, lly, 1/w, 1/h)
uniform vec4 rect;
uniform vec4 differencerect;
uniform sampler1D lut;
uniform vec2 brightness;
uniform float alpha;
uniform float cutOffRadius;
uniform float outerCutOffRadius;
uniform float hgln;
uniform float hglt;
uniform float unsharpMaskingKernel[FSIZE * FSIZE];
uniform mat4 cameraTransformationInverse;
uniform vec4 cameraDifferenceRotationQuat;
uniform vec4 diffcameraDifferenceRotationQuat;
uniform vec2 viewport;
uniform vec2 viewportOffset;
uniform vec3 cutOffDirection;
uniform float cutOffValue;
uniform vec2 polarRadii;

vec4 getColor(vec2 texcoord, vec2 difftexcoord, float factor) {
    float convolution = 0.;
    vec4 color = texture2D(image, texcoord);

    if (isdifference != NODIFFERENCE) {
        color.r = color.r - texture2D(differenceImage, difftexcoord).r;
        vec4 diffcolor;
        for (int i = 0; i < FSIZE; i++) {
            for (int j = 0; j < FSIZE; j++) {
                diffcolor.r = texture2D(image, texcoord + vec2(i - FDISP, j - FDISP) * pixelSizeWeighting.xy).r
                            - texture2D(differenceImage, difftexcoord + vec2(i - FDISP, j - FDISP) * pixelSizeWeighting.xy).r;
                convolution += diffcolor.r * unsharpMaskingKernel[FSIZE * i + j];
            }
        }
        color.r = color.r * BOOST + 0.5;
        convolution = convolution * BOOST + 0.5;
    } else {
        for (int i = 0; i < FSIZE; i++) {
            for (int j = 0; j < FSIZE; j++) {
                convolution += texture2D(image, texcoord + vec2(i - FDISP, j - FDISP) * pixelSizeWeighting.xy).r * unsharpMaskingKernel[FSIZE * i + j];
            }
        }
    }
    color.r = (1. + pixelSizeWeighting.z) * color.r - pixelSizeWeighting.z * convolution;

    float scale = brightness.y;
    if (enhanced == 1) {
        scale *= factor;
    }
    color.r = scale * color.r + brightness.x;

    color.rgb = texture1D(lut, color.r).rgb;
    color.a = alpha;
    return color;
}

void clamp_texcoord(vec2 texcoord) {
    if (texcoord.x < 0. || texcoord.y < 0. || texcoord.x > 1. || texcoord.y > 1.)
        discard;
}

vec3 rotate_vector_inverse(vec4 quat, vec3 vec) {
    return vec + 2.0 * cross(cross(vec, quat.xyz) + quat.w * vec, quat.xyz);
}

vec3 rotate_vector(vec4 quat, vec3 vec) {
    return vec + 2.0 * cross(quat.xyz, cross(quat.xyz, vec) + quat.w * vec);
}

float intersectPlane(vec4 vecin) {
    vec3 altnormal = rotate_vector(cameraDifferenceRotationQuat, vec3(0., 0., 1.));
    if (altnormal.z < 0.) {
        discard;
    }
    return -dot(altnormal.xy, vecin.xy) / altnormal.z;
}

float intersectPlanediff(vec4 vecin) {
    vec3 altnormal = rotate_vector(diffcameraDifferenceRotationQuat, vec3(0., 0., 1.));
    if (altnormal.z < 0.) {
        discard;
    }
    return -dot(altnormal.xy, vecin.xy) / altnormal.z;
}
