package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;

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
        refModelViewProjectionMatrix = gl.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        colorRef = gl.glGetUniformLocation(id, "color");
        setTextureUnit(gl, id, "image", GLTexture.Unit.THREE);
    }

    void bindParams(GL3 gl, float[] color) {
        gl.glUniform4fv(colorRef, 1, color, 0);
    }

    void bindMVP(GL3 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
    }

}
