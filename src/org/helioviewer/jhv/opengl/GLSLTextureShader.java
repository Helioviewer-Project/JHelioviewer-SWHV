package org.helioviewer.jhv.opengl;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform4fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

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
        refModelViewProjectionMatrix = glGetUniformLocation(id, "ModelViewProjectionMatrix");
        colorRef = glGetUniformLocation(id, "color");
        setTextureUnit(gl, id, "image", GLTexture.Unit.THREE);
    }

    void bindParams(GL3 gl, float[] color) {
        glUniform4fv(colorRef, color);
    }

    void bindMVP(GL3 gl) {
        glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
