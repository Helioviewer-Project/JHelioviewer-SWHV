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
