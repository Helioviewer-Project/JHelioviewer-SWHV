void main(void)
{  
    vec2 normalizedScreenpos = (gl_FragCoord.xy-viewportOffset)/viewport-.5;
    normalizedScreenpos *= (2.*vec2(viewport.y/viewport.x, 1.));
    vec4 scrpos =  cameraTransformationInverse * vec4(normalizedScreenpos.x, normalizedScreenpos.y, -1., 1.) +0.5;
    clamp_texcoord(scrpos.xy);
    float scale = 2.*(rect.y + 1.0/rect.w);
    vec2 texcoord;
    vec3 xcart;
    vec3 xcartrot;
    float tmpConvolutionSum = 0.;
    float theta = (scrpos.y)*PI;
    float phi = PI + hgln + scrpos.x*TWOPI;
    if(cutOffRadius > 1.)
        discard;
    while(phi>TWOPI){
        phi = phi-TWOPI;
    }
    /*
    if( phi > PI/32. && phi<TWOPI - PI/32.){
        discard;
    }
    */
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
    clamp_texcoord(texcoord);
    gl_FragColor = getColor(texcoord, texcoord, 1.);
}
