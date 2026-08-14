void main(void) {
    vec2 w = getViewPosition();
    float viewRadius = length(w);
    float t = 2. * viewRadius;
    if (t > 1. || t == 0.)
        discard;

    float angle = atan(-w.x, w.y);
    if (angle < 0.)
        angle += TWOPI;
    clamp_coord(vec2(angle / TWOPI, t));

    float radialCoordinate = unwarpRadius(t);
    vec2 hpcXY = (radialCoordinate / viewRadius) * w;
    outColor = sampleWarpedHpcColor(hpcXY);
}
