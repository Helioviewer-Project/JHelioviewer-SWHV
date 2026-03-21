#version 330 core

//precision mediump float;

#define NODIFFERENCE 0
#define PI 3.1415926535897932384626433832795
#define HALFPI (PI / 2.)
#define TWOPI  (2. * PI)

#define CLIP_SCALE_NARROW 1. / (2. * 32.)
#define CLIP_SCALE_WIDE   1. / (2. * 50. * 215.09151684811678)

#define BOOST 1. / (0.2 * 2.)

const int WCS_PROJECTION_TAN = 0;
const int WCS_PROJECTION_AZP = 1;
const int WCS_PROJECTION_ZPN = 2;

out vec4 outColor;

struct Screen {
    mat4 inverseMVP;
    vec4 viewport;
    float iaspect;
    float xStart;
    float xStop;
    float yStart;
    float yStop;
};

layout(std140) uniform ScreenBlock {
    Screen screen;
};

struct WCS {
    vec4 cameraDiff; // not strictly WCS
    vec4 rect;
    vec4 crota;
    vec2 crval;
    float deltaT; // not strictly WCS
    float padding;
    vec4 projectionMeta; // x = projection code, y = plane units per radian, z = observer distance
};

layout(std140) uniform WCSBlock {
    WCS wcs[2];
};

struct Display {
    vec4 color;
    vec3 sharpen;
    float isDiff;
    vec3 sector;
    float enhanced;
    vec3 cutOff;
    float calculateDepth;
    vec2 brightness;
    vec2 radii;
    vec2 slit;
};

layout(std140) uniform DisplayBlock {
    Display display;
};

uniform sampler2D image;
uniform sampler2D diffImage;
uniform sampler1D lut;

uniform vec3 grid[2];

uniform float pv0[6]; // should be in WCS uniform block
uniform float pv1[6];

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
    vec2 brightness = display.brightness;
    if (display.enhanced != 0. && factor != 1.)
        brightness.y *= pow(factor, display.enhanced);

    float value;
    bool diffMode = display.isDiff != NODIFFERENCE;
    if (!diffMode) {
        value = fetch(image, texcoord, brightness);
    } else {
        value = fetch(image, texcoord, brightness) - fetch(diffImage, difftexcoord, brightness);
        value = value * BOOST + 0.5;
    }

    vec2 sharpenStep = display.sharpen.xy;
    float sharpenMix = display.sharpen.z;
    if (sharpenMix != 0.) {
        float blurredValue = 0.;
        if (!diffMode) {
            for (int i = 0; i < BLUR_TAP_COUNT; i++) {
                vec2 offset = blurOffset[i] * sharpenStep;
                blurredValue += fetch(image, texcoord + offset, brightness) * blurKernel[i];
            }
        } else {
            for (int i = 0; i < BLUR_TAP_COUNT; i++) {
                vec2 offset = blurOffset[i] * sharpenStep;
                blurredValue += (fetch(image, texcoord + offset, brightness) - fetch(diffImage, difftexcoord + offset, brightness)) * blurKernel[i];
            }
            blurredValue = blurredValue * BOOST + 0.5;
        }
        value = mix(value, blurredValue, sharpenMix);
    }

    value += dither(texcoord);

    return texture(lut, value) * display.color;
}

void clamp_texture(const vec2 texcoord) {
    if (texcoord.x < 0. || texcoord.y < 0. || texcoord.x > 1. || texcoord.y > 1.)
        discard;
}

