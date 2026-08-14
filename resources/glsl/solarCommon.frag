#version 300 es

precision highp float;

#define NODIFFERENCE 0.
#define PI 3.1415926535897932384626433832795
#define HALFPI (PI / 2.)
#define TWOPI  (2. * PI)

#define CLIP_SCALE_NARROW 1. / (2. * 32.)
#define CLIP_SCALE_WIDE   1. / (2. * 50. * 215.09151684811678)

#define BOOST 1. / (0.2 * 2.)

const float WCS_PROJECTION_TAN = 0.;
const float WCS_PROJECTION_ARC = 1.;
const float WCS_PROJECTION_AZP = 2.;
const float WCS_PROJECTION_ZPN = 3.;
const float WCS_PROJECTION_CAR = 4.;
const float WCS_PROJECTION_CEA = 5.;

out vec4 outColor;
in vec2 normalizedScreenpos;

struct WCS {
    vec4 cameraDiff; // not strictly WCS
    vec4 rect;
    vec4 planeToImage; // row-major 2x2 matrix
    vec2 crval;
    float zpnUpperEta;
    float deltaT; // not strictly WCS
};

layout(std140) uniform WCSBlock {
    WCS wcs[2];
};

struct ProjectionParams {
    float projectionCode;
    float planeUnitsPerRadian;
    float observerDistance;
    float padding0;
    vec4 sourceViewQuat;
};

layout(std140) uniform ProjectionBlock {
    ProjectionParams projection[2];
};

layout(std140) uniform ScreenBlock {
    mat4 inverseMVP;
    float iaspect;
    float xStart;
    float xStop;
    float yStart;
    float yStop;
    float lambda;
    vec2 latiOrigin;
} screen;

layout(std140) uniform DisplayBlock {
    vec4 color;
    vec3 sharpen;
    float isDiff;
    vec2 userSector;
    vec2 metadataSector;
    vec3 cutOff;
    float calculateDepth;
    vec2 brightness;
    vec2 radii;
    vec2 slit;
    float enhanced;
    float upsilonLow;
    float upsilonHigh;
} display;

uniform sampler2D image;
uniform sampler2D diffImage;
uniform sampler2D lut;
uniform sampler2D mask;

uniform float pv0[6]; // kept as plain uniforms for simple indexed access
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
    if (texture(mask, texcoord).r == 0.)
        discard;

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

    if (display.upsilonLow != 1. || display.upsilonHigh != 1.) {
        // Two-sided gamma about the median (Gilly & DeForest Eq. 2): upsilonLow and
        // upsilonHigh independently set the curvature below and above I = 0.5
        value = clamp(value, 0., 1.);
        value = value < .5 ? .5 * pow(2. * value, display.upsilonLow) : 1. - .5 * pow(2. - 2. * value, display.upsilonHigh);
    }

    value += dither(texcoord);

    return texture(lut, vec2(value, 0.5)) * display.color;
}

void clamp_coord(const vec2 coord) {
    if (coord.x < display.slit.x || coord.y < 0. || coord.x > display.slit.y || coord.y > 1.)
        discard;
}

void clamp_value(const float value, const float low, const float high) {
    if (value < low || value > high)
        discard;
}

// Convert normalized screen coordinates to the view-aligned plane in scene units.
// The projection is orthographic, so xy is independent of clip-space z and needs no perspective divide.
vec2 getViewPosition(void) {
    return (screen.inverseMVP * vec4(normalizedScreenpos, -1., 1.)).xy;
}

// Map the centered view plane to the [0, 1] map domain: remove the viewport's
// horizontal aspect scaling, move the origin to the lower-left, and discard outside it.
vec2 getNormalizedMapPos(void) {
    vec2 pos = getViewPosition();
    pos = vec2(screen.iaspect * pos.x, pos.y) + .5;
    clamp_coord(pos);
    return pos;
}

