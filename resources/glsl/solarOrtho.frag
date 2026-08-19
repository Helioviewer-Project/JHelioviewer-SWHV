
const vec3 zAxis = vec3(0, 0, 1);
const float PLANE_Z_EPS = 1e-8;

// TAN choice for ortho only
// 0 = formal-TAN
// 1 = simple-TAN
#define SIMPLE_TAN 1

// Source-image sampling from the orthographic scene point.
vec2 sampleOrthoTexcoord(const vec3 world, const Image img, const float[6] PV) {
    // Surface maps sample directly from world lon/lat, without observer-image geometry.
    if (isSurfaceMap(img))
        return sampleSurfaceMapTexcoord(world, img, PV);
#if SIMPLE_TAN
    if (img.projectionCode == WCS_PROJECTION_TAN)
        return wcsPlaneToTexcoord(world.xy - img.crval, img);
#endif
    vec2 helioprojective = worldToHelioprojective(world, img.observerDistance);
    return helioprojectiveToTexcoord(helioprojective, img, PV);
}

float intersectPlane(const vec4 quat, const vec2 viewPosition, const bool discardBackFacing) {
    vec3 altnormal = rotate_vector(quat, zAxis);
    if (discardBackFacing && altnormal.z <= 0.)
        discard;
    if (abs(altnormal.z) < PLANE_Z_EPS)
        discard;
    return -dot(altnormal.xy, viewPosition) / altnormal.z;
}

vec3 rotateOnDiskPoint(const Image img, const vec3 hitPoint) {
    vec3 rotated = rotate_vector_inverse(img.cameraDiff, hitPoint);
    if (img.deltaT != 0.)
        rotated = differential(img.deltaT, rotated);
    return rotated;
}

void main(void) {
    vec2 viewPosition = getViewPosition();
    bool diffMode = display.isDiff != NODIFFERENCE;
    bool surfaceMapMode = isSurfaceMap(images[0]);
    bool diffSurfaceMapMode = isSurfaceMap(images[1]);

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
            rotatedHitPoint = rotate_vector_inverse(images[0].sourceViewQuat, hitPoint);
        } else {
            rotatedHitPoint = rotateOnDiskPoint(images[0], hitPoint);
        }

        enhancementFactor = 1.;
        gl_FragDepth = getDepth(hitPoint.z);
    } else {
        enhancementFactor = sqrt(radius2);
        gl_FragDepth = 1.;
    }

    // Observer-image projections keep the existing off-limb / back-side fallback.
    if (!surfaceMapMode && rotatedHitPoint.z <= 0.) { // off-limb or back
        hitPoint = vec3(viewPosition, intersectPlane(images[0].cameraDiff, viewPosition, onDisk));
        rotatedHitPoint = rotate_vector_inverse(images[0].cameraDiff, hitPoint);
        if (onDisk && hitPoint.z < 0.) // differential: off-limb behind sphere
            discard;
        if (dot(rotatedHitPoint, rotatedHitPoint) <= 1.) // differential: central disk
            discard;
        if (display.calculateDepth != 0.) // intersecting Euhforia planes
            gl_FragDepth = getDepth(hitPoint.z);
    }

    clipPlanarMasks(rotatedHitPoint.xy);
    vec2 texCoord = sampleOrthoTexcoord(rotatedHitPoint, images[0], pv0);

    vec2 diffTexCoord = texCoord;
    if (diffMode) {
        vec3 diffHitPoint = vec3(0.);
        vec3 diffRotatedHitPoint = vec3(0.);

        if (onDisk) {
            diffHitPoint = vec3(viewPosition, sqrt(1. - radius2));
            if (diffSurfaceMapMode) {
                diffRotatedHitPoint = rotate_vector_inverse(images[1].sourceViewQuat, diffHitPoint);
            } else {
                diffRotatedHitPoint = rotateOnDiskPoint(images[1], diffHitPoint);
            }
        }

        if (!diffSurfaceMapMode && diffRotatedHitPoint.z <= 0.) {
            diffHitPoint = vec3(viewPosition, intersectPlane(images[1].cameraDiff, viewPosition, onDisk));
            diffRotatedHitPoint = rotate_vector_inverse(images[1].cameraDiff, diffHitPoint);
            if (onDisk && diffHitPoint.z < 0.) // differential: off-limb behind sphere
                discard;
            if (dot(diffRotatedHitPoint, diffRotatedHitPoint) <= 1.) // differential: central disk
                discard;
        }

        clipPlanarMasks(diffRotatedHitPoint.xy);
        diffTexCoord = sampleOrthoTexcoord(diffRotatedHitPoint, images[1], pv1);
    }
    outColor = getColor(texCoord, diffTexCoord, enhancementFactor);
}
