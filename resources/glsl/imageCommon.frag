#version 300 es

precision highp float;

#define NODIFFERENCE 0.
#define PI 3.1415926535897932384626433832795
#define HALFPI (PI / 2.)
#define TWOPI  (2. * PI)

#define BOOST (1. / (0.2 * 2.))

const float WCS_PROJECTION_TAN = 0.;
const float WCS_PROJECTION_ARC = 1.;
const float WCS_PROJECTION_AZP = 2.;
const float WCS_PROJECTION_ZPN = 3.;
const float WCS_PROJECTION_CAR = 4.;
const float WCS_PROJECTION_CEA = 5.;
const float ORTHOGRAPHIC_TAN = 6.; // JHV display policy; the FITS projection remains TAN.

// A prepared-image-frame point p yields the projection direction (p.xy, projectionOriginZ - p.z).
// Observer images project from their observer, surface maps from Sun center with the display rotation
// folded into the projection frame. Sphere points below minSurfaceZ have no source data.

out vec4 outColor;
in vec2 normalizedScreenpos;

// Prepared image frame: observer-aligned for observer images, display-aligned for surface maps.
struct Image {
    // Projection-local coordinates from a forward-Z ray in the prepared image frame.
    mat3 projectionFromRay;
    float projectionCode; // WCS projection enum, or ORTHOGRAPHIC_TAN display policy.
    float azpTanTilt;
    float azpSecTilt;
    float zpnMaxAngle;
    // Effective WCS PV2 coefficients: ZPN polynomial coefficients; AZP/CEA use pv1.
    float pv0;
    float pv1;
    float pv2;
    float pv3;
    float pv4;
    float pv5;
    vec2 northPole;
    vec2 southPole;

    // Projection plane -> original normalized image -> decoded texture.
    vec2 imageFromPlaneX;
    vec2 imageFromPlaneY;
    vec2 imageOffset;
    vec2 texScale;
    vec2 texOffset;

    // Geometry in the prepared image frame, followed by its rotations.
    float observerDistance; // In solar radii.
    float projectionOriginZ; // Observer distance for images, zero for maps.
    float minSurfaceZ; // 1/D for observer images, zero for Orthographic TAN, -inf for maps.
    float deltaT; // Megaseconds from the tracing time.
    vec4 imageToView; // Prepared image frame -> displayed frame.
    vec4 solarToImage; // Solar frame -> prepared image frame.
};

layout(std140) uniform ImageBlock {
    Image images[2];
};

layout(std140) uniform ScreenBlock {
    mat4 inverseMVP;
    vec4 mapBounds; // xStart, xStop, yStart, yStop
    vec2 latiOrigin;
    float iaspect;
    float lambda;
} screen;

layout(std140) uniform DisplayBlock {
    vec4 color;
    vec3 sharpen;
    float isDiff;
    vec2 brightness;
    vec2 upsilon;
    vec2 userSector;
    vec2 metadataSector;
    vec3 cutOff;
    float calculateDepth;
    vec2 radii;
    vec2 slit;
    float enhanced;
    float padding0;
    float padding1;
    float padding2;
} display;

uniform sampler2D image;
uniform sampler2D diffImage;
uniform sampler2D lut;
uniform sampler2D mask;

#define BLUR_TAP_COUNT (3 * 3)
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

// https://shader-tutorial.dev/advanced/color-banding-dithering/
const float NOISE_GRANULARITY = 1. / 255.;
const vec2 nvec = vec2(12.9898, 78.233);

float dither(const vec2 coord) {
    float random = fract(sin(dot(coord, nvec)) * 43758.5453);
    return mix(-NOISE_GRANULARITY, NOISE_GRANULARITY, random);
}

float fetch(const sampler2D tex, const vec2 coord, const vec2 bright) {
    return texture(tex, coord).r * bright.y + bright.x;
}

vec4 getColor(const vec2 texcoord, const vec2 difftexcoord, const float factor) {
    vec2 imageCoord = texcoord * images[0].texScale + images[0].texOffset;
    vec2 diffImageCoord = difftexcoord * images[1].texScale + images[1].texOffset;
    vec2 brightness = display.brightness;
    if (display.enhanced != 0. && factor != 1.)
        brightness.y *= pow(factor, display.enhanced);

    bool diffMode = display.isDiff != NODIFFERENCE;
    float centerValue = fetch(image, imageCoord, brightness);
    if (diffMode)
        centerValue -= fetch(diffImage, diffImageCoord, brightness);
    float value = diffMode ? centerValue * BOOST + 0.5 : centerValue;

    vec2 sharpenStep = display.sharpen.xy;
    float sharpenMix = display.sharpen.z;
    if (sharpenMix != 0.) {
        float blurredValue = 0.;
        if (!diffMode) {
            for (int i = 0; i < BLUR_TAP_COUNT; i++) {
                vec2 offset = blurOffset[i] * sharpenStep;
                blurredValue += (i == BLUR_TAP_COUNT / 2 ? centerValue : fetch(image, imageCoord + offset, brightness)) * blurKernel[i];
            }
        } else {
            vec2 diffScale = images[1].texScale / images[0].texScale;
            for (int i = 0; i < BLUR_TAP_COUNT; i++) {
                vec2 offset = blurOffset[i] * sharpenStep;
                blurredValue += (i == BLUR_TAP_COUNT / 2 ? centerValue : fetch(image, imageCoord + offset, brightness) - fetch(diffImage, diffImageCoord + offset * diffScale, brightness)) * blurKernel[i];
            }
            blurredValue = blurredValue * BOOST + 0.5;
        }
        value = mix(value, blurredValue, sharpenMix);
    }

    if (display.upsilon.x != 1. || display.upsilon.y != 1.) {
        // Two-sided gamma about the median (Gilly & DeForest Eq. 2): upsilonLow and
        // upsilonHigh independently set the curvature below and above I = 0.5
        value = clamp(value, 0., 1.);
        value = value < .5 ? .5 * pow(2. * value, display.upsilon.x) : 1. - .5 * pow(2. - 2. * value, display.upsilon.y);
    }

    value += dither(texcoord);

    return texture(lut, vec2(value, 0.5)) * display.color;
}

// Detector-mask rejection and color output are shared by every image mode.
void renderImage(const vec2 texcoord, const vec2 difftexcoord, const float factor) {
    if (texture(mask, texcoord).r == 0.)
        discard;
    outColor = getColor(texcoord, difftexcoord, factor);
}
