
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

vec2 distort(const vec2 c, float mju) {
    if (mju == 0)
        return c;

    clamp_coord(c);
    vec2 v = c - 0.5;
    float R = length(v);
    float Z = cos(asin(R));
    float Ra = (mju + 1) / (Z + mju);
    return vec2(v.x * Ra, v.y * Ra) + 0.5;
}

void main(void) {
    vec2 normalizedScreenpos = 2. * (gl_FragCoord.xy - screen.viewport.xy) / screen.viewport.zw - 1.;
    vec4 up1 = screen.inverseMVP * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.);

    float radius2 = dot(up1.xy, up1.xy);
    bool onDisk = radius2 <= 1.;

    float factor;
    vec3 hitPoint = vec3(0.), rotatedHitPoint = vec3(0.), diffRotatedHitPoint = vec3(0.);
    vec3 centeredHitPoint = vec3(0.), diffCenteredHitPoint = vec3(0.);

    if (onDisk) {
        hitPoint = vec3(up1.x, up1.y, sqrt(1. - radius2));
        rotatedHitPoint      = differential(wcs[0].deltaT, rotate_vector_inverse(wcs[0].cameraDiff, hitPoint));
        centeredHitPoint     = apply_center(rotatedHitPoint, wcs[0].crval, wcs[0].crota);

        if (display.isDiff != NODIFFERENCE) {
            diffRotatedHitPoint  = differential(wcs[1].deltaT, rotate_vector_inverse(wcs[1].cameraDiff, hitPoint));
            diffCenteredHitPoint = apply_center(diffRotatedHitPoint, wcs[1].crval, wcs[1].crota);
        }

        factor = 1.;
        gl_FragDepth = 0.5 - hitPoint.z * CLIP_SCALE_NARROW;
    } else {
        factor = sqrt(radius2);
        gl_FragDepth = 1.;
    }

    if (rotatedHitPoint.z <= 0.) { // off-limb or back
        hitPoint = vec3(up1.x, up1.y, intersectPlane(wcs[0].cameraDiff, up1, onDisk));
        if (onDisk && hitPoint.z < 0.) // differential: off-limb behind sphere
            discard;

        rotatedHitPoint = rotate_vector_inverse(wcs[0].cameraDiff, hitPoint);
        if (length(rotatedHitPoint) <= 1.) // differential: central disk
            discard;

        centeredHitPoint = apply_center(rotatedHitPoint, wcs[0].crval, wcs[0].crota);

        if (display.calculateDepth != 0) // intersecting Euhforia planes
            gl_FragDepth = 0.5 - hitPoint.z * CLIP_SCALE_WIDE;
    }

    if (display.sector.z != 0.) {
        float theta = atan(centeredHitPoint.y, centeredHitPoint.x);
        if (theta < display.sector.x || theta > display.sector.y)
            discard;
    }

    vec4 rect = wcs[0].rect;
    vec2 texcoord = distort(rect.zw * vec2(centeredHitPoint.x - rect.x, -centeredHitPoint.y - rect.y), wcs[0].pv);
    clamp_coord(texcoord);

    float geometryFlatDist = abs(dot(rotatedHitPoint.xy, display.cutOff.xy));
    vec2 cutOffAlt = vec2(-display.cutOff.y, display.cutOff.x);
    float geometryFlatDistAlt = abs(dot(rotatedHitPoint.xy, cutOffAlt));

    float rotatedHitPointRad = length(rotatedHitPoint.xy);
    if (rotatedHitPointRad > display.radii.y || rotatedHitPointRad < display.radii.x ||
        (display.cutOff.z >= 0. && (geometryFlatDist > display.cutOff.z || geometryFlatDistAlt > display.cutOff.z))) {
        discard;
    }

    vec2 difftexcoord;
    if (display.isDiff != NODIFFERENCE) {
        if (/*radius2 >= 1. ||*/ diffRotatedHitPoint.z <= 0.) {
            hitPoint = vec3(up1.x, up1.y, intersectPlane(wcs[1].cameraDiff, up1, onDisk));
            diffRotatedHitPoint  = rotate_vector_inverse(wcs[1].cameraDiff, hitPoint);
            diffCenteredHitPoint = apply_center(diffRotatedHitPoint, wcs[1].crval, wcs[1].crota);
        }

        vec4 rect = wcs[1].rect;
        difftexcoord = distort(rect.zw * vec2(diffCenteredHitPoint.x - rect.x, -diffCenteredHitPoint.y - rect.y), wcs[1].pv);
        clamp_coord(difftexcoord);

        float diffRotatedHitPointRad = length(diffRotatedHitPoint.xy);
        if (diffRotatedHitPointRad > display.radii.y || diffRotatedHitPointRad < display.radii.x) {
            discard;
        }
    }
    outColor = getColor(texcoord, difftexcoord, factor);
}
