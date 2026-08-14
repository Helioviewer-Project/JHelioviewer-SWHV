vec2 warpHpcXY() {
    vec2 mapPos = getNormalizedMapPos();

    float angle = mapPos.x * TWOPI;
    float radialCoordinate = unwarpRadius(mapPos.y);
    return radialCoordinate * vec2(-sin(angle), cos(angle));
}

void main(void) {
    outColor = sampleWarpedHpcColor(warpHpcXY());
}
