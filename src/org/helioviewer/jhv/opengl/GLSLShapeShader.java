package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class GLSLShapeShader extends GLSLShader {

    static final GLSLShapeShader point = new GLSLShapeShader("/data/PointVertex.glsl", "/data/PointFrag.glsl");
    static final GLSLShapeShader shape = new GLSLShapeShader("/data/ShapeVertex.glsl", "/data/ShapeFrag.glsl");
    static int positionRef = 0;
    static int colorRef = 1;

    private int refModelViewProjectionMatrix;
    private int factorRef;

    private final float[] factor = { 1 };

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
    protected void _dispose(GL2 gl) {
        super._dispose(gl);
    }

    @Override
    protected void _init(GL2 gl, boolean f) {
        super._init(gl, f);
    }

    @Override
    protected void _after_init(GL2 gl) {
        refModelViewProjectionMatrix = gl.glGetUniformLocation(progID, "ModelViewProjectionMatrix");
        positionRef = gl.glGetAttribLocation(progID, "position");
        colorRef = gl.glGetAttribLocation(progID, "color");
        factorRef = gl.glGetUniformLocation(progID, "factor");
    }

    public void bindParams(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, GLMatrix.get());
        gl.glUniform1fv(factorRef, 1, factor, 0);
    }

    public void setFactor(double _factor) {
        factor[0] = (float) _factor;
    }

}
