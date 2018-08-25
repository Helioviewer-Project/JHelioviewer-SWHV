#version 140

precision mediump float;

#define NODIFFERENCE 0
#define PI 3.1415926535897932384626433832795
#define TWOPI 2.*PI

#define BOOST 1. / (0.2 * 2.)

out vec4 outColor;

uniform sampler2D image;
uniform int isdifference;
uniform int enhanced;
uniform sampler2D diffImage;

uniform vec4 rect;
uniform vec4 differencerect;
uniform sampler1D lut;
uniform vec3 brightness;
uniform vec4 color;

uniform vec2 split;

uniform float hgln;
uniform float hglt;
uniform float hglnDiff;
uniform float hgltDiff;
uniform float crota;
uniform float crotaDiff;

uniform mat4 cameraTransformationInverse;
uniform vec4 cameraDifferenceRotationQuat;
uniform vec4 diffcameraDifferenceRotationQuat;
uniform vec3 viewport;
uniform vec2 viewportOffset;
uniform vec3 cutOffDirection;
uniform float cutOffValue;
uniform vec2 cutOffRadius;
uniform vec2 polarRadii;

#define FSIZE 3 * 3
uniform vec3 sharpen;
uniform float blurKernel[FSIZE];
uniform vec2 blurOffset[FSIZE];

float fetch(const sampler2D tex, const vec2 coord, const vec3 bright) {
    return /*pow(texture2D(tex, coord).r, bright.z)*/ texture(tex, coord).r * bright.y + bright.x;
}

vec4 getColor(const vec2 texcoord, const vec2 difftexcoord, const float factor) {
    vec3 b = brightness;
    if (enhanced == 1) {
        b.y *= factor * factor * factor;
    }

    float v;
    float conv = 0.;
    if (isdifference == NODIFFERENCE) {
        v = fetch(image, texcoord, b);
        for (int i = 0; i < FSIZE; i++) {
            conv += fetch(image, texcoord + blurOffset[i] * sharpen.xy, b) * blurKernel[i];
        }
    } else {
        v = fetch(image, texcoord, b) - fetch(diffImage, difftexcoord, b);
        v = v * BOOST + 0.5;

        for (int i = 0; i < FSIZE; i++) {
            conv += (fetch(image, texcoord + blurOffset[i] * sharpen.xy, b) - fetch(diffImage, difftexcoord + blurOffset[i] * sharpen.xy, b)) * blurKernel[i];
        }
        conv = conv * BOOST + 0.5;
    }
    v = mix(v, conv, sharpen.z);

    return texture(lut, v) * color;
}

void clamp_texcoord(const vec2 texcoord) {
    if (texcoord.x < split[0] || texcoord.y < 0. || texcoord.x > split[1] || texcoord.y > 1.)
        discard;
}

vec3 rotate_vector_inverse(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(cross(vec, quat.xyz) + quat.w * vec, quat.xyz);
}

vec3 rotate_vector(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(quat.xyz, cross(quat.xyz, vec) + quat.w * vec);
}

float intersectPlane(const vec4 quat, const vec4 vecin) {
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
