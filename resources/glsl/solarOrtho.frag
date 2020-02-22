
const vec3 zAxis = vec3(0, 0, 1);

vec3 differential(const float dt, const vec3 v) {
    if (dt == 0.)
        return v;

    float phi = atan(v.x, v.z);
    float theta = asin(v.y);
    phi -= differentialRotation(dt, theta); // difference from rigid rotation
    return vec3(cos(theta) * sin(phi), v.y, cos(theta) * cos(phi));
}

vec3 rotate_vector_inverse(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(cross(vec, quat.xyz) + quat.w * vec, quat.xyz);
}

vec3 rotate_vector(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(quat.xyz, cross(quat.xyz, vec) + quat.w * vec);
}

float intersectPlane(const vec4 quat, const vec4 vecin, const bool hideBack) {
    vec3 altnormal = rotate_vector(quat, zAxis);
    if (hideBack && altnormal.z <= 0.)
        discard;
    return -dot(altnormal.xy, vecin.xy) / altnormal.z;
}

void main(void) {
    vec2 normalizedScreenpos = 2. * (gl_FragCoord.xy - viewportOffset) / viewport.xy - 1.;
    vec4 up1 = cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);

    float radius2 = dot(up1.xy, up1.xy);
    bool onDisk = radius2 <= 1.;

    float factor;
    vec3 hitPoint = vec3(0.), rotatedHitPoint = vec3(0.), diffrotatedHitPoint = vec3(0.);

    if (onDisk) {
        hitPoint = vec3(up1.x, up1.y, sqrt(1. - radius2));
        rotatedHitPoint =     differential(deltaT,     rotate_vector_inverse(cameraDifferenceRotationQuat, hitPoint));
        diffrotatedHitPoint = differential(deltaTDiff, rotate_vector_inverse(diffcameraDifferenceRotationQuat, hitPoint));
        factor = 1.;
        gl_FragDepth = 0.5 - hitPoint.z * CLIP_SCALE_NARROW;
    } else {
        factor = sqrt(radius2);
        gl_FragDepth = 1.;
    }

    if (rotatedHitPoint.z <= 0.) { // off-limb or back
        hitPoint = vec3(up1.x, up1.y, intersectPlane(cameraDifferenceRotationQuat, up1, onDisk));
        if (onDisk && hitPoint.z < 0.) // differential: off-limb behind sphere
            discard;

        rotatedHitPoint = rotate_vector_inverse(cameraDifferenceRotationQuat, hitPoint);
        if (length(rotatedHitPoint) <= 1.) // differential: central disk
            discard;

        if (calculateDepth != 0) // intersecting Euhforia planes
            gl_FragDepth = 0.5 - hitPoint.z * CLIP_SCALE_WIDE;
    }

    if (sector[0] != 0.) {
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
