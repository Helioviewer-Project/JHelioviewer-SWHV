package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

class GLSLLineShader extends GLSLShader {

    static final GLSLLineShader line = new GLSLLineShader("/data/LineVertex.glsl", "/data/LineFrag.glsl");
    static int previousLineRef = 0;
    static int lineRef = 1;
    static int nextLineRef = 2;
    static int directionRef = 3;
    static int linecolorRef = 4;

    private int refModelViewProjectionMatrix;
    // private int miterRef;
    private int thicknessRef;
    private int aspectRef;

    // private final int[] miter = { 1 };
    private final float[] thickness = { 0.005f };
    private final float[] aspect = { 1 };

    private GLSLLineShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL2 gl) {
        line._init(gl, false);
    }

    public static void dispose(GL2 gl) {
        line._dispose(gl);
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
        previousLineRef = gl.glGetAttribLocation(progID, "previousLine");
        lineRef = gl.glGetAttribLocation(progID, "line");
        nextLineRef = gl.glGetAttribLocation(progID, "nextLine");
        directionRef = gl.glGetAttribLocation(progID, "direction");
        linecolorRef = gl.glGetAttribLocation(progID, "linecolor");

        aspectRef = gl.glGetUniformLocation(progID, "aspect");
        // miterRef = gl.glGetUniformLocation(progID, "miter");
        thicknessRef = gl.glGetUniformLocation(progID, "thickness");
    }

    void bindParams(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, GLMatrix.get());
        gl.glUniform1fv(thicknessRef, 1, thickness, 0);
        gl.glUniform1fv(aspectRef, 1, aspect, 0);
        // gl.glUniform1iv(miterRef, 1, miter, 0);
    }

    void setAspect(double _aspect) {
        aspect[0] = (float) _aspect;
    }

    void setThickness(double _thickness) {
        thickness[0] = (float) _thickness;
    }

}
