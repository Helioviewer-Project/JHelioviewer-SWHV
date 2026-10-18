void main(void) {
    vec2 mapPos = getNormalizedMapPos();
    if (!insideMap(mapPos))
        discard;
    vec2 direction = sinCos(mapPos.x * TWOPI);
    vec2 hpcXY = unwarpRadius(mapPos.y) * vec2(-direction.x, direction.y);
    renderWarpedHpc(hpcXY);
}
