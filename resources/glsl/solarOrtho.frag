void main(void) {
    vec2 normalizedScreenpos = 2. * (gl_FragCoord.xy - viewportOffset) / viewport.xy - 1.;
    vec4 up1 = cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);

    float factor, radius2 = dot(up1.xy, up1.xy);
    vec3 hitPoint = vec3(0.), rotatedHitPoint = vec3(0.);

    if (radius2 < 1.) {
        hitPoint = vec3(up1.x, up1.y, sqrt(1. - radius2));
        rotatedHitPoint = rotate_vector_inverse(cameraDifferenceRotationQuat, hitPoint);
        factor = 1.;
        gl_FragDepth = gl_FragCoord.z;
    } else {
        factor = sqrt(radius2);
        gl_FragDepth = 1.;
    }
    bool onDisk = radius2 <= 1;

    if (rotatedHitPoint.z <= 0.) { // off-limb or back
        hitPoint = vec3(up1.x, up1.y, intersectPlane(cameraDifferenceRotationQuat, up1, onDisk));
        rotatedHitPoint = rotate_vector_inverse(cameraDifferenceRotationQuat, hitPoint);

        if (calculateDepth != 0) // intersecting Eufhoria planes
            gl_FragDepth = 0.5 - hitPoint.z * MAX_DEPTH_FACTOR;
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
        vec3 diffrotatedHitPoint = rotate_vector_inverse(diffcameraDifferenceRotationQuat, hitPoint);
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
