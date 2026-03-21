
vec2 samplePolarTexcoord(const vec2 crval, const vec4 crota, const vec4 rect, const vec2 scrpos, const float radialCoordinate) {
    if (radialCoordinate > display.radii.y || radialCoordinate < display.radii.x)
        discard;

    // Effective polar map convention is 0 at north and increasing anti-clockwise.
    // This basis must stay consistent with the Java-side non-ortho projection after
    // the subsequent apply_center(..., vec3(pos.x, -pos.y, 0.), ...) step.
    float theta = -(scrpos.x * TWOPI + HALFPI);
    vec2 polarXY = vec2(cos(theta), sin(theta)) * radialCoordinate;

    if (display.cutOff.z >= 0.) {
        // Convert the polar north-up basis to the display-plane x/y basis used by cutOff.
        vec2 displayXY = polarXY.yx;
        vec2 cutOffAlt = vec2(-display.cutOff.y, display.cutOff.x);
        float geometryFlatDist = abs(dot(displayXY, display.cutOff.xy));
        float geometryFlatDistAlt = abs(dot(displayXY, cutOffAlt));
        if (geometryFlatDist > display.cutOff.z || geometryFlatDistAlt > display.cutOff.z)
            discard;
    }

    vec3 centered = apply_center(vec3(polarXY.x, -polarXY.y, 0.), crval, crota);
    vec2 texCoord = rect.zw * vec2(centered.x - rect.x, -centered.y - rect.y);
    clamp_texture(texCoord);
    return texCoord;
}

void main(void) {
    vec4 color;
    vec2 scrpos = getScrPos();
    float radialCoordinate = screen.yStart + scrpos.y * (screen.yStop - screen.yStart);
    float enhancementFactor = max(1., radialCoordinate);
    bool diffMode = display.isDiff != NODIFFERENCE;
    vec2 texCoord = samplePolarTexcoord(wcs[0].crval, wcs[0].crota, wcs[0].rect, scrpos, radialCoordinate);
    if (!diffMode) {
        color = getColor(texCoord, texCoord, enhancementFactor);
    } else {
        vec2 diffTexCoord = samplePolarTexcoord(wcs[1].crval, wcs[1].crota, wcs[1].rect, scrpos, radialCoordinate);
        color = getColor(texCoord, diffTexCoord, enhancementFactor);
    }
    outColor = color;
}
