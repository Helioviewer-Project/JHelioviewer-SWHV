#version 110
attribute vec3 previousLine;
attribute vec3 line;
attribute vec3 nextLine;
attribute float direction; 


uniform float aspect;
uniform float thickness;
uniform int miter;

void main() {
  vec2 aspectVec = vec2(1., 1.);
  mat4 m = gl_ModelViewProjectionMatrix;
  //mat4 m = mat4(2./aspect,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1);
  vec4 previousProjected = m * vec4(previousLine, 1.0);
  vec4 currentProjected = m * vec4(line, 1.0);
  vec4 nextProjected = m * vec4(nextLine , 1.0);
  
  //get 2D screen space with W divide and aspect correction
  vec2 currentScreen = (currentProjected.xy / currentProjected.w) * aspectVec;
  vec2 previousScreen = previousProjected.xy / previousProjected.w * aspectVec;
  vec2 nextScreen = nextProjected.xy / nextProjected.w * aspectVec;

  float len = thickness;

  //starting point uses (next - current)
  vec2 dir = vec2(0.0);
  if (currentScreen == previousScreen) {
    dir = normalize(nextScreen - currentScreen);
  } 
  //ending point uses (current - previous)
  else if (currentScreen == nextScreen) {
    dir = normalize(currentScreen - previousScreen);
  }
  //somewhere in middle, needs a join
  else {
    //get directions from (C - B) and (B - A)
    vec2 dirA = normalize((currentScreen - previousScreen));
    if (miter == 1) {
      vec2 dirB = normalize((nextScreen - currentScreen));
      //now compute the miter join normal and length
      vec2 tangent = normalize(dirA + dirB);
      vec2 perp = vec2(-dirA.y, dirA.x);
      vec2 miter = vec2(-tangent.y, tangent.x);
      dir = tangent;
      len = thickness / dot(miter, perp);
    } else {
      dir = dirA;
    }
  }
  
  if(len>2.*thickness) { 
    len = thickness;
  }
  vec2 normal =  vec2(-dir.y, dir.x);
  normal *= len/2.;
  normal.y *= aspect/2.;
 
  vec4 offset = vec4(-normal*direction, 0.0, 0.0);
  gl_Position = currentProjected + offset;
}
