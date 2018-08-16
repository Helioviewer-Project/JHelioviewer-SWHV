package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

import org.helioviewer.jhv.math.Transform;

class GLSLTextureShader extends GLSLShader {

    static final GLSLTextureShader texture = new GLSLTextureShader("/glsl/texture.vert", null, "/glsl/texture.frag");

    private int refModelViewProjectionMatrix;
    private int colorRef;

    private float[] color = {1, 1, 1, 1};

    private GLSLTextureShader(String vertex, String geometry, String fragment) {
        super(vertex, geometry, fragment);
    }

    public static void init(GL2 gl) {
        texture._init(gl, false);
    }

    public static void dispose(GL2 gl) {
        texture._dispose(gl);
    }

    @Override
    protected void _after_init(GL2 gl) {
        refModelViewProjectionMatrix = gl.glGetUniformLocation(progID, "ModelViewProjectionMatrix");
        colorRef = gl.glGetUniformLocation(progID, "color");
        bind(gl);
        setTextureUnit(gl, "image", GLTexture.Unit.ZERO);
        setTextureUnit(gl, "vertexBuffer", GLTexture.Unit.FOUR);
        setTextureUnit(gl, "coordBuffer", GLTexture.Unit.FIVE);
        unbind(gl);
    }

    void bindParams(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
        gl.glUniform4fv(colorRef, 1, color, 0);
    }

    void setColor(float[] _color) {
        color = _color;
    }

}
