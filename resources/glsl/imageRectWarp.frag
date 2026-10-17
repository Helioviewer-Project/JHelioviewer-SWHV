vec2 warpHpcXY() {
    vec2 mapPos = getNormalizedMapPos();

    float angle = mapPos.x * TWOPI;
    float radialCoordinate = unwarpRadius(mapPos.y);
    vec2 direction = sinCos(angle);
    return radialCoordinate * vec2(-direction.x, direction.y);
}

void main(void) {
    outColor = sampleWarpedHpcColor(warpHpcXY());
}
