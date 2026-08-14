void main(void) {
    vec2 mapPos = getNormalizedMapPos();
    vec2 helioprojective = normalizedMapToHelioprojective(mapPos);
    bool diffMode = display.isDiff != NODIFFERENCE;
    vec2 hpcXY = helioprojectiveToHpcXY(helioprojective, projection[0].observerDistance);
    float enhancementFactor;
    clipPlanarMasks(hpcXY);
    vec2 texCoord = sampleHpcTexcoord(wcs[0], projection[0], helioprojective, hpcXY, pv0, enhancementFactor);
    if (!diffMode) {
        outColor = getColor(texCoord, texCoord, enhancementFactor);
        return;
    }

    vec2 diffHpcXY = helioprojectiveToHpcXY(helioprojective, projection[1].observerDistance);
    float diffEnhancementFactor;
    clipPlanarMasks(diffHpcXY);
    vec2 diffTexCoord = sampleHpcTexcoord(wcs[1], projection[1], helioprojective, diffHpcXY, pv1, diffEnhancementFactor);
    outColor = getColor(texCoord, diffTexCoord, max(enhancementFactor, diffEnhancementFactor));
}
