
#define testDeltaT (3 * 86400. * 1e-6) // usec

vec3 differential(vec3 p, float deltaT) {
    if (deltaT == 0)
        return p;

    float phi = atan(p.x, p.z);
    float theta = acos(p.y);

    float sin2l = sin(theta);
    sin2l *= sin2l;
    float sin4l = sin2l * sin2l;
    phi -= deltaT * (0.343 * sin2l + 0.474 * sin4l);

    return vec3(sin(theta) * sin(phi), cos(theta), sin(theta) * cos(phi));
}

float intersectPlane(const vec4 quat, const vec4 vecin, bool hideBack) {
    vec3 altnormal = rotate_vector(quat, vec3(0., 0., 1.));
    if (hideBack && altnormal.z <= 0.)
        discard;
    return -dot(altnormal.xy, vecin.xy) / altnormal.z;
}

void main(void) {
    vec2 normalizedScreenpos = 2. * (gl_FragCoord.xy - viewportOffset) / viewport.xy - 1.;
    vec4 up1 = cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);

    float radius2 = dot(up1.xy, up1.xy);
    bool onDisk = radius2 <= 1;

    float factor;
    vec3 hitPoint = vec3(0.), rotatedHitPoint = vec3(0.), diffrotatedHitPoint = vec3(0.);;

    if (onDisk) {
        hitPoint = vec3(up1.x, up1.y, sqrt(1. - radius2));
        rotatedHitPoint = differential(rotate_vector_inverse(cameraDifferenceRotationQuat, hitPoint), 0);
        diffrotatedHitPoint = differential(rotate_vector_inverse(diffcameraDifferenceRotationQuat, hitPoint), 0);
        factor = 1.;
        gl_FragDepth = 0.5 - hitPoint.z * CLIP_SCALE_NARROW;
    } else {
        factor = sqrt(radius2);
        gl_FragDepth = 1.;
    }

    if (rotatedHitPoint.z <= 0.) { // off-limb or back
        hitPoint = vec3(up1.x, up1.y, intersectPlane(cameraDifferenceRotationQuat, up1, onDisk));
        rotatedHitPoint = rotate_vector_inverse(cameraDifferenceRotationQuat, hitPoint);

        if (length(rotatedHitPoint) <= 1) // central disk
            discard;

        if (calculateDepth != 0) // intersecting Eufhoria planes
            gl_FragDepth = 0.5 - hitPoint.z * CLIP_SCALE_WIDE;
    }

    if (sector[0] != 0) {
        float theta = atan(rotatedHitPoint.y, rotatedHitPoint.x);
        if (theta < sector[1] || theta > sector[2])
            discard;
    }

    vec2 texcoord = vec2((rotatedHitPoint.x - rect.x) * rect.z, (-rotatedHitPoint.y - rect.y) * rect.w);
    clamp_coord(texcoord);

    float geometryFlatDist = abs(dot(rotatedHitPoint.xy, cutOffDirection));
    vec2 cutOffDirectionAlt = vec2(-cutOffDirection.y, cutOffDirection.x);
    float geometryFlatDistAlt = abs(dot(rotatedHitPoint.xy, cutOffDirectionAlt));

    float rotatedHitPointRad = length(rotatedHitPoint.xy);
    if (rotatedHitPointRad > radii[1] || rotatedHitPointRad < radii[0] ||
        (cutOffValue >= 0. && (geometryFlatDist > cutOffValue || geometryFlatDistAlt > cutOffValue))) {
        discard;
    }

    vec2 difftexcoord;
    if (isdifference != NODIFFERENCE) {
        if (/*radius2 >= 1. ||*/ diffrotatedHitPoint.z <= 0.) {
            hitPoint = vec3(up1.x, up1.y, intersectPlane(diffcameraDifferenceRotationQuat, up1, onDisk));
            diffrotatedHitPoint = rotate_vector_inverse(diffcameraDifferenceRotationQuat, hitPoint);
        }

        difftexcoord = vec2((diffrotatedHitPoint.x - differencerect.x) * differencerect.z, (-diffrotatedHitPoint.y - differencerect.y) * differencerect.w);
        clamp_coord(difftexcoord);

        float diffrotatedHitPointRad = length(diffrotatedHitPoint.xy);
        if (diffrotatedHitPointRad > radii[1] || diffrotatedHitPointRad < radii[0]) {
            discard;
        }
    }
    outColor = getColor(texcoord, difftexcoord, factor);
}
