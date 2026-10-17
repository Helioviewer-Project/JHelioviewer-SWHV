
const vec3 zAxis = vec3(0, 0, 1);
const float PLANE_Z_EPS = 1e-8;

// Source-image sampling from the orthographic scene point.
vec2 sampleOrthoTexcoord(const vec3 world, const Image img, const float[6] PV) {
    // Surface maps sample directly from world lon/lat, without observer-image geometry.
    if (isSurfaceMap(img))
        return sampleSurfaceMapTexcoord(world, img, PV);
    // Preserve the simple-TAN display policy; its scaled CRVAL is in the affine offset.
    if (img.projectionCode == WCS_PROJECTION_TAN)
        return wcsPlaneToTexcoord(world.xy, img);
    return observerToTexcoord(world, img, PV);
}

vec3 intersectImagePlane(const vec4 quat, const vec2 viewPosition, const bool onDisk, out vec3 hitPoint) {
    vec3 altnormal = rotate_vector(quat, zAxis);
    if (onDisk && altnormal.z <= 0.)
        discard;
    if (abs(altnormal.z) < PLANE_Z_EPS)
        discard;
    hitPoint = vec3(viewPosition, -dot(altnormal.xy, viewPosition) / altnormal.z);
    vec3 rotatedHitPoint = rotate_vector_inverse(quat, hitPoint);
    if (onDisk && hitPoint.z < 0.) // differential: off-limb behind sphere
        discard;
    if (dot(rotatedHitPoint, rotatedHitPoint) <= 1.) // differential: central disk
        discard;
    return rotatedHitPoint;
}

vec3 sourceOnDiskPoint(const Image img, const vec3 hitPoint) {
    // Surface maps stay attached to the visible sphere under drag/view rotation.
    if (isSurfaceMap(img))
        return rotate_vector_inverse(img.sourceViewQuat, hitPoint);
    vec3 rotated = rotate_vector_inverse(img.cameraDiff, hitPoint);
    if (img.deltaT != 0.)
        rotated = differentialInSourceFrame(img, rotated);
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
    vec3 spherePoint = vec3(0.), rotatedHitPoint = vec3(0.);

    if (onDisk) {
        spherePoint = vec3(viewPosition, sqrt(1. - radius2));
        rotatedHitPoint = sourceOnDiskPoint(images[0], spherePoint);

        enhancementFactor = 1.;
        gl_FragDepth = getDepth(spherePoint.z);
    } else {
        enhancementFactor = sqrt(radius2);
        gl_FragDepth = 1.;
    }

    // Observer-image projections keep the existing off-limb / back-side fallback.
    if (!surfaceMapMode && rotatedHitPoint.z <= 0.) { // off-limb or back
        vec3 hitPoint;
        rotatedHitPoint = intersectImagePlane(images[0].cameraDiff, viewPosition, onDisk, hitPoint);
        if (display.calculateDepth != 0.) // intersecting Euhforia planes
            gl_FragDepth = getDepth(hitPoint.z);
    }

    clipPlanarMasks(rotatedHitPoint.xy);
    vec2 texCoord = sampleOrthoTexcoord(rotatedHitPoint, images[0], pv0);

    vec2 diffTexCoord = texCoord;
    if (diffMode) {
        vec3 diffRotatedHitPoint = vec3(0.);

        if (onDisk)
            diffRotatedHitPoint = sourceOnDiskPoint(images[1], spherePoint);

        if (!diffSurfaceMapMode && diffRotatedHitPoint.z <= 0.) {
            vec3 diffHitPoint;
            diffRotatedHitPoint = intersectImagePlane(images[1].cameraDiff, viewPosition, onDisk, diffHitPoint);
        }

        clipPlanarMasks(diffRotatedHitPoint.xy);
        diffTexCoord = sampleOrthoTexcoord(diffRotatedHitPoint, images[1], pv1);
    }
    outColor = getColor(texCoord, diffTexCoord, enhancementFactor);
}
