vec2 get_polar_texcoord(vec4 rect){
    vec2 normalizedScreenpos = 2.*((((gl_FragCoord.xy-viewportOffset)/viewport)-.5)*vec2(viewport.y/viewport.x, 1.));
    vec4 scrpos =  cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.) +0.5;
    clamp_texcoord(scrpos.xy);
    float theta = -(scrpos.x*TWOPI + PI/2.);
    float start = polarRadii.x;
    float end = polarRadii.y;
    float interpolated = exp(start + scrpos.y * (end-start));
    if(interpolated > outerCutOffRadius){
        discard;
    }
    if( interpolated < cutOffRadius){
        discard;
    }
    vec2 texcoord = rect.zw * (-rect.xy + vec2(cos(theta), sin(theta)) * interpolated);
    clamp_texcoord(texcoord);
    return texcoord;
}

void main(void)
{  
    vec2 texcoord = get_polar_texcoord(rect);
    vec2 difftexcoord = get_polar_texcoord(differencerect); 
    gl_FragColor = getColor(texcoord, difftexcoord);
}
