void main(void) {
    vec2 w = getViewPosition();
    float viewRadius = length(w);
    float t = 2. * viewRadius;
    if (t > 1.)
        discard;
    vec2 hpcXY = t == 0. ? vec2(0.) : (unwarpRadius(t) / viewRadius) * w;
    renderWarpedHpc(hpcXY);
}
