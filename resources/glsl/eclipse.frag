#version 300 es

precision highp float;

in vec2 normalizedScreenpos;
out vec4 outColor;

layout(std140) uniform ScreenBlock {
    mat4 inverseMVP;
} screen;

uniform vec4 disk;
uniform float planeDistance;
uniform vec4 rotation;
uniform vec4 color;

const vec3 zAxis = vec3(0, 0, 1);

vec3 rotate_vector_inverse(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(cross(vec, quat.xyz) + quat.w * vec, quat.xyz);
}

vec3 rotate_vector(const vec4 quat, const vec3 vec) {
    return vec + 2. * cross(quat.xyz, cross(quat.xyz, vec) + quat.w * vec);
}

void main(void) {
    vec2 viewPosition = (screen.inverseMVP * vec4(normalizedScreenpos, -1., 1.)).xy;
    float radius2 = dot(viewPosition, viewPosition);
    bool onDisk = radius2 <= 1.;
    vec3 source = onDisk ? rotate_vector_inverse(rotation, vec3(viewPosition, sqrt(1. - radius2))) : vec3(0.);
    vec3 ray;
    if (onDisk && source.z >= 0.) {
        ray = vec3(source.xy, planeDistance);
    } else {
        vec3 normal = rotate_vector(rotation, zAxis);
        if (onDisk && normal.z <= 0.)
            discard;
        source = rotate_vector_inverse(rotation, vec3(viewPosition * normal.z, -dot(normal.xy, viewPosition)));
        ray = vec3(source.xy, planeDistance * normal.z) * sign(normal.z);
    }
    vec3 offset = cross(ray, disk.xyz);
    if (dot(ray, disk.xyz) <= 0. || dot(offset, offset) > disk.w * dot(ray, ray))
        discard;
    outColor = color;
}
