void get_lati_texcoord(const float grid[2], const float dt, const float lt, const float cr[3], const vec2 scrpos, const vec4 rect, out vec2 texcoord) {
    float phi =   grid[0] + scrpos.x * TWOPI;
    float theta = grid[1] + scrpos.y * PI;

    if (dt != 0) {
        phi += differentialRotation(dt, theta);
    }

    clamp_value(theta, 0, PI);

    vec3 xcart;
    xcart.x = sin(theta) * cos(phi);
    xcart.y = sin(theta) * sin(phi);
    xcart.z = cos(theta);
    /*
    mat3 crot = mat3(
         1.,  0.,         0.,
         0.,  cos(cr), sin(cr),
         0., -sin(cr), cos(cr)
    );
    mat3 rot = mat3(
          cos(lt), 0., sin(lt),
          0.,      1., 0.,
         -sin(lt), 0., cos(lt)
    );
    mat3 crotm = crot * rot;
    */
    float slt = sin(lt);
    float clt = cos(lt);
    float scr = cr[1];
    float ccr = cr[2];
    mat3 crotm = mat3( // should be pre-computed
              clt,   0.,       slt,
       -scr * slt,  ccr, scr * clt,
       -ccr * slt, -scr, ccr * clt
    );
    vec3 xcartrot = crotm * xcart;
    if (xcartrot.x < 0.)
        discard;

    texcoord = vec2(rect.w * (xcartrot.y - rect.x), rect.w * (xcartrot.z - rect.y));
    clamp_texture(texcoord);
}

void main(void) {
    if (radii[0] > 1.) // coronagraphs
        discard;

    vec4 color;
    vec2 texcoord;

    vec2 scrpos = getScrPos();
    get_lati_texcoord(grid, deltaT, hglt, crota, scrpos, rect, texcoord);
    if (isdifference == NODIFFERENCE) {
        color = getColor(texcoord, texcoord, 1);
    } else {
        vec2 difftexcoord;
        float difftexcoord_radius;
        get_lati_texcoord(gridDiff, deltaTDiff, hgltDiff, crotaDiff, scrpos, differencerect, difftexcoord);
        color = getColor(texcoord, difftexcoord, 1);
    }
    outColor = color;
}
