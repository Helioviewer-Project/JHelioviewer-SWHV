vec2 normalizedMapToHelioprojective(const vec2 mapPos) {
    return vec2(
        radians(screen.mapBounds.x + mapPos.x * (screen.mapBounds.y - screen.mapBounds.x)),
        radians(screen.mapBounds.z + mapPos.y * (screen.mapBounds.w - screen.mapBounds.z)));
}

vec2 observerRayToHpcXY(const vec3 ray, const float observerDistance) {
    // Wide-angle images can extend beyond the Sun-plane horizon.
    if (ray.z == 0.)
        discard;
    return -observerDistance * ray.xy / ray.z;
}

void main(void) {
    vec2 mapPos = getNormalizedMapPos();
    vec2 helioprojective = normalizedMapToHelioprojective(mapPos);
    vec3 ray = helioprojectiveToObserverRay(helioprojective);
    bool diffMode = display.isDiff != NODIFFERENCE;
    vec2 hpcXY = observerRayToHpcXY(ray, images[0].observerDistance);
    float enhancementFactor;
    clipPlanarMasks(hpcXY);
    vec2 texCoord = sampleHpcTexcoord(images[0], ray, hpcXY, pv0, enhancementFactor);
    if (!diffMode) {
        outColor = getColor(texCoord, texCoord, enhancementFactor);
        return;
    }

    vec2 diffHpcXY = observerRayToHpcXY(ray, images[1].observerDistance);
    float diffEnhancementFactor;
    clipPlanarMasks(diffHpcXY);
    vec2 diffTexCoord = sampleHpcTexcoord(images[1], ray, diffHpcXY, pv1, diffEnhancementFactor);
    outColor = getColor(texCoord, diffTexCoord, max(enhancementFactor, diffEnhancementFactor));
}
