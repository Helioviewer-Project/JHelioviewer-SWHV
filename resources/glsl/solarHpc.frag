vec2 screenToHelioprojective(const vec2 scrpos) {
    return vec2(
        radians(screen.xStart + scrpos.x * (screen.xStop - screen.xStart)),
        radians(screen.yStart + scrpos.y * (screen.yStop - screen.yStart)));
}

bool helioprojectiveToWorld(const vec2 helioprojective, const float observerDistance, out vec3 world) {
    vec3 ray = helioprojectiveToObserverRay(helioprojective);
    float b = observerDistance * ray.z;
    float c = observerDistance * observerDistance - 1.;
    vec3 observer = observerPosition(observerDistance);
    float discriminant = b * b - c;
    if (discriminant < 0.) {
        world = vec3(0.);
        return false;
    }

    float root = sqrt(discriminant);
    float tNear = -b - root;
    float tFar = -b + root;
    float t = tNear > 0. ? tNear : tFar;
    if (t <= 0.) {
        world = vec3(0.);
        return false;
    }

    world = observer + t * ray;
    return true;
}

float hpcEnhancementFactor(const vec2 helioprojective, const float observerDistance) {
    return max(1., length(helioprojectiveToHpcPlanePoint(helioprojective, observerDistance).xy));
}

vec2 get_hpc_texcoord(const WCS wcs, const vec2 scrpos, const float dt, const float[6] PV, out float factor) {
    vec2 helioprojective = screenToHelioprojective(scrpos);
    factor = 1.;

    vec3 world;
    if (helioprojectiveToWorld(helioprojective, wcs.projectionMeta.z, world)) {
        if (dt != 0.) {
            vec3 rotatedWorld = differential(dt, world);
            helioprojective = worldToHelioprojective(rotatedWorld, wcs.projectionMeta.z);
        }
    } else {
        factor = hpcEnhancementFactor(helioprojective, wcs.projectionMeta.z);
    }

    vec2 plane = projectHelioprojectiveToWcsPlane(helioprojective, wcs, PV);
    return wcsPlaneToTexcoord(plane, wcs);
}

void main(void) {
    vec4 color;
    vec2 scrpos = getScrPos();
    vec2 texcoord;
    float factor;
    texcoord = get_hpc_texcoord(wcs[0], scrpos, wcs[0].deltaT, pv0, factor);
    if (display.isDiff == NODIFFERENCE) {
        clamp_texture(texcoord);
        color = getColor(texcoord, texcoord, factor);
    } else {
        vec2 difftexcoord;
        float difffactor;
        difftexcoord = get_hpc_texcoord(wcs[1], scrpos, wcs[1].deltaT, pv1, difffactor);
        clamp_texture(texcoord);
        clamp_texture(difftexcoord);
        color = getColor(texcoord, difftexcoord, max(factor, difffactor));
    }
    outColor = color;
}
