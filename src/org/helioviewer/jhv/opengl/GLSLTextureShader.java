package org.helioviewer.jhv.opengl;

import org.lwjgl.opengl.GL33;

import org.helioviewer.jhv.camera.Transform;

class GLSLTextureShader extends GLSLShader {

    static final GLSLTextureShader texture = new GLSLTextureShader("/glsl/texture.vert", "/glsl/texture.frag");

    private int refModelViewProjectionMatrix;
    private int colorRef;

    private GLSLTextureShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init() {
        texture._init(false);
    }

    public static void dispose() {
        texture._dispose();
    }

    @Override
    protected void initUniforms(int id) {
        refModelViewProjectionMatrix = GL33.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        colorRef = GL33.glGetUniformLocation(id, "color");
        setTextureUnit(id, "image", GLTexture.Unit.THREE);
    }

    void bindParams(float[] color) {
        GL33.glUniform4fv(colorRef, color);
    }

    void bindMVP() {
        GL33.glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
