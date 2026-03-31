
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

vec2 helioprojectiveToHpcXY(const vec2 helioprojective, const float observerDistance) {
    return helioprojectiveToHpcPlanePoint(helioprojective, observerDistance).xy;
}

float hpcEnhancementFactor(const vec2 hpcXY) {
    return max(1., length(hpcXY));
}

void clipHpcGeometry(const vec2 hpcXY) {
    if (display.sector.z != 0.) {
        float theta = atan(hpcXY.y, hpcXY.x);
        if (theta < display.sector.x || theta > display.sector.y)
            discard;
    }

    float radial2 = dot(hpcXY, hpcXY);
    float minRadius2 = display.radii.x * display.radii.x;
    float maxRadius2 = display.radii.y * display.radii.y;
    if (radial2 > maxRadius2 || radial2 < minRadius2)
        discard;

    if (display.cutOff.z >= 0.) {
        float flatDist = abs(dot(hpcXY, display.cutOff.xy));
        vec2 cutOffAlt = vec2(-display.cutOff.y, display.cutOff.x);
        float flatDistAlt = abs(dot(hpcXY, cutOffAlt));
        if (flatDist > display.cutOff.z || flatDistAlt > display.cutOff.z)
            discard;
    }
}

vec2 sampleHpcTexcoord(const WCS wcs, const ProjectionParams projection, vec2 helioprojective, const vec2 hpcXY, const float dt, const float[6] PV, out float enhancementFactor) {
    enhancementFactor = 1.;
    float observerDistance = projection.observerDistance;

    vec3 world;
    if (helioprojectiveToWorld(helioprojective, observerDistance, world)) {
        if (dt != 0.) {
            vec3 rotatedWorld = differential(dt, world);
            helioprojective = worldToHelioprojective(rotatedWorld, observerDistance);
        }
    } else {
        enhancementFactor = hpcEnhancementFactor(hpcXY);
    }

    vec2 plane = projectHelioprojectiveToWcsPlane(helioprojective, wcs, projection, PV);
    return wcsPlaneToTexcoord(plane, wcs);
}

void main(void) {
    vec4 color;
    vec2 scrpos = getScrPos();
    vec2 helioprojective = screenToHelioprojective(scrpos);
    bool diffMode = display.isDiff != NODIFFERENCE;
    float observerDistance = projection[0].observerDistance;
    vec2 hpcXY = helioprojectiveToHpcXY(helioprojective, observerDistance);
    vec2 texCoord;
    float enhancementFactor;
    clipHpcGeometry(hpcXY);
    texCoord = sampleHpcTexcoord(wcs[0], projection[0], helioprojective, hpcXY, wcs[0].deltaT, pv0, enhancementFactor);
    if (!diffMode) {
        color = getColor(texCoord, texCoord, enhancementFactor);
    } else {
        float diffObserverDistance = projection[1].observerDistance;
        vec2 diffHpcXY = helioprojectiveToHpcXY(helioprojective, diffObserverDistance);
        vec2 diffTexCoord;
        float diffEnhancementFactor;
        clipHpcGeometry(diffHpcXY);
        diffTexCoord = sampleHpcTexcoord(wcs[1], projection[1], helioprojective, diffHpcXY, wcs[1].deltaT, pv1, diffEnhancementFactor);
        color = getColor(texCoord, diffTexCoord, max(enhancementFactor, diffEnhancementFactor));
    }
    outColor = color;
}
