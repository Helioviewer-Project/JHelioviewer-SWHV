package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.camera.Transform;

import com.jogamp.opengl.GL3;

class GLSLShapeShader extends GLSLShader {

    static final GLSLShapeShader point = new GLSLShapeShader("/glsl/point.vert", "/glsl/point.frag");
    static final GLSLShapeShader shape = new GLSLShapeShader("/glsl/shape.vert", "/glsl/shape.frag");

    private int refModelViewProjectionMatrix;
    private int factorRef;

    private final float[] factor = {1};

    private GLSLShapeShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL3 gl) {
        point._init(gl, false);
        shape._init(gl, false);
    }

    public static void dispose(GL3 gl) {
        point._dispose(gl);
        shape._dispose(gl);
    }

    @Override
    protected void initUniforms(GL3 gl, int id) {
        refModelViewProjectionMatrix = gl.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        factorRef = gl.glGetUniformLocation(id, "factor");
    }

    void bindParams(GL3 gl, double _factor) {
        factor[0] = (float) _factor;
        gl.glUniform1fv(factorRef, 1, factor, 0);
    }

    void bindMVP(GL3 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
    }

}
