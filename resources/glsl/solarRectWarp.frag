void main(void) {
    vec2 mapPos = getNormalizedMapPos();

    float angle = mapPos.x * TWOPI;
    float radialCoordinate = unwarpRadius(mapPos.y);
    vec2 hpcXY = radialCoordinate * vec2(-sin(angle), cos(angle));
    outColor = sampleWarpedHpcColor(hpcXY);
}
