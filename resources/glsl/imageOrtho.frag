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

// Sphere points below the source horizon fall back to the image plane.
bool orthoSample(const Image img, const vec2 viewPosition, const vec3 spherePoint, const bool onDisk,
                 out vec2 texcoord, out float depth) {
    depth = onDisk ? getDepth(spherePoint.z) : 1.;
    vec3 point;
    if (onDisk) {
        point = differentialInImageFrame(img, rotate_vector_inverse(img.imageToView, spherePoint));
        if (point.z >= img.minSurfaceZ)
            return insidePlanarMasks(point.xy) && imageTexcoord(imageDirection(img, point), img, texcoord);
    }
    vec3 hitPoint;
    if (!intersectImagePlane(img.imageToView, viewPosition, onDisk, point, hitPoint))
        return false;
    if (display.calculateDepth != 0.)
        depth = getDepth(hitPoint.z);
    return insidePlanarMasks(point.xy) && imageTexcoord(imageDirection(img, point), img, texcoord);
}

void main(void) {
    vec2 viewPosition = getViewPosition();
    float radius2 = dot(viewPosition, viewPosition);
    bool onDisk = radius2 <= 1.;
    vec3 spherePoint = onDisk ? vec3(viewPosition, sqrt(1. - radius2)) : vec3(0.);
    vec2 texCoord;
    float depth;
    if (!orthoSample(images[0], viewPosition, spherePoint, onDisk, texCoord, depth))
        discard;
    vec2 diffTexCoord = texCoord;
    if (display.isDiff != NODIFFERENCE) {
        float diffDepth;
        if (!orthoSample(images[1], viewPosition, spherePoint, onDisk, diffTexCoord, diffDepth))
            discard;
    }
    gl_FragDepth = depth;
    renderImage(texCoord, diffTexCoord, onDisk ? 1. : sqrt(radius2));
}
