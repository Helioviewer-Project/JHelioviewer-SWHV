#version 120
attribute vec3 previousLine;
attribute vec3 line;
attribute vec3 nextLine;
attribute float direction;
attribute vec4 linecolor;

varying vec4 frag_linecolor;

uniform float aspect;
uniform float thickness;

uniform mat4 projection;
uniform mat4 view;

// https://mattdesl.svbtle.com/drawing-lines-is-hard
void main() {
  vec2 aspectVec = vec2(aspect, 1.);

  mat4 ModelViewProjectionMatrix = projection * view;
  vec4 previousProjected = ModelViewProjectionMatrix * vec4(previousLine, 1.0);
  vec4 currentProjected = ModelViewProjectionMatrix * vec4(line, 1.0);
  vec4 nextProjected = ModelViewProjectionMatrix * vec4(nextLine, 1.0);

  vec2 currentScreen = currentProjected.xy / currentProjected.w * aspectVec;
  vec2 previousScreen = previousProjected.xy / previousProjected.w * aspectVec;
  vec2 nextScreen = nextProjected.xy / nextProjected.w * aspectVec;

  float len = thickness;

  vec2 dir = vec2(0.0);
  if (currentScreen == previousScreen) {
    dir = normalize(nextScreen - currentScreen);
  } else {
    dir = normalize(currentScreen - previousScreen);
  }

  vec2 normal = vec2(-dir.y, dir.x);
  normal *= len / 2.;
  normal.x /= aspect;

  vec4 offset = vec4(-normal * direction, 0.0, 0.0);
  gl_Position = currentProjected + offset;
  frag_linecolor = linecolor;
}
