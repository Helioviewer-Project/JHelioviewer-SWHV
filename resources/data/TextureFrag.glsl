#version 120
varying vec2 texCoord;

uniform vec4 color;
uniform sampler2D image;

void main() {
    gl_FragColor = color * texture2D(image, texCoord);
}
