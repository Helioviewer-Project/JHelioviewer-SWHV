
// "Flat in disk": a layer painted with its native image-plane coordinates
// inside the disk view's world space, instead of getting the radial warp.
// Used for the solar disk, which is a sphere rather than annular geometry.

vec2 sampleFlatTexcoord(const vec2 crval, const vec4 crota, const vec4 rect, const vec2 w) {
    // Sky-plane scaling pegged to the solar limb: the rim corresponds to
    // screen.diskFlatRadius R_sun, computed so that r = 1 lands at the same
    // screen radius as the radial warp's r = 1 (see bindScreen)
    vec2 polarXY = 2. * screen.diskFlatRadius * vec2(w.x, -w.y);
    float radialCoordinate = length(polarXY);
    if (radialCoordinate > display.radii.y || radialCoordinate < display.radii.x)
        discard;

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
    if (screen.diskFlatRadius <= 0.)
        discard;
    vec4 color;
    vec2 w = getDiskPos();
    float t = 2. * length(w);
    if (t > 1.)
        discard;
    float angle = atan(-w.x, w.y);
    if (angle < 0.)
        angle += TWOPI;
    clamp_coord(vec2(angle / TWOPI, t));

    float enhancementFactor = 1.;
    bool diffMode = display.isDiff != NODIFFERENCE;
    vec2 texCoord = sampleFlatTexcoord(wcs[0].crval, wcs[0].crota, wcs[0].rect, w);
    if (!diffMode) {
        color = getColor(texCoord, texCoord, enhancementFactor);
    } else {
        vec2 diffTexCoord = sampleFlatTexcoord(wcs[1].crval, wcs[1].crota, wcs[1].rect, w);
        color = getColor(texCoord, diffTexCoord, enhancementFactor);
    }
    outColor = color;
}
