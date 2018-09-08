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
    protected void bindAttribLocations(GL2 gl, int id) {
        gl.glBindAttribLocation(id, 0, "Vertex");
        gl.glBindAttribLocation(id, 1, "Color");
    }

    @Override
    protected void initUniforms(GL2 gl, int id) {
        refModelViewProjectionMatrix = gl.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        factorRef = gl.glGetUniformLocation(id, "factor");
    }

    void bindParams(GL2 gl, double _factor) {
        factor[0] = (float) _factor;
        gl.glUniform1fv(factorRef, 1, factor, 0);
    }

    void bindMVP(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
    }

}
