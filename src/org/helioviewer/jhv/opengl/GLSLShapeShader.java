package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.math.Transform;

import com.jogamp.opengl.GL2;

class GLSLShapeShader extends GLSLShader {

    static final GLSLShapeShader point = new GLSLShapeShader("/glsl/point.vert", "/glsl/point.frag");
    static final GLSLShapeShader shape = new GLSLShapeShader("/glsl/shape.vert", "/glsl/shape.frag");

    private int refModelViewProjectionMatrix;
    private int factorRef;

    private final float[] factor = {1};

    private GLSLShapeShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL2 gl) {
        point._init(gl, false);
        shape._init(gl, false);
    }

    public static void dispose(GL2 gl) {
        point._dispose(gl);
        shape._dispose(gl);
    }

    @Override
    protected void bindAttribs(GL2 gl) {
        gl.glBindAttribLocation(progID, 0, "Vertex");
        gl.glBindAttribLocation(progID, 1, "Color");
    }

    @Override
    protected void _after_init(GL2 gl) {
        bind(gl);
        refModelViewProjectionMatrix = gl.glGetUniformLocation(progID, "ModelViewProjectionMatrix");
        factorRef = gl.glGetUniformLocation(progID, "factor");
    }

    void bindParams(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
        gl.glUniform1fv(factorRef, 1, factor, 0);
    }

    void setFactor(double _factor) {
        factor[0] = (float) _factor;
    }

}
