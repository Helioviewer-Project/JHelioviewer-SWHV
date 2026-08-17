#version 300 es

precision highp float;
precision highp sampler3D;

in vec3 texturePosition;
out vec4 outColor;

uniform sampler3D volume;
uniform sampler3D validityMask;
uniform vec3 corner;
uniform vec3 dimensions;
uniform vec3 axisX;
uniform vec3 axisY;
uniform vec3 axisZ;
uniform vec3 rayDirection;

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

float exitDistance(float position, float direction) {
    if (direction > 0.0)
        return (1.0 - position) / direction;
    if (direction < 0.0)
        return -position / direction;
    return 1e30;
}

void main(void) {
    float idealStepDistance = 0.5 / max(max(abs(rayDirection.x) * dimensions.x,
                                                abs(rayDirection.y) * dimensions.y),
                                        abs(rayDirection.z) * dimensions.z);
    float totalDistance = min(min(exitDistance(texturePosition.x, rayDirection.x),
                                  exitDistance(texturePosition.y, rayDirection.y)),
                              exitDistance(texturePosition.z, rayDirection.z));
    int stepCount = max(1, min(MAX_STEPS, int(ceil(totalDistance / idealStepDistance))));
    float stepDistance = totalDistance / float(stepCount);
    vec3 stepVector = rayDirection * stepDistance;
    vec3 position = texturePosition + 0.5 * stepVector;
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
        float opacity = 1.0 - exp(-MAX_OPTICAL_DEPTH * filteredValue * stepDistance);
        accumulated.rgb += (1.0 - accumulated.a) * opacity * vec3(value);
        accumulated.a += (1.0 - accumulated.a) * opacity;
        if (accumulated.a >= 0.995)
            break;

        position += stepVector;
    }

    if (accumulated.a <= 0.0)
        discard;
    accumulated.rgb = clamp(accumulated.rgb + dither(texturePosition), vec3(0.0), vec3(accumulated.a));
    outColor = accumulated;
}
