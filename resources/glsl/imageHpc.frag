void main(void) {
    vec2 mapPos = getNormalizedMapPos();
    if (!insideMap(mapPos))
        discard;
    vec2 angles = radians(mix(screen.mapBounds.xz, screen.mapBounds.yw, mapPos));
    vec3 ray = helioprojectiveToObserverRay(angles);
    renderHpc(ray, ray);
}
