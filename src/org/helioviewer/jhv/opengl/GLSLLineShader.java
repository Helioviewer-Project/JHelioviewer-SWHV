package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

public class GLSLLineShader extends GLSLShader {

    public static final GLSLLineShader line = new GLSLLineShader("/data/vertexline.glsl", "/data/fragmentline.glsl");
    public static int previousLineRef = 0;
    public static int lineRef = 1;
    public static int nextLineRef = 2;
    public static int directionRef = 3;
    public static int linecolorRef = 4;

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
        previousLineRef = gl.glGetAttribLocation(progID, "previousLine");
        lineRef = gl.glGetAttribLocation(progID, "line");
        nextLineRef = gl.glGetAttribLocation(progID, "nextLine");
        directionRef = gl.glGetAttribLocation(progID, "direction");
        linecolorRef = gl.glGetAttribLocation(progID, "linecolor");

        aspectRef = gl.glGetUniformLocation(progID, "aspect");
        // miterRef = gl.glGetUniformLocation(progID, "miter");
        thicknessRef = gl.glGetUniformLocation(progID, "thickness");
    }

    public void bindParams(GL2 gl) {
        gl.glUniform1fv(thicknessRef, 1, thickness, 0);
        gl.glUniform1fv(aspectRef, 1, aspect, 0);
        // gl.glUniform1iv(miterRef, 1, miter, 0);
    }

    public void setAspect(double _aspect) {
        aspect[0] = (float) _aspect;
    }

    public void setThickness(double _thickness) {
        thickness[0] = (float) _thickness;
    }

    @Override
    protected void bindAttribs(GL2 gl) {
    }

}
