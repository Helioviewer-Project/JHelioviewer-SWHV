const vec3 zAxis = vec3(0, 0, 1);
const float PLANE_Z_EPS = 1e-8;

bool intersectImagePlane(const vec4 quat, const vec2 viewPosition, const bool onDisk,
                         out vec3 sourcePoint, out vec3 hitPoint) {
    vec3 normal = rotate_vector(quat, zAxis);
    if ((onDisk && normal.z <= 0.) || abs(normal.z) < PLANE_Z_EPS)
        return false;
    hitPoint = vec3(viewPosition, -dot(normal.xy, viewPosition) / normal.z);
    sourcePoint = rotate_vector_inverse(quat, hitPoint);
    return !(onDisk && hitPoint.z < 0.) && dot(sourcePoint, sourcePoint) > 1.;
}

// Surface maps use solar-world coordinates; observer images use their source observer's frame.
bool orthoSample(const Image img, const vec2 viewPosition, const vec3 spherePoint, const bool onDisk,
                 const float[6] PV, out vec2 texcoord, out float depth) {
    depth = onDisk ? getDepth(spherePoint.z) : 1.;
    if (isSurfaceMap(img)) {
        if (!onDisk || !insidePlanarMasks(viewPosition))
            return false;
        return imageTexcoord(rotate_vector_inverse(img.sourceViewQuat, spherePoint), img, PV, texcoord);
    }

    bool simpleTan = img.projectionCode == WCS_PROJECTION_TAN;
    vec3 point;
    if (onDisk) {
        point = rotate_vector_inverse(img.cameraDiff, spherePoint);
        if (img.deltaT != 0.)
            point = differentialInSourceFrame(img, point);
    }
    // Simple-TAN uses parallel rays; other observer projections use finite distance.
    if (!onDisk || (simpleTan ? point.z <= 0. : point.z < 1. / img.observerDistance)) {
        vec3 hitPoint;
        if (!intersectImagePlane(img.cameraDiff, viewPosition, onDisk, point, hitPoint))
            return false;
        if (display.calculateDepth != 0.)
            depth = getDepth(hitPoint.z);
    }
    if (!insidePlanarMasks(point.xy))
        return false;
    // Simple-TAN uses the scene plane; the binder includes its scaled CRVAL offset.
    if (simpleTan) {
        texcoord = wcsPlaneToTexcoord(point.xy, img);
        return insideImage(texcoord);
    }
    return imageTexcoord(vec3(point.xy, img.observerDistance - point.z), img, PV, texcoord);
}

void main(void) {
    vec2 viewPosition = getViewPosition();
    float radius2 = dot(viewPosition, viewPosition);
    bool onDisk = radius2 <= 1.;
    vec3 spherePoint = onDisk ? vec3(viewPosition, sqrt(1. - radius2)) : vec3(0.);
    vec2 texCoord;
    float depth;
    if (!orthoSample(images[0], viewPosition, spherePoint, onDisk, pv0, texCoord, depth))
        discard;
    vec2 diffTexCoord = texCoord;
    if (display.isDiff != NODIFFERENCE) {
        float diffDepth;
        if (!orthoSample(images[1], viewPosition, spherePoint, onDisk, pv1, diffTexCoord, diffDepth))
            discard;
    }
    gl_FragDepth = depth;
    renderImage(texCoord, diffTexCoord, onDisk ? 1. : sqrt(radius2));
}
