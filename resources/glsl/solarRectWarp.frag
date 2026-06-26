
// Rectangular (angle x radius) unwrap, but the radial axis follows the
// PowerDisk/RadialWarp Box-Cox transform driven by screen.yParam (p): linear inside the limb,
// power-law outside, so one p slider spans inverse / linear / logarithmic for this layout too.

vec2 sampleRectWarpTexcoord(const vec2 crval, const vec4 crota, const vec4 rect, const vec2 scrpos, const float radialCoordinate) {
    if (radialCoordinate > display.radii.y || radialCoordinate < display.radii.x)
        discard;

    // Polar map convention: 0 at north, increasing anti-clockwise.
    float angle = scrpos.x * TWOPI;
    vec2 polarXY = vec2(-sin(angle), -cos(angle)) * radialCoordinate;

    if (display.cutOff.z >= 0.) {
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
    vec2 scrpos = getScrPos();
    // Match PowerMapScale: linear inside the limb (scaled <= 1), power-law outside.
    // screen.yParam = p; p == 0 is the exact logarithmic endpoint.
    float scaled = screen.yStart + scrpos.y * (screen.yStop - screen.yStart);
    float p = screen.yParam;
    float radialCoordinate = scaled <= 1. ? scaled
            : (p == 0. ? exp(scaled - 1.) : pow(1. + p * (scaled - 1.), 1. / p));
    float enhancementFactor = max(1., radialCoordinate);
    bool diffMode = display.isDiff != NODIFFERENCE;
    vec2 texCoord = sampleRectWarpTexcoord(wcs[0].crval, wcs[0].crota, wcs[0].rect, scrpos, radialCoordinate);
    if (!diffMode) {
        color = getColor(texCoord, texCoord, enhancementFactor);
    } else {
        vec2 diffTexCoord = sampleRectWarpTexcoord(wcs[1].crval, wcs[1].crota, wcs[1].rect, scrpos, radialCoordinate);
        color = getColor(texCoord, diffTexCoord, enhancementFactor);
    }
    outColor = color;
}