// Convert a normalized warp radius back to radial distance in solar radii.
// The disk is linear; only distances beyond the limb use Box-Cox scaling.
float unwarpRadius(float normalizedRadius) {
    float outerRadius = screen.yStop;
    float limbPosition = 1. / outerRadius;
    if (outerRadius <= 1. || normalizedRadius <= limbPosition)
        return normalizedRadius / limbPosition;

    float u = (normalizedRadius - limbPosition) / (1. - limbPosition);
    float lambda = screen.lambda;
    return lambda == 0.
            ? pow(outerRadius, u)
            : pow(1. + u * (pow(outerRadius, lambda) - 1.), 1. / lambda);
}

vec3 rotate_vector_inverse(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(cross(vec, quat.xyz) + quat.w * vec, quat.xyz);
}

vec3 rotate_vector(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(quat.xyz, cross(quat.xyz, vec) + quat.w * vec);
}

vec2 transform_plane_to_image(const vec4 transform, const vec2 vec) {
    return vec2(
        transform.x * vec.x + transform.y * vec.y,
        transform.z * vec.x + transform.w * vec.y);
}

// Differential solar rotation.
float differentialRotation(const float dt, const float sinLatitude) {
    float sinLat2 = sinLatitude * sinLatitude;
    // Snodgrass, Table 1 Magnetic - http://articles.adsabs.harvard.edu/pdf/1990ApJ...351..309S
    return dt * (0.01367 - 0.339 * sinLat2 - 0.485 * sinLat2 * sinLat2); // 2.879 urad/s - 14.1844 deg/86400s (not fully right: 1st SI, 2nd TDB)
}

vec3 differential(const float dt, const vec3 v) {
    float delta = differentialRotation(dt, v.y);
    float sinDelta = sin(delta);
    float cosDelta = cos(delta);
    return vec3(
        v.x * cosDelta - v.z * sinDelta,
        v.y,
        v.z * cosDelta + v.x * sinDelta);
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
    float cosPhi = cos(phi);
    float cosTheta = cos(theta);
    float raySign = cosPhi * cosTheta < 0. ? -1. : 1.;
    return vec3(raySign * sin(phi) * cosTheta, raySign * sin(theta), -raySign * cosPhi * cosTheta);
}

vec2 helioprojectiveToHpcXY(const vec2 helioprojective, const float observerDistance) {
    vec3 ray = helioprojectiveToObserverRay(helioprojective);
    if (ray.z >= 0.)
        discard;
    return -observerDistance * ray.xy / ray.z;
}

// Native zenithal coordinates for TAN/ARC/AZP/ZPN forward projection.
vec3 nativeZenithalCoordinates(const vec2 helioprojective, const vec2 crval, const float planeUnitsPerRad) {
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

    return vec3(
        cosLat * sinDeltaLon,
        cosLat0 * sinLat - sinLat0 * cosLat * cosDeltaLon,
        sinLat0 * sinLat + cosLat0 * cosLat * cosDeltaLon);
}

vec2 projectTanToWcsPlane(const vec2 helioprojective, const vec2 crval, const float planeUnitsPerRad) {
    vec3 nativeCoords = nativeZenithalCoordinates(helioprojective, crval, planeUnitsPerRad);
    if (nativeCoords.z <= 0.)
        discard;

    float scale = planeUnitsPerRad / nativeCoords.z;
    return scale * nativeCoords.xy;
}

vec2 projectArcToWcsPlane(const vec2 helioprojective, const vec2 crval, const float planeUnitsPerRad) {
    vec3 nativeCoords = nativeZenithalCoordinates(helioprojective, crval, planeUnitsPerRad);
    float nativeRadius = length(nativeCoords.xy);
    if (nativeRadius == 0.)
        return vec2(0.);

    float nativeDistance = atan(nativeRadius, nativeCoords.z);
    float scale = planeUnitsPerRad * nativeDistance / nativeRadius;
    return scale * nativeCoords.xy;
}

