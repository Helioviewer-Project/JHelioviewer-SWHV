#version 150 core

in vec3 previous;
in vec3 current;
in vec3 next;
in float direction;
in vec4 color;

out vec4 frag_color;

uniform float thickness;
uniform vec3 viewport;

uniform mat4 projection;
uniform mat4 view;

// https://mattdesl.svbtle.com/drawing-lines-is-hard
void main(void) {
    mat4 ModelViewProjectionMatrix = projection * view;
    vec4 previousProjected = ModelViewProjectionMatrix * vec4(previous, 1.);
    vec4 currentProjected = ModelViewProjectionMatrix * vec4(current, 1.);

    vec2 dir;
    if (currentProjected == previousProjected) {
        vec4 nextProjected = ModelViewProjectionMatrix * vec4(next, 1.);
        dir = normalize(nextProjected.xy - currentProjected.xy);
    } else {
        dir = normalize(currentProjected.xy - previousProjected.xy);
    }

    vec2 normal = vec2(dir.y * viewport.z, -dir.x);
    normal *= currentProjected.w * thickness / 2.;

    vec4 offset = vec4(normal * direction, 0., 0.);
    gl_Position = currentProjected + offset;
    frag_color = color;
}
