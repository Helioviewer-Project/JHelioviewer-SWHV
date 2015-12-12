void main(void)
{  
    vec2 scrpos= (gl_FragCoord.xy)/viewport;
    vec2 texcoord = rect.zw * (-rect.xy + vec2(cos(scrpos.x*TWOPI), sin(scrpos.x*TWOPI)) * scrpos.y * 2.);
    vec2 difftexcoord = differencerect.zw * (-differencerect.xy + vec2(cos(scrpos.x*TWOPI), sin(scrpos.x*TWOPI)) * scrpos.y * 2.);
    clamp_texcoord(texcoord);
    clamp_texcoord(difftexcoord);
    gl_FragColor = getColor(texcoord, difftexcoord);
}
