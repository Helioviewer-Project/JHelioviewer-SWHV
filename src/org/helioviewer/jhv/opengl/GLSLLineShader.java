package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.math.Transform;

import com.jogamp.opengl.GL2;

class GLSLLineShader extends GLSLShader {

    static final GLSLLineShader line = new GLSLLineShader("/glsl/line.vert", "/glsl/line.frag");

    private int refModelViewProjectionMatrix;
    private int aspectRef;
    private int thicknessRef;

    private final float[] aspect = {1};
    private final float[] thickness = {0.05f};

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
    protected void bindAttribLocations(GL2 gl, int id) {
        gl.glBindAttribLocation(id, 0, "Vertex");
        gl.glBindAttribLocation(id, 1, "Color");
        gl.glBindAttribLocation(id, 2, "NextVertex");
        gl.glBindAttribLocation(id, 3, "NextColor");
    }

    @Override
    protected void initUniforms(GL2 gl, int id) {
        refModelViewProjectionMatrix = gl.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        aspectRef = gl.glGetUniformLocation(id, "aspect");
        thicknessRef = gl.glGetUniformLocation(id, "thickness");
    }

    void bindParams(GL2 gl, double _aspect, double _thickness) {
        aspect[0] = (float) _aspect;
        gl.glUniform1fv(aspectRef, 1, aspect, 0);
        thickness[0] = (float) (0.5 * _thickness);
        gl.glUniform1fv(thicknessRef, 1, thickness, 0);
    }

    void bindMVP(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
    }

}
