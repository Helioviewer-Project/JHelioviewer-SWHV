#version 120
varying vec4 frag_color;

//uniform vec3 viewport;
//uniform vec2 viewportOffset;

void main(void) {
    if (frag_color.a == 0.)
        discard;

    // vec2 ndcPos = 2. * (gl_FragCoord.xy - viewportOffset) / viewport.xy - 1.;

    gl_FragColor = frag_color;
}