vec2 projectAzpToWcsPlane(const vec2 helioprojective, const vec2 crval, const float planeUnitsPerRad, const float[6] PV) {
    float mu = PV[1];
    float gamma = radians(PV[2]);

    vec3 nativeCoords = nativeZenithalCoordinates(helioprojective, crval, planeUnitsPerRad);
    if (nativeCoords.x == 0. && nativeCoords.y == 0.)
        return vec2(0.);

    // For the non-slanted AZP case, mu > 1 folds back once dR/dtheta changes sign.
    // Keep only the primary forward branch.
    if (gamma == 0. && mu > 1. && mu * nativeCoords.z + 1. <= 0.)
        discard;

    float denom = mu + nativeCoords.z - nativeCoords.y * tan(gamma);
    if (denom <= 0.)
        discard;

    float scale = planeUnitsPerRad * (mu + 1.) / denom;
    return scale * vec2(nativeCoords.x, nativeCoords.y / cos(gamma));
}

float zpnRadial(const float eta, const float[6] PV) {
    float radial = PV[5];
    for (int i = 4; i >= 0; --i)
        radial = radial * eta + PV[i];
    return radial;
}

vec2 projectZpnToWcsPlane(const vec2 helioprojective, const WCS wcs, const float planeUnitsPerRad, const float[6] PV) {
    vec3 nativeCoords = nativeZenithalCoordinates(helioprojective, wcs.crval, planeUnitsPerRad);
    float nativeRadius = length(nativeCoords.xy);
    if (nativeRadius == 0.)
        return vec2(0.);

    float nativeDistance = atan(nativeRadius, nativeCoords.z);
    if (nativeDistance > wcs.zpnUpperEta)
        discard;

    float radial = zpnRadial(nativeDistance, PV);
    if (radial < 0.)
        discard;

    float scale = planeUnitsPerRad * radial / nativeRadius;
    return scale * nativeCoords.xy;
}

float wrapDeltaLongitude(float lon, float lon0) {
    return mod(lon - lon0 + PI, TWOPI) - PI;
}

// Surface-map forward projections used by Latitudinal and Orthographic.
vec2 projectCarToWcsPlane(const vec3 world, const vec2 crval, const float planeUnitsPerRad) {
    // CAR is a direct surface lon/lat map, not observer-image geometry.
    float lon = atan(world.x, world.z);
    float lat = asin(clamp(world.y / length(world), -1., 1.));
    vec2 referenceAngles = crval / planeUnitsPerRad;
    return vec2(
        planeUnitsPerRad * wrapDeltaLongitude(lon, referenceAngles.x),
        planeUnitsPerRad * (lat - referenceAngles.y));
}

vec2 projectCeaToWcsPlane(const vec3 world, const vec2 crval, const float planeUnitsPerRad, const float[6] PV) {
    // CEA is a direct surface lon/lat map with equal-area latitude scaling.
    float lon = atan(world.x, world.z);
    float sinLat = clamp(world.y / length(world), -1., 1.);
    float lambda = max(abs(PV[1]), 1e-12);
    vec2 referenceCoord = crval / planeUnitsPerRad;
    return vec2(
        planeUnitsPerRad * wrapDeltaLongitude(lon, referenceCoord.x),
        planeUnitsPerRad * (sinLat / lambda - referenceCoord.y));
}

// Projection-space to texture-space mapping.
vec2 projectHelioprojectiveToWcsPlane(const vec2 helioprojective, const WCS wcs, const ProjectionParams projection, const float[6] PV) {
    if (projection.projectionCode == WCS_PROJECTION_TAN)
        return projectTanToWcsPlane(helioprojective, wcs.crval, projection.planeUnitsPerRadian);
    if (projection.projectionCode == WCS_PROJECTION_ARC)
        return projectArcToWcsPlane(helioprojective, wcs.crval, projection.planeUnitsPerRadian);
    if (projection.projectionCode == WCS_PROJECTION_AZP)
        return projectAzpToWcsPlane(helioprojective, wcs.crval, projection.planeUnitsPerRadian, PV);
    if (projection.projectionCode == WCS_PROJECTION_ZPN)
        return projectZpnToWcsPlane(helioprojective, wcs, projection.planeUnitsPerRadian, PV);

    return projectTanToWcsPlane(helioprojective, wcs.crval, projection.planeUnitsPerRadian);
}

