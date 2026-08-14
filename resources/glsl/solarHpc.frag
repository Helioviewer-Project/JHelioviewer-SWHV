vec2 normalizedMapToHelioprojective(const vec2 mapPos) {
    return vec2(
        radians(screen.mapBounds.x + mapPos.x * (screen.mapBounds.y - screen.mapBounds.x)),
        radians(screen.mapBounds.z + mapPos.y * (screen.mapBounds.w - screen.mapBounds.z)));
}

vec2 helioprojectiveToHpcXY(const vec2 helioprojective, const float observerDistance) {
    vec3 ray = helioprojectiveToObserverRay(helioprojective);
    if (ray.z >= 0.)
        discard;
    return -observerDistance * ray.xy / ray.z;
}

void main(void) {
    vec2 mapPos = getNormalizedMapPos();
    vec2 helioprojective = normalizedMapToHelioprojective(mapPos);
    bool diffMode = display.isDiff != NODIFFERENCE;
    vec2 hpcXY = helioprojectiveToHpcXY(helioprojective, images[0].observerDistance);
    float enhancementFactor;
    clipPlanarMasks(hpcXY);
    vec2 texCoord = sampleHpcTexcoord(images[0], helioprojective, hpcXY, pv0, enhancementFactor);
    if (!diffMode) {
        outColor = getColor(texCoord, texCoord, enhancementFactor);
        return;
    }

    vec2 diffHpcXY = helioprojectiveToHpcXY(helioprojective, images[1].observerDistance);
    float diffEnhancementFactor;
    clipPlanarMasks(diffHpcXY);
    vec2 diffTexCoord = sampleHpcTexcoord(images[1], helioprojective, diffHpcXY, pv1, diffEnhancementFactor);
    outColor = getColor(texCoord, diffTexCoord, max(enhancementFactor, diffEnhancementFactor));
}
