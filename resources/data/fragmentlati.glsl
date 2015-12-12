void main(void)
{  
    float tmpConvolutionSum = 0.;
    vec2 scrpos= (gl_FragCoord.xy)/viewport;
    float scale = 2.*(rect.y + 1.0/rect.w);
    vec2 texcoord;
    vec3 xcart;
    vec3 xcartrot;
    float theta = (1.-scrpos.y)*PI;
    float phi = PI + hgln + scrpos.x*TWOPI;
    while(phi>TWOPI){
        phi = phi-TWOPI;
    }
    if( phi > PI/32. && phi<TWOPI - PI/32.){
        discard;
    }
    xcart.x = sin(theta)*cos(phi)/scale;
    xcart.y = sin(theta)*sin(phi)/scale;
    xcart.z = cos(theta)/scale;
    float hglto = hglt;
    mat3 rot = mat3(
         cos(hglto),  0.,  sin(hglto),
         0.            ,  1.,  0.           ,
         -sin(hglto),  0.,  cos(hglto)
    );
    xcartrot = rot * xcart; 
    if(xcartrot.x<0.0)
        discard;
    texcoord.x = xcartrot.y + 0.5;
    texcoord.y = xcartrot.z + 0.5;

    gl_FragColor = getColor(texcoord, texcoord);
}
