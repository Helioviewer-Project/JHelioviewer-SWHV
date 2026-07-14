void main(void) {
    vec4 color;
    vec2 scrpos = getScrPos();

    float angle = scrpos.x * TWOPI;
    float radialCoordinate = radialCoordinateFromNormalizedRadius(scrpos.y);
    vec2 hpcXY = radialCoordinate * vec2(-sin(angle), cos(angle));
    vec2 helioprojective = hpcXYToHelioprojective(hpcXY, projection[0].observerDistance);
    float enhancementFactor;
    bool diffMode = display.isDiff != NODIFFERENCE;
    clipHpcGeometry(hpcXY);
    vec2 texCoord = sampleHpcTexcoord(wcs[0], projection[0], helioprojective, hpcXY, wcs[0].deltaT, pv0, enhancementFactor);
    if (!diffMode) {
        color = getColor(texCoord, texCoord, enhancementFactor);
    } else {
        vec2 diffHelioprojective = hpcXYToHelioprojective(hpcXY, projection[1].observerDistance);
        float diffEnhancementFactor;
        vec2 diffTexCoord = sampleHpcTexcoord(wcs[1], projection[1], diffHelioprojective, hpcXY, wcs[1].deltaT, pv1, diffEnhancementFactor);
        color = getColor(texCoord, diffTexCoord, max(enhancementFactor, diffEnhancementFactor));
    }
    outColor = color;
}
