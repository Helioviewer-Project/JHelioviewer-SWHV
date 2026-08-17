#version 300 es

precision highp float;
precision highp sampler3D;

in vec3 texturePosition;
out vec4 outColor;

uniform sampler3D volume;
uniform sampler3D validityMask;
uniform sampler2D lut;
uniform vec3 corner;
uniform vec3 dimensions;
uniform vec3 axisX;
uniform vec3 axisY;
uniform vec3 axisZ;
uniform vec3 rayDirection;
uniform float opacity;
uniform vec3 cropMin;
uniform vec3 cropMax;

const int MAX_STEPS = 2048;
const float MAX_OPTICAL_DEPTH = 2.0;
const float NOISE_GRANULARITY = 1.0 / 255.0;
const vec3 nvec = vec3(12.9898, 78.233, 37.719);

float dither(vec3 coord) {
    float random = fract(sin(dot(coord, nvec)) * 43758.5453);
    return mix(-NOISE_GRANULARITY, NOISE_GRANULARITY, random);
}

bool outside(vec3 p) {
    return any(lessThan(p, vec3(0.0))) || any(greaterThan(p, vec3(1.0)));
}

bool intersectAxis(float position, float direction, float minimum, float maximum,
                   inout float entry, inout float exit) {
    if (direction == 0.0)
        return position >= minimum && position <= maximum;
    float first = (minimum - position) / direction;
    float second = (maximum - position) / direction;
    entry = max(entry, min(first, second));
    exit = min(exit, max(first, second));
    return entry < exit;
}

void main(void) {
    float idealStepDistance = 0.5 / max(max(abs(rayDirection.x) * dimensions.x,
                                        abs(rayDirection.y) * dimensions.y),
                                        abs(rayDirection.z) * dimensions.z);
    float entryDistance = 0.0;
    float exitDistance = 1e30;
    if (!intersectAxis(texturePosition.x, rayDirection.x, cropMin.x, cropMax.x, entryDistance, exitDistance) ||
            !intersectAxis(texturePosition.y, rayDirection.y, cropMin.y, cropMax.y, entryDistance, exitDistance) ||
            !intersectAxis(texturePosition.z, rayDirection.z, cropMin.z, cropMax.z, entryDistance, exitDistance))
        discard;
    float totalDistance = exitDistance - entryDistance;
    int stepCount = max(1, min(MAX_STEPS, int(ceil(totalDistance / idealStepDistance))));
    float stepDistance = totalDistance / float(stepCount);
    vec3 stepVector = rayDirection * stepDistance;
    vec3 position = texturePosition + rayDirection * entryDistance + 0.5 * stepVector;
    vec4 accumulated = vec4(0.0);

    for (int i = 0; i < MAX_STEPS; i++) {
        if (i >= stepCount)
            break;
        if (outside(position))
            break;

        vec3 worldPosition = corner + position.x * axisX + position.y * axisY + position.z * axisZ;
        if (dot(worldPosition, worldPosition) <= 1.0)
            break;

        float validity = texture(validityMask, position).r;
        // Renormalize the linearly filtered value over the defined neighboring voxels.
        if (validity <= 0.0) {
            position += stepVector;
            continue;
        }
        float filteredValue = texture(volume, position).r;
        float value = filteredValue / validity;
        vec4 sampleColor = texture(lut, vec2(value, 0.5));
        float sampleOpacity = sampleColor.a * (1.0 - exp(-MAX_OPTICAL_DEPTH * filteredValue * stepDistance));
        accumulated.rgb += (1.0 - accumulated.a) * sampleOpacity * sampleColor.rgb;
        accumulated.a += (1.0 - accumulated.a) * sampleOpacity;
        if (accumulated.a >= 0.995)
            break;

        position += stepVector;
    }

    if (accumulated.a <= 0.0)
        discard;
    accumulated.rgb = clamp(accumulated.rgb + dither(texturePosition), vec3(0.0), vec3(accumulated.a));
    outColor = opacity * accumulated;
}
