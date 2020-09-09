
const vec3 zAxis = vec3(0, 0, 1);

vec3 differential(const float dt, const vec3 v) {
    if (dt == 0.)
        return v;

    float phi = atan(v.x, v.z);
    float theta = asin(v.y);
    phi -= differentialRotation(dt, theta); // difference from rigid rotation
    return vec3(cos(theta) * sin(phi), v.y, cos(theta) * cos(phi));
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
    vec3 hitPoint = vec3(0.), rotatedHitPoint = vec3(0.), diffRotatedHitPoint = vec3(0.);
    vec3 centeredHitPoint = vec3(0.), diffCenteredHitPoint = vec3(0.);

    if (onDisk) {
        hitPoint = vec3(up1.x, up1.y, sqrt(1. - radius2));
        rotatedHitPoint      = differential(deltaT[0], rotate_vector_inverse(cameraDifference[0], hitPoint));
        centeredHitPoint     = apply_center(rotatedHitPoint, crval[0], crota[0]);

        diffRotatedHitPoint  = differential(deltaT[1], rotate_vector_inverse(cameraDifference[1], hitPoint));
        diffCenteredHitPoint = apply_center(diffRotatedHitPoint, crval[1], crota[1]);

        factor = 1.;
        gl_FragDepth = 0.5 - hitPoint.z * CLIP_SCALE_NARROW;
    } else {
        factor = sqrt(radius2);
        gl_FragDepth = 1.;
    }

    if (rotatedHitPoint.z <= 0.) { // off-limb or back
        hitPoint = vec3(up1.x, up1.y, intersectPlane(cameraDifference[0], up1, onDisk));
        if (onDisk && hitPoint.z < 0.) // differential: off-limb behind sphere
            discard;

        rotatedHitPoint = rotate_vector_inverse(cameraDifference[0], hitPoint);
        if (length(rotatedHitPoint) <= 1.) // differential: central disk
            discard;

        centeredHitPoint = apply_center(rotatedHitPoint, crval[0], crota[0]);

        if (calculateDepth != 0) // intersecting Euhforia planes
            gl_FragDepth = 0.5 - hitPoint.z * CLIP_SCALE_WIDE;
    }

    if (sector[0] != 0.) {
        float theta = atan(centeredHitPoint.y, centeredHitPoint.x);
        if (theta < sector[1] || theta > sector[2])
            discard;
    }

    vec2 texcoord = rect.zw * vec2(centeredHitPoint.x - rect.x, -centeredHitPoint.y - rect.y);
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
        if (/*radius2 >= 1. ||*/ diffRotatedHitPoint.z <= 0.) {
            hitPoint = vec3(up1.x, up1.y, intersectPlane(cameraDifference[1], up1, onDisk));
            diffRotatedHitPoint  = rotate_vector_inverse(cameraDifference[1], hitPoint);
            diffCenteredHitPoint = apply_center(diffRotatedHitPoint, crval[1], crota[1]);
        }

        difftexcoord = differencerect.zw * vec2(diffCenteredHitPoint.x - differencerect.x, -diffCenteredHitPoint.y - differencerect.y);
        clamp_coord(difftexcoord);

        float diffRotatedHitPointRad = length(diffRotatedHitPoint.xy);
        if (diffRotatedHitPointRad > radii[1] || diffRotatedHitPointRad < radii[0]) {
            discard;
        }
    }
    outColor = getColor(texcoord, difftexcoord, factor);
}
