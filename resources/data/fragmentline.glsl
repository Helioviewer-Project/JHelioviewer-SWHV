#version 120
varying vec4 frag_linecolor;
varying float frag_direction;


float smoothstep(float edge0, float edge1, float x) {
  float t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
  return t * t * (3.0 - 2.0 * t);
}

float smoothpulse(float x) {
  float steppos = 0.1;
  if(x<steppos)
    return smoothstep(0, steppos, x);
  if (x>1.-steppos)
      return smoothstep(0, steppos, 1.-x);
  return 1.;
}

void main() {
  if(frag_linecolor.a == 0.)
      discard;
  
  gl_FragColor = frag_linecolor;
  gl_FragColor.a = smoothpulse((frag_direction + 1.)/2.);
}
