
const vec3 zAxis = vec3(0, 0, 1);
const float PLANE_Z_EPS = 1e-8;

// TAN choice for ortho only
// 0 = formal-TAN
// 1 = simple-TAN
#define SIMPLE_TAN 1

// Source-image sampling from the orthographic scene point.
vec2 sampleOrthoTexcoord(const vec3 world, const WCS wcs, const ProjectionParams projection, const float[6] PV) {
    // Surface maps sample directly from world lon/lat, without observer-image geometry.
    if (isSurfaceMap(projection))
        return sampleSurfaceMapTexcoord(world, wcs, projection, PV);
#if SIMPLE_TAN
    if (projection.projectionCode == WCS_PROJECTION_TAN)
        return wcsPlaneToTexcoord(world.xy - wcs.crval, wcs);
#endif
    vec2 helioprojective = worldToHelioprojective(world, projection.observerDistance);
    return helioprojectiveToTexcoord(helioprojective, wcs, projection, PV);
}

float intersectPlane(const vec4 quat, const vec2 viewPosition, const bool discardBackFacing) {
    vec3 altnormal = rotate_vector(quat, zAxis);
    if (discardBackFacing && altnormal.z <= 0.)
        discard;
    if (abs(altnormal.z) < PLANE_Z_EPS)
        discard;
    return -dot(altnormal.xy, viewPosition) / altnormal.z;
}

vec3 rotateOnDiskPoint(const WCS wcs, const vec3 hitPoint) {
    vec3 rotated = rotate_vector_inverse(wcs.cameraDiff, hitPoint);
    if (wcs.deltaT != 0.)
        rotated = differential(wcs.deltaT, rotated);
    return rotated;
}

void main(void) {
    vec2 viewPosition = getViewPosition();
    bool diffMode = display.isDiff != NODIFFERENCE;
    bool surfaceMapMode = isSurfaceMap(projection[0]);
    bool diffSurfaceMapMode = isSurfaceMap(projection[1]);

    float radius2 = dot(viewPosition, viewPosition);
    bool onDisk = radius2 <= 1.;
    // CAR/CEA have no off-limb representation; wrap only the visible solar sphere.
    if (surfaceMapMode && !onDisk)
        discard;
    if (diffMode && diffSurfaceMapMode && !onDisk)
        discard;

    float enhancementFactor;
    vec3 hitPoint = vec3(0.), rotatedHitPoint = vec3(0.);

    if (onDisk) {
        hitPoint = vec3(viewPosition, sqrt(1. - radius2));
        if (surfaceMapMode) {
            // CAR/CEA stay attached to the visible sphere under drag/view rotation.
            rotatedHitPoint = rotate_vector_inverse(projection[0].sourceViewQuat, hitPoint);
        } else {
            rotatedHitPoint = rotateOnDiskPoint(wcs[0], hitPoint);
        }

        enhancementFactor = 1.;
        gl_FragDepth = 0.5 - hitPoint.z * CLIP_SCALE_NARROW;
    } else {
        enhancementFactor = sqrt(radius2);
        gl_FragDepth = 1.;
    }

    bool didFallback = false;
    // Observer-image projections keep the existing off-limb / back-side fallback.
    if (!surfaceMapMode && rotatedHitPoint.z <= 0.) { // off-limb or back
        hitPoint = vec3(viewPosition, intersectPlane(wcs[0].cameraDiff, viewPosition, onDisk));
        rotatedHitPoint = rotate_vector_inverse(wcs[0].cameraDiff, hitPoint);
        if (onDisk && hitPoint.z < 0.) // differential: off-limb behind sphere
            discard;
        if (dot(rotatedHitPoint, rotatedHitPoint) <= 1.) // differential: central disk
            discard;
        didFallback = true;
    }

    if (didFallback && display.calculateDepth != 0.) // intersecting Euhforia planes
        gl_FragDepth = 0.5 - hitPoint.z * CLIP_SCALE_WIDE;

    clipPlanarMasks(rotatedHitPoint.xy);
    vec2 texCoord = sampleOrthoTexcoord(rotatedHitPoint, wcs[0], projection[0], pv0);

    vec2 diffTexCoord = texCoord;
    if (diffMode) {
        vec3 diffHitPoint = vec3(0.);
        vec3 diffRotatedHitPoint = vec3(0.);

        if (onDisk) {
            diffHitPoint = vec3(viewPosition, sqrt(1. - radius2));
            if (diffSurfaceMapMode) {
                diffRotatedHitPoint = rotate_vector_inverse(projection[1].sourceViewQuat, diffHitPoint);
            } else {
                diffRotatedHitPoint = rotateOnDiskPoint(wcs[1], diffHitPoint);
            }
        }

        if (!diffSurfaceMapMode && diffRotatedHitPoint.z <= 0.) {
            diffHitPoint = vec3(viewPosition, intersectPlane(wcs[1].cameraDiff, viewPosition, onDisk));
            diffRotatedHitPoint = rotate_vector_inverse(wcs[1].cameraDiff, diffHitPoint);
            if (onDisk && diffHitPoint.z < 0.) // differential: off-limb behind sphere
                discard;
            if (dot(diffRotatedHitPoint, diffRotatedHitPoint) <= 1.) // differential: central disk
                discard;
        }

        clipPlanarMasks(diffRotatedHitPoint.xy);
        diffTexCoord = sampleOrthoTexcoord(diffRotatedHitPoint, wcs[1], projection[1], pv1);
    }
    outColor = getColor(texCoord, diffTexCoord, enhancementFactor);
}
