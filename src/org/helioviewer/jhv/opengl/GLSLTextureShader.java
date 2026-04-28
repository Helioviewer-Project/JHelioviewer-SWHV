package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.camera.Transform;

class GLSLTextureShader extends GLSLShader {

    static final GLSLTextureShader texture = new GLSLTextureShader("/glsl/texture.vert", "/glsl/texture.frag");
    static final GLSLTextureShader text = new GLSLTextureShader("/glsl/texture.vert", "/glsl/textureText.frag");
    static final GLSLTextureShader msdf = new GLSLTextureShader("/glsl/texture.vert", "/glsl/textureMsdf.frag");

    private int refModelViewProjectionMatrix;
    private int colorRef;
    private int unitRangeRef;

    private GLSLTextureShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init() {
        texture._init(false);
        text._init(false);
        msdf._init(false);
    }

    public static void dispose() {
        texture._dispose();
        text._dispose();
        msdf._dispose();
    }

    @Override
    protected void initUniforms(int id) {
        refModelViewProjectionMatrix = GL.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        colorRef = GL.glGetUniformLocation(id, "color");
        unitRangeRef = GL.glGetUniformLocation(id, "unitRange");
        setTextureUnit(id, "image", GLTexture.Unit.THREE);
    }

    void bindParams(float[] color) {
        GL.glUniform4fv(colorRef, color);
    }

    void bindMsdfParams(float[] color, float unitRangeX, float unitRangeY) {
        bindParams(color);
        GL.glUniform2f(unitRangeRef, unitRangeX, unitRangeY);
    }

    void bindMVP() {
        GL.glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
