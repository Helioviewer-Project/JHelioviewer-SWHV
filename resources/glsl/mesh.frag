#version 300 es

precision highp float;

in vec4 vertexColor;
in vec2 texCoord;
out vec4 outColor;

uniform vec4 baseColor;
uniform sampler2D baseColorTexture;
uniform int hasBaseColorTexture;
uniform int alphaMode;
uniform float alphaCutoff;

const int ALPHA_OPAQUE = 0;
const int ALPHA_MASK = 1;

void main(void) {
    vec4 color = baseColor * vertexColor;
    if (hasBaseColorTexture != 0)
        color *= texture(baseColorTexture, texCoord);

    if (alphaMode == ALPHA_OPAQUE) {
        color.a = 1.;
    } else if (alphaMode == ALPHA_MASK) {
        if (color.a < alphaCutoff)
            discard;
        color.a = 1.;
    } else {
        color.rgb *= color.a;
    }
    outColor = color;
}
