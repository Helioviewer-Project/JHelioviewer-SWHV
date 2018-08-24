package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.math.Transform;

import com.jogamp.opengl.GL2;

class GLSLLineShader extends GLSLShader {

    static final GLSLLineShader line = new GLSLLineShader("/glsl/line.vert", "/glsl/line.frag");

    private int refModelViewProjectionMatrix;
    private int thicknessRef;
    private int viewportRef;

    private final float[] thickness = {0.05f};
    private final float[] viewport = {1, 1, 1, 1};

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
    protected void bindAttribLocations(GL2 gl) {
        gl.glBindAttribLocation(progID, 0, "Vertex");
        gl.glBindAttribLocation(progID, 1, "Color");
        gl.glBindAttribLocation(progID, 2, "NextVertex");
        gl.glBindAttribLocation(progID, 3, "NextColor");
    }

    @Override
    protected void initUniforms(GL2 gl) {
        refModelViewProjectionMatrix = gl.glGetUniformLocation(progID, "ModelViewProjectionMatrix");
        thicknessRef = gl.glGetUniformLocation(progID, "thickness");
        viewportRef = gl.glGetUniformLocation(progID, "viewport");
    }

    void bindParams(GL2 gl, double aspect, double _thickness) {
        viewport[0] = (float) aspect;
        gl.glUniform4fv(viewportRef, 1, viewport, 0);
        thickness[0] = (float) (0.5 * _thickness);
        gl.glUniform1fv(thicknessRef, 1, thickness, 0);
    }

    void bindMVP(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
    }

}
