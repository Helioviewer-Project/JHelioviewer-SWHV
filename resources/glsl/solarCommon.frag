#version 330 core

//precision mediump float;

#define NODIFFERENCE 0
#define PI 3.1415926535897932384626433832795
#define HALFPI (PI / 2.)
#define TWOPI  (2. * PI)

#define CLIP_SCALE_NARROW 1. / (2. * 32.)
#define CLIP_SCALE_WIDE   1. / (2. * 50. * 215.09151684811678)

#define BOOST 1. / (0.2 * 2.)

out vec4 outColor;

struct Screen {
    mat4 inverseMVP;
    vec4 viewport;
    float iaspect;
    float yStart;
    float yStop;
    //float padding;
};

layout(std140) uniform ScreenBlock {
    Screen screen;
};

struct WCS {
    vec4 cameraDiff; // not strictly WCS
    vec4 rect;
    vec4 crota;
    vec2 crval;
    // padding 2
};

layout(std140) uniform WCSBlock {
    WCS wcs[2];
};

struct Display {
    vec4 color;
    float bOffset;
    float bScale;
    float enhanced;
    float isDiff;
    float inneR;
    float outeR;
};

layout(std140) uniform DisplayBlock {
    Display display;
};

uniform sampler2D image;
uniform int calculateDepth;
uniform sampler2D diffImage;

uniform sampler1D lut;

uniform vec3 grid[2];

uniform float deltaT[2];

uniform float slit[2];
uniform float sector[3];
uniform vec2 cutOffDirection;
uniform float cutOffValue;

#define FSIZE 3 * 3
uniform vec3 sharpen;
// float[] bc = { 0.06136, 0.24477, 0.38774, 0.24477, 0.06136 }
// https://www.rastergrid.com/blog/2010/09/efficient-gaussian-blur-with-linear-sampling/
const float[] bc = float[](.30613, .38774, .30613);
const float[] blurKernel = float[](
    bc[0] * bc[0], bc[0] * bc[1], bc[0] * bc[2],
    bc[1] * bc[0], bc[1] * bc[1], bc[1] * bc[2],
    bc[2] * bc[0], bc[2] * bc[1], bc[2] * bc[2]
);

const float[] bo = float[](-1.2004377, 0., 1.2004377);
const vec2[] blurOffset = vec2[](
    vec2(bo[0], bo[0]), vec2(bo[1], bo[0]), vec2(bo[2], bo[0]),
    vec2(bo[0], bo[1]), vec2(bo[1], bo[1]), vec2(bo[2], bo[1]),
    vec2(bo[0], bo[2]), vec2(bo[1], bo[2]), vec2(bo[2], bo[2])
);

float fetch(const sampler2D tex, const vec2 coord, const float bright[2]) {
    return texture(tex, coord).r * bright[1] + bright[0];
}

// https://shader-tutorial.dev/advanced/color-banding-dithering/
const float NOISE_GRANULARITY = 1. / 255.;
const vec2 nvec = vec2(12.9898, 78.233);

float dither(const vec2 coord) {
    float random = fract(sin(dot(coord, nvec)) * 43758.5453);
    return mix(-NOISE_GRANULARITY, NOISE_GRANULARITY, random);
}

vec4 getColor(const vec2 texcoord, const vec2 difftexcoord, const float factor) {
    float b[2] = float[](display.bOffset, display.bScale);
    if (display.enhanced == 1) {
        b[1] *= factor * factor * factor;
    }

    float v;
    float conv = 0.;
    if (display.isDiff == NODIFFERENCE) {
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
    v = mix(v, conv, sharpen.z) + dither(texcoord);

    return texture(lut, v) * display.color;
}

void clamp_texture(const vec2 texcoord) {
    if (texcoord.x < 0. || texcoord.y < 0. || texcoord.x > 1. || texcoord.y > 1.)
        discard;
}

void clamp_coord(const vec2 coord) {
    if (coord.x < slit[0] || coord.y < 0. || coord.x > slit[1] || coord.y > 1.)
        discard;
}

void clamp_value(const float value, const float low, const float high) {
    if (value < low || value > high)
        discard;
}

vec2 getScrPos(void) {
    vec2 normalizedScreenpos = 2. * (gl_FragCoord.xy - screen.viewport.xy) / screen.viewport.zw - 1.;
    vec4 up1 = screen.inverseMVP * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);
    vec2 scrpos = vec2(screen.iaspect * up1.x, up1.y) + .5;
    clamp_coord(scrpos);
    return scrpos;
}

vec3 rotate_vector_inverse(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(cross(vec, quat.xyz) + quat.w * vec, quat.xyz);
}

vec3 rotate_vector(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(quat.xyz, cross(quat.xyz, vec) + quat.w * vec);
}

vec3 apply_center(const vec3 v, const vec2 shift, const vec4 quat) {
    vec3 r = vec3(v.xy - shift, v.z);
    return rotate_vector_inverse(quat, r);
}

float differentialRotation(const float dt, const float theta) {
    float sin2l = sin(theta);
    sin2l *= sin2l;
    // Snodgrass, Table 1 Magnetic - http://articles.adsabs.harvard.edu/pdf/1990ApJ...351..309S
    return dt * (0.01367 - 0.339 * sin2l - 0.485 * sin2l * sin2l); // 2.879 urad/s - 14.1844 deg/86400s (not fully right: 1st SI, 2nd TDB)
}
