
vec2 sampleDiskTexcoord(const vec2 crval, const vec4 crota, const vec4 rect, const vec2 w, const float t, const float radialCoordinate) {
    if (radialCoordinate > display.radii.y || radialCoordinate < display.radii.x)
        discard;

    // The disk shows the image plane with the radius remapped; same polar map
    // convention as solarRectWarp.frag, 0 at north and increasing anti-clockwise.
    // This basis must stay consistent with the Java-side non-ortho projection after
    // the subsequent rotate_plane_inverse(..., vec2(pos.x, -pos.y) - crval) step.
    vec2 polarXY = (2. * radialCoordinate / t) * vec2(w.x, -w.y);

    if (display.cutOff.z >= 0.) {
        // Convert the polar north-up basis to the display-plane x/y basis used by cutOff.
        vec2 displayXY = polarXY.yx;
        vec2 cutOffAlt = vec2(-display.cutOff.y, display.cutOff.x);
        float geometryFlatDist = abs(dot(displayXY, display.cutOff.xy));
        float geometryFlatDistAlt = abs(dot(displayXY, cutOffAlt));
        if (geometryFlatDist > display.cutOff.z || geometryFlatDistAlt > display.cutOff.z)
            discard;
    }

    vec2 centered = rotate_plane_inverse(crota, vec2(polarXY.x, -polarXY.y) - crval);
    vec2 texCoord = rect.zw * vec2(centered.x - rect.x, -centered.y - rect.y);
    clamp_texture(texCoord);
    return texCoord;
}

void main(void) {
    vec4 color;
    vec2 w = getDiskPos();
    float t = 2. * length(w);
    if (t > 1. || t == 0.)
        discard;
    float angle = atan(-w.x, w.y);
    if (angle < 0.)
        angle += TWOPI;
    clamp_coord(vec2(angle / TWOPI, t));
    // Match PowerMapScale: linear inside the limb (scaled <= 1), power-law outside.
    // screen.yParam = p; p == 0 is the exact logarithmic endpoint.
    float scaled = screen.yStart + t * (screen.yStop - screen.yStart);
    float p = screen.yParam;
    float radialCoordinate = scaled <= 1. ? scaled
            : (p == 0. ? exp(scaled - 1.) : pow(1. + p * (scaled - 1.), 1. / p));
    float enhancementFactor = max(1., radialCoordinate);
    bool diffMode = display.isDiff != NODIFFERENCE;
    vec2 texCoord = sampleDiskTexcoord(wcs[0].crval, wcs[0].crota, wcs[0].rect, w, t, radialCoordinate);
    if (!diffMode) {
        color = getColor(texCoord, texCoord, enhancementFactor);
    } else {
        vec2 diffTexCoord = sampleDiskTexcoord(wcs[1].crval, wcs[1].crota, wcs[1].rect, w, t, radialCoordinate);
        color = getColor(texCoord, diffTexCoord, enhancementFactor);
    }
    outColor = color;
}
