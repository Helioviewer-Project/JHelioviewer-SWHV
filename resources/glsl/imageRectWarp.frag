void main(void) {
    vec2 mapPos = getNormalizedMapPos();
    if (!insideMap(mapPos))
        discard;
    vec2 direction = sinCos(mapPos.x * TWOPI);
    vec2 hpcXY = unwarpRadius(mapPos.y) * vec2(-direction.x, direction.y);
    renderHpc(vec3(hpcXY, -images[0].observerDistance), vec3(hpcXY, -images[1].observerDistance));
}
