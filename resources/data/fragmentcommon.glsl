#version 120
#define NODIFFERENCE 0
#define PI 3.1415926535897932384626433832795
#define TWOPI 2.*3.1415926535897932384626433832795

#define BOOST 1. / (0.2 * 2.)
#define FSIZE 3

uniform sampler2D image;
uniform int isdifference;
uniform int enhanced;
uniform sampler2D differenceImage;

uniform vec4 rect;
uniform vec4 differencerect;
uniform sampler1D lut;
uniform vec3 brightness;
uniform vec4 colorParam;
uniform float hgln;
uniform float hglt;
uniform float crota;

uniform float blurKernel[FSIZE * FSIZE];
uniform float offset[FSIZE];
uniform vec3 sharpenParam;

uniform mat4 cameraTransformationInverse;
uniform vec4 cameraDifferenceRotationQuat;
uniform vec4 diffcameraDifferenceRotationQuat;
uniform vec2 viewport;
uniform vec2 viewportOffset;
uniform vec3 cutOffDirection;
uniform float cutOffValue;
uniform vec2 cutOffRadius;
uniform vec2 polarRadii;

float fetch(sampler2D tex, vec2 coord, vec3 bright) {
    return pow(texture2D(tex, coord).r, bright.z) * bright.y + bright.x;
}

vec4 getColor(vec2 texcoord, vec2 difftexcoord, float factor) {
    vec3 b = brightness;
    if (enhanced == 1) {
        b.y *= factor;
    }

    float v;
    float conv = 0.;
    if (isdifference == NODIFFERENCE) {
        v = fetch(image, texcoord, b);
        for (int j = 0; j < FSIZE; j++) {
            for (int i = 0; i < FSIZE; i++) {
                conv += fetch(image, texcoord + vec2(offset[i], offset[j]) * sharpenParam.xy, b) * blurKernel[FSIZE * j + i];
            }
        }
    } else {
        v = fetch(image, texcoord, b) - fetch(differenceImage, difftexcoord, b);
        float diff;
        for (int j = 0; j < FSIZE; j++) {
            for (int i = 0; i < FSIZE; i++) {
                diff = fetch(image, texcoord + vec2(offset[i], offset[j]) * sharpenParam.xy, b) -
                       fetch(differenceImage, difftexcoord + vec2(offset[i], offset[j]) * sharpenParam.xy, b);
                conv += diff * blurKernel[FSIZE * j + i];
            }
        }
        v = v * BOOST + 0.5;
        conv = conv * BOOST + 0.5;
    }
    v = mix(v, conv, sharpenParam.z);

    return texture1D(lut, v) * colorParam;
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
    if (altnormal.z < 0.)
        discard;
    return -dot(altnormal.xy, vecin.xy) / altnormal.z;
}

float intersectPlanediff(vec4 vecin) {
    vec3 altnormal = rotate_vector(diffcameraDifferenceRotationQuat, vec3(0., 0., 1.));
    if (altnormal.z < 0.)
        discard;
    return -dot(altnormal.xy, vecin.xy) / altnormal.z;
}
