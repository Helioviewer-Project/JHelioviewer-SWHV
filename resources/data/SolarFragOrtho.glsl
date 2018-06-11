void main(void) {
    vec2 normalizedScreenpos = 2. * ((gl_FragCoord.xy - viewportOffset) / viewport.xy - 0.5);
    vec4 up1 = cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);
    vec2 texcoord;
    vec2 difftexcoord;
    vec3 hitPoint = vec3(up1.x, up1.y, sqrt(1. - dot(up1.xy, up1.xy)));
    vec3 rotatedHitPoint = rotate_vector_inverse(cameraDifferenceRotationQuat, hitPoint);
    float radius2 = dot(up1.xy, up1.xy);
    if (radius2 >= 1. || dot(rotatedHitPoint.xyz, vec3(0., 0., 1.)) <= 0.) {
        hitPoint = vec3(up1.x, up1.y, intersectPlane(up1));
        rotatedHitPoint = rotate_vector_inverse(cameraDifferenceRotationQuat, hitPoint);
    }

    float factor = 1.;
    if (radius2 >= 1.) {
        factor = sqrt(radius2);
    }

    float geometryFlatDist = abs(dot(rotatedHitPoint, cutOffDirection));
    vec3 cutOffDirectionAlt = vec3(-cutOffDirection.y, cutOffDirection.x, 0.);
    float geometryFlatDistAlt = abs(dot(rotatedHitPoint, cutOffDirectionAlt));
    texcoord = vec2((rotatedHitPoint.x - rect.x) * rect.z, (-rotatedHitPoint.y - rect.y) * rect.w);
    clamp_texcoord(texcoord);

    if (dot(rotatedHitPoint.xy, rotatedHitPoint.xy) > cutOffRadius.y * cutOffRadius.y ||
        dot(rotatedHitPoint.xy, rotatedHitPoint.xy) < cutOffRadius.x * cutOffRadius.x ||
        (cutOffValue >= 0. && (geometryFlatDist > cutOffValue || geometryFlatDistAlt > cutOffValue))) {
        discard;
    }

    if (isdifference != NODIFFERENCE) {
        vec3 diffrotatedHitPoint = rotate_vector_inverse(diffcameraDifferenceRotationQuat, hitPoint);
        if (radius2 >= 1. && dot(diffrotatedHitPoint.xyz, vec3(0., 0., 1.)) <= 0.) {
            hitPoint = vec3(up1.x, up1.y, intersectPlanediff(up1));
            diffrotatedHitPoint = rotate_vector_inverse(diffcameraDifferenceRotationQuat, hitPoint);
        }
        difftexcoord = vec2((diffrotatedHitPoint.x - differencerect.x) * differencerect.z, (-diffrotatedHitPoint.y - differencerect.y) * differencerect.w);
        clamp_texcoord(difftexcoord);

        if (dot(diffrotatedHitPoint.xy, diffrotatedHitPoint.xy) > cutOffRadius.y * cutOffRadius.y ||
            dot(diffrotatedHitPoint.xy, diffrotatedHitPoint.xy) < cutOffRadius.x * cutOffRadius.x) {
            discard;
        }
    }
    gl_FragColor = getColor(texcoord, difftexcoord, factor);
}
