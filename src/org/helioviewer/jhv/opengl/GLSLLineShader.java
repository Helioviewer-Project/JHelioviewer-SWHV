package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.math.Transform;

import com.jogamp.opengl.GL2;

class GLSLLineShader extends GLSLShader {

    static final GLSLLineShader line = new GLSLLineShader("/glsl/line.vert", "/glsl/line.frag");
    static int previousRef = 0;
    static int currentRef = 1;
    static int nextRef = 2;
    static int directionRef = 3;
    static int colorRef = 4;

    private int projectionRef;
    private int viewRef;
    private int thicknessRef;

    private int viewportRef;
    private int viewportOffsetRef;

    private final float[] thickness = { 0.005f };
    private final float[] viewport = new float[3];
    private final float[] viewportOffset = new float[2];

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
        projectionRef = gl.glGetUniformLocation(progID, "projection");
        viewRef = gl.glGetUniformLocation(progID, "view");

        previousRef = gl.glGetAttribLocation(progID, "previous");
        currentRef = gl.glGetAttribLocation(progID, "current");
        nextRef = gl.glGetAttribLocation(progID, "next");
        directionRef = gl.glGetAttribLocation(progID, "direction");
        colorRef = gl.glGetAttribLocation(progID, "color");

        thicknessRef = gl.glGetUniformLocation(progID, "thickness");
        viewportRef = gl.glGetUniformLocation(progID, "viewport");
        viewportOffsetRef = gl.glGetUniformLocation(progID, "viewportOffset");
    }

    void bindParams(GL2 gl) {
        gl.glUniformMatrix4fv(projectionRef, 1, false, Transform.getProjection());
        gl.glUniformMatrix4fv(viewRef, 1, false, Transform.getView());
        gl.glUniform1fv(thicknessRef, 1, thickness, 0);
    }

    void bindViewport(GL2 gl, float offsetX, float offsetY, float width, float height) {
        viewportOffset[0] = offsetX;
        viewportOffset[1] = offsetY;
        gl.glUniform2fv(viewportOffsetRef, 1, viewportOffset, 0);
        viewport[0] = width;
        viewport[1] = height;
        viewport[2] = height / width;
        gl.glUniform3fv(viewportRef, 1, viewport, 0);
    }

    void setThickness(double _thickness) {
        thickness[0] = (float) _thickness;
    }

}