vec2 wcsPlaneToUnclampedTexcoord(const vec2 plane, const WCS wcs) {
    vec2 centered = transform_plane_to_image(wcs.planeToImage, plane);
    vec4 rect = wcs.rect;
    return rect.zw * vec2(centered.x - rect.x, -centered.y - rect.y);
}

vec2 wcsPlaneToTexcoord(const vec2 plane, const WCS wcs) {
    vec2 texcoord = wcsPlaneToUnclampedTexcoord(plane, wcs);
    clamp_coord(texcoord);
    return texcoord;
}

vec2 wcsPlaneToWrappedXTexcoord(const vec2 plane, const WCS wcs) {
    vec2 texcoord = wcsPlaneToUnclampedTexcoord(plane, wcs);
    texcoord.x = fract(texcoord.x);
    clamp_coord(texcoord);
    return texcoord;
}

vec2 normalizedMapToHelioprojective(const vec2 mapPos) {
    return vec2(
        radians(screen.xStart + mapPos.x * (screen.xStop - screen.xStart)),
        radians(screen.yStart + mapPos.y * (screen.yStop - screen.yStart)));
}

bool helioprojectiveToWorld(const vec2 helioprojective, const float observerDistance, out vec3 world) {
    vec3 ray = helioprojectiveToObserverRay(helioprojective);
    float b = observerDistance * ray.z;
    float c = observerDistance * observerDistance - 1.;
    vec3 observer = observerPosition(observerDistance);
    float discriminant = b * b - c;
    if (discriminant < 0.) {
        world = vec3(0.);
        return false;
    }

    float root = sqrt(discriminant);
    float tNear = -b - root;
    float tFar = -b + root;
    float t = tNear > 0. ? tNear : tFar;
    if (t <= 0.) {
        world = vec3(0.);
        return false;
    }

    world = observer + t * ray;
    return true;
}

vec2 hpcXYToHelioprojective(const vec2 hpcXY, const float observerDistance) {
    return worldToHelioprojective(vec3(hpcXY, 0.), observerDistance);
}

float hpcEnhancementFactor(const vec2 hpcXY) {
    return max(1., length(hpcXY));
}

void clipSectorOpening(const float theta, const vec2 sector) {
    if (sector.y <= 0.)
        return;

    float delta = abs(theta - sector.x);
    float angularDistance = min(delta, TWOPI - delta);
    if (angularDistance < sector.y)
        discard;
}

void clipSectors(const vec2 point) {
    if (display.metadataSector.y <= 0. && display.userSector.y <= 0.)
        return;

    float theta = atan(point.y, point.x);
    clipSectorOpening(theta, display.metadataSector);
    clipSectorOpening(theta, display.userSector);
}

void clipPlanarMasks(const vec2 point) {
    clipSectors(point);

    float radial2 = dot(point, point);
    float minRadius2 = display.radii.x * display.radii.x;
    float maxRadius2 = display.radii.y * display.radii.y;
    if (radial2 > maxRadius2 || radial2 < minRadius2)
        discard;

    if (display.cutOff.z >= 0.) {
        float flatDist = abs(dot(point, display.cutOff.xy));
        vec2 cutOffAlt = vec2(-display.cutOff.y, display.cutOff.x);
        float flatDistAlt = abs(dot(point, cutOffAlt));
        if (flatDist > display.cutOff.z || flatDistAlt > display.cutOff.z)
            discard;
    }
}

vec2 sampleHpcTexcoord(const WCS wcs, const ProjectionParams projection, vec2 helioprojective, const vec2 hpcXY, const float[6] PV, out float enhancementFactor) {
    enhancementFactor = 1.;
    float observerDistance = projection.observerDistance;

    vec3 world;
    if (helioprojectiveToWorld(helioprojective, observerDistance, world)) {
        if (wcs.deltaT != 0.) {
            vec3 rotatedWorld = differential(wcs.deltaT, world);
            helioprojective = worldToHelioprojective(rotatedWorld, observerDistance);
        }
    } else {
        enhancementFactor = hpcEnhancementFactor(hpcXY);
    }

    vec2 plane = projectHelioprojectiveToWcsPlane(helioprojective, wcs, projection, PV);
    return wcsPlaneToTexcoord(plane, wcs);
}
