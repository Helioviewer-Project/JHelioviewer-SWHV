package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.camera.Transform;

import com.jogamp.opengl.GL2;

class GLSLLineShader extends GLSLShader {

    static final GLSLLineShader line = new GLSLLineShader("/glsl/line.vert", "/glsl/line.frag");

    private int refModelViewProjectionMatrix;
    private int iaspectRef;
    private int thicknessRef;

    private final float[] iaspect = {1};
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
    protected void initUniforms(GL2 gl, int id) {
        refModelViewProjectionMatrix = gl.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        iaspectRef = gl.glGetUniformLocation(id, "iaspect");
        thicknessRef = gl.glGetUniformLocation(id, "thickness");
    }

    void bindParams(GL2 gl, double aspect, double _thickness) {
        iaspect[0] = (float) (1 / aspect);
        gl.glUniform1fv(iaspectRef, 1, iaspect, 0);
        thickness[0] = (float) (0.5 * _thickness);
        gl.glUniform1fv(thicknessRef, 1, thickness, 0);
    }

    void bindMVP(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
    }

}