void clamp_coord(const vec2 coord) {
    if (coord.x < display.slit.x || coord.y < 0. || coord.x > display.slit.y || coord.y > 1.)
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

// Differential solar rotation.
float differentialRotation(const float dt, const float theta) {
    float sinLat2 = sin(theta);
    sinLat2 *= sinLat2;
    // Snodgrass, Table 1 Magnetic - http://articles.adsabs.harvard.edu/pdf/1990ApJ...351..309S
    return dt * (0.01367 - 0.339 * sinLat2 - 0.485 * sinLat2 * sinLat2); // 2.879 urad/s - 14.1844 deg/86400s (not fully right: 1st SI, 2nd TDB)
}

vec3 differential(const float dt, const vec3 v) {
    float phi = atan(v.x, v.z);
    float theta = asin(v.y);
    phi -= differentialRotation(dt, theta); // difference from rigid rotation
    return vec3(cos(theta) * sin(phi), v.y, cos(theta) * cos(phi));
}

// Observer-centred helioprojective geometry.
vec2 worldToHelioprojective(const vec3 world, const float observerDistance) {
    float zeta = observerDistance - world.z;
    return vec2(
        atan(world.x, zeta),
        atan(world.y, sqrt(world.x * world.x + zeta * zeta)));
}

vec3 observerPosition(const float observerDistance) {
    return vec3(0., 0., observerDistance);
}

vec3 helioprojectiveToObserverRay(const vec2 helioprojective) {
    float phi = helioprojective.x;
    float theta = helioprojective.y;
    return normalize(vec3(tan(phi), tan(theta) / cos(phi), -1.));
}

vec3 helioprojectiveToHpcPlanePoint(const vec2 helioprojective, const float observerDistance) {
    vec3 ray = helioprojectiveToObserverRay(helioprojective);
    if (ray.z >= 0.)
        discard;
    return observerPosition(observerDistance) - observerDistance * ray / ray.z;
}

// Native zenithal coordinates for TAN/AZP/ZPN forward projection.
void nativeZenithalCoordinates(
    const vec2 helioprojective,
    const vec2 crval,
    const float planeUnitsPerRad,
    out float nativeX,
    out float nativeY,
    out float cosNativeDistance
) {
    float phi = helioprojective.x;
    float theta = helioprojective.y;
    vec2 referenceAngles = crval / planeUnitsPerRad;
    float phi0 = referenceAngles.x;
    float theta0 = referenceAngles.y;

    float sinLat = sin(theta);
    float cosLat = cos(theta);
    float sinLat0 = sin(theta0);
    float cosLat0 = cos(theta0);
    float deltaLon = phi - phi0;
    float sinDeltaLon = sin(deltaLon);
    float cosDeltaLon = cos(deltaLon);

    nativeX = cosLat * sinDeltaLon;
    nativeY = cosLat0 * sinLat - sinLat0 * cosLat * cosDeltaLon;
    cosNativeDistance = sinLat0 * sinLat + cosLat0 * cosLat * cosDeltaLon;
}

vec2 projectTanToWcsPlane(const vec2 helioprojective, const vec2 crval, const float planeUnitsPerRad) {
    float nativeX;
    float nativeY;
    float cosNativeDistance;
    nativeZenithalCoordinates(helioprojective, crval, planeUnitsPerRad, nativeX, nativeY, cosNativeDistance);
    if (cosNativeDistance <= 0.)
        discard;

    return planeUnitsPerRad * vec2(
        nativeX / cosNativeDistance,
        nativeY / cosNativeDistance);
}

vec2 projectAzpToWcsPlane(const vec2 helioprojective, const vec2 crval, const float planeUnitsPerRad, const float[6] PV) {
    float mu = PV[1];
    float gamma = radians(PV[2]);

    float nativeX;
    float nativeY;
    float cosNativeDistance;
    nativeZenithalCoordinates(helioprojective, crval, planeUnitsPerRad, nativeX, nativeY, cosNativeDistance);
    float nativeRadius = length(vec2(nativeX, nativeY));
    if (nativeRadius == 0.)
        return vec2(0.);

    // For the non-slanted AZP case, mu > 1 folds back once dR/dtheta changes sign.
    // Keep only the primary forward branch.
    if (gamma == 0. && mu > 1. && mu * cosNativeDistance + 1. <= 0.)
        discard;

    float denom = mu + cosNativeDistance - nativeY * tan(gamma);
    if (denom <= 0.)
        discard;

    float radial = (mu + 1.) * nativeRadius / denom;
    return planeUnitsPerRad * vec2(
        radial * nativeX / nativeRadius,
        radial * nativeY / (nativeRadius * cos(gamma)));
}

// Six-term ZPN forward projection on the primary monotone branch.
void zpnRadialAndDerivative(const float eta, const float[6] PV, out float radial, out float derivative) {
    radial = PV[5];
    derivative = 5. * PV[5];
    for (int i = 4; i >= 1; --i) {
        radial = radial * eta + PV[i];
        derivative = derivative * eta + float(i) * PV[i];
    }
    radial = radial * eta + PV[0];
}

vec2 projectZpnToWcsPlane(const vec2 helioprojective, const vec2 crval, const float planeUnitsPerRad, const float[6] PV) {
    float nativeX;
    float nativeY;
    float cosNativeDistance;
    nativeZenithalCoordinates(helioprojective, crval, planeUnitsPerRad, nativeX, nativeY, cosNativeDistance);
    float nativeRadius = length(vec2(nativeX, nativeY));
    if (nativeRadius == 0.)
        return vec2(0.);

    float nativeDistance = acos(clamp(cosNativeDistance, -1., 1.));
    float radial;
    float derivative;
    zpnRadialAndDerivative(nativeDistance, PV, radial, derivative);
    if (radial < 0. || derivative <= 0.)
        discard;

    return planeUnitsPerRad * vec2(
        radial * nativeX / nativeRadius,
        radial * nativeY / nativeRadius);
}

// Projection-space to texture-space mapping.
vec2 projectHelioprojectiveToWcsPlane(const vec2 helioprojective, const WCS wcs, const float[6] PV) {
    int projection = int(wcs.projectionMeta.x);
    if (projection == WCS_PROJECTION_TAN)
        return projectTanToWcsPlane(helioprojective, wcs.crval, wcs.projectionMeta.y);
    if (projection == WCS_PROJECTION_AZP)
        return projectAzpToWcsPlane(helioprojective, wcs.crval, wcs.projectionMeta.y, PV);
    if (projection == WCS_PROJECTION_ZPN)
        return projectZpnToWcsPlane(helioprojective, wcs.crval, wcs.projectionMeta.y, PV);

    return helioprojective - wcs.crval;
}

vec2 wcsPlaneToTexcoord(const vec2 plane, const WCS wcs) {
    vec2 centered = rotate_vector_inverse(wcs.crota, vec3(plane, 0)).xy;
    vec4 rect = wcs.rect;
    vec2 texcoord = rect.zw * vec2(centered.x - rect.x, -centered.y - rect.y);
    clamp_coord(texcoord);
    return texcoord;
}
