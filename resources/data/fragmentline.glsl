#version 120
varying vec4 frag_linecolor;
varying float frag_direction;


float smoothstep(float edge0, float edge1, float x) {
  float t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
  return t * t * (3.0 - 2.0 * t);
}

float smoothdoublestep(float edge0, float edge1, float x) {
  float halfway = edge0 + (edge1-edge0)/2.;
  if ( x < halfway) {
     return smoothstep(edge0, halfway, 2 * (x-edge0));
  }
  return smoothstep(halfway, edge1, 2 * (edge1 - x) );
}

void main() {
  if(frag_linecolor.a == 0.)
      discard;
      
  
  gl_FragColor = frag_linecolor;
  gl_FragColor.w = smoothdoublestep(0., 1., (frag_direction + 1.)/2.);
}
