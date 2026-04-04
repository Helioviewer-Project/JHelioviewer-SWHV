package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;
import org.lwjgl.opengl.GL33;

import org.helioviewer.jhv.camera.Transform;

class GLSLTextureShader extends GLSLShader {

    static final GLSLTextureShader texture = new GLSLTextureShader("/glsl/texture.vert", "/glsl/texture.frag");

    private int refModelViewProjectionMatrix;
    private int colorRef;

    private GLSLTextureShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL3 gl) {
        texture._init(gl, false);
    }

    public static void dispose(GL3 gl) {
        texture._dispose(gl);
    }

    @Override
    protected void initUniforms(GL3 gl, int id) {
        refModelViewProjectionMatrix = GL33.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        colorRef = GL33.glGetUniformLocation(id, "color");
        setTextureUnit(gl, id, "image", GLTexture.Unit.THREE);
    }

    void bindParams(GL3 gl, float[] color) {
        GL33.glUniform4fv(colorRef, color);
    }

    void bindMVP(GL3 gl) {
        GL33.glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
