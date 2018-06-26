#version 150 core

#define NODIFFERENCE 0
#define PI 3.1415926535897932384626433832795
#define TWOPI 2.*PI

#define BOOST 1. / (0.2 * 2.)
#define FSIZE 3

out vec4 FragColor;

uniform sampler2D image;
uniform int isdifference;
uniform int enhanced;
uniform sampler2D differenceImage;

uniform vec4 rect;
uniform vec4 differencerect;
uniform sampler1D lut;
uniform vec3 brightness;
uniform vec4 color;

uniform float hgln;
uniform float hglt;
uniform float hglnDiff;
uniform float hgltDiff;
uniform float crota;
uniform float crotaDiff;

uniform float blurKernel[FSIZE * FSIZE];
uniform float offset[FSIZE];
uniform vec3 sharpen;

uniform mat4 cameraTransformationInverse;
uniform vec4 cameraDifferenceRotationQuat;
uniform vec4 diffcameraDifferenceRotationQuat;
uniform vec3 viewport;
uniform vec2 viewportOffset;
uniform vec3 cutOffDirection;
uniform float cutOffValue;
uniform vec2 cutOffRadius;
uniform vec2 polarRadii;

float fetch(sampler2D tex, vec2 coord, vec3 bright) {
    return /*pow(texture2D(tex, coord).r, bright.z)*/ texture(tex, coord).r * bright.y + bright.x;
}

vec4 getColor(vec2 texcoord, vec2 difftexcoord, float factor) {
    vec3 b = brightness;
    if (enhanced == 1) {
        b.y *= factor * factor * factor;
    }

    float v;
    float conv = 0.;
    if (isdifference == NODIFFERENCE) {
        v = fetch(image, texcoord, b);
        for (int j = 0; j < FSIZE; j++) {
            for (int i = 0; i < FSIZE; i++) {
                conv += fetch(image, texcoord + vec2(offset[i], offset[j]) * sharpen.xy, b) * blurKernel[FSIZE * j + i];
            }
        }
    } else {
        v = fetch(image, texcoord, b) - fetch(differenceImage, difftexcoord, b);
        float diff;
        for (int j = 0; j < FSIZE; j++) {
            for (int i = 0; i < FSIZE; i++) {
                diff = fetch(image, texcoord + vec2(offset[i], offset[j]) * sharpen.xy, b) -
                       fetch(differenceImage, difftexcoord + vec2(offset[i], offset[j]) * sharpen.xy, b);
                conv += diff * blurKernel[FSIZE * j + i];
            }
        }
        v = v * BOOST + 0.5;
        conv = conv * BOOST + 0.5;
    }
    v = mix(v, conv, sharpen.z);

    return texture(lut, v) * color;
}

void clamp_texcoord(const vec2 texcoord) {
    if (texcoord.x < 0. || texcoord.y < 0. || texcoord.x > 1. || texcoord.y > 1.)
        discard;
}

vec3 rotate_vector_inverse(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(cross(vec, quat.xyz) + quat.w * vec, quat.xyz);
}

vec3 rotate_vector(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(quat.xyz, cross(quat.xyz, vec) + quat.w * vec);
}

float intersectPlane(const vec4 quat, vec4 vecin) {
    vec3 altnormal = rotate_vector(quat, vec3(0., 0., 1.));
    if (altnormal.z <= 0.)
        discard;
    return -dot(altnormal.xy, vecin.xy) / altnormal.z;
}

vec2 getScrPos(void) {
    vec2 normalizedScreenpos = 2. * (gl_FragCoord.xy - viewportOffset) / viewport.xy - 1.;
    vec4 up1 = cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);
    vec2 scrpos = vec2(viewport.z * up1.x, up1.y) + .5;
    clamp_texcoord(scrpos);
    return scrpos;
}
