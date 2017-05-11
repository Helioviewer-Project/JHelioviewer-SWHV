package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

public class GLSLLineShader extends GLSLShader {

    private static final GLSLLineShader line = new GLSLLineShader("/data/vertexline.glsl", "/data/fragmentline.glsl");
    private static  int previousLineRef = 0;
    private static int lineRef = 1;
    private static int nextLineRef = 2;
    private static int directionRef = 3;

    private int miterRef;
    private int thicknessRef;
    private int aspectRef;

    private final int[] miter = { 1 };
    private final float[] thickness = { 0.005f };
    private final float[] aspect = { 1 };
//    private boolean inited = false;

    public GLSLLineShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL2 gl) {
        line._init(gl, false);
    }

    public static void dispose(GL2 gl) {
        line._dispose(gl);
    }

    protected  void _dispose(GL2 gl) {
        super._dispose(gl);
//        inited = false;
    }

    protected  void _init(GL2 gl, boolean f) {
        super._init(gl, f);
//        inited = true;
    }

    @Override
    protected void _after_init(GL2 gl) {
        previousLineRef = gl.glGetAttribLocation(progID, "previousLine");
        lineRef = gl.glGetAttribLocation(progID, "line");
        nextLineRef = gl.glGetAttribLocation(progID, "nextLine");
        directionRef = gl.glGetAttribLocation(progID, "direction");

        aspectRef = gl.glGetUniformLocation(progID, "aspect");
        miterRef = gl.glGetUniformLocation(progID, "miter");
        thicknessRef = gl.glGetUniformLocation(progID, "thickness"); 
    }

    public void bindParams(GL2 gl) {
        gl.glUniform1fv(thicknessRef, 1, thickness, 0);
        gl.glUniform1fv(aspectRef, 1, aspect, 0);
        gl.glUniform1iv(miterRef, 1, miter, 0);
    }

    public void setAspect(float _aspect) {
        aspect[0] = _aspect;
    }

    protected void bindAttribs(GL2 gl){
        gl.glBindAttribLocation(progID, previousLineRef, "previousLine");
        gl.glBindAttribLocation(progID, lineRef, "line");
        gl.glBindAttribLocation(progID, nextLineRef, "nextLine");
        gl.glBindAttribLocation(progID, directionRef, "direction");
    }

}
