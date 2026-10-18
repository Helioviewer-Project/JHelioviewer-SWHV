bool hpcMaskAndEnhancement(const vec3 ray, const float observerDistance, out float offDiskFactor) {
    if (ray.z >= 0.) {
        // No forward plane: infinite mask radius and no radial enhancement.
        offDiskFactor = 1.;
        return isinf(display.radii.y) && display.cutOff.z < 0. && insideSectorMasks(ray.xy);
    }
    vec2 hpcXY = -observerDistance * ray.xy / ray.z;
    if (!insidePlanarMasks(hpcXY))
        return false;
    offDiskFactor = display.enhanced == 0. ? 1. : max(1., length(hpcXY));
    return true;
}

void main(void) {
    vec2 mapPos = getNormalizedMapPos();
    if (!insideMap(mapPos))
        discard;
    vec2 angles = radians(mix(screen.mapBounds.xz, screen.mapBounds.yw, mapPos));
    vec3 ray = helioprojectiveToObserverRay(angles);
    float offDiskFactor;
    if (!hpcMaskAndEnhancement(ray, images[0].observerDistance, offDiskFactor))
        discard;
    float diffOffDiskFactor = offDiskFactor;
    if (display.isDiff != NODIFFERENCE) {
        if (!hpcMaskAndEnhancement(ray, images[1].observerDistance, diffOffDiskFactor))
            discard;
    }
    renderHpc(ray, offDiskFactor, ray, diffOffDiskFactor);
}
