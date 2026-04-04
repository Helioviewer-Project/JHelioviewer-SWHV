package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;
import org.lwjgl.opengl.GL33;

import org.helioviewer.jhv.camera.Transform;

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

    public static void init(GL3 gl) {
        line._init(gl, false);
    }

    public static void dispose(GL3 gl) {
        line._dispose(gl);
    }

    @Override
    protected void initUniforms(GL3 gl, int id) {
        refModelViewProjectionMatrix = GL33.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        iaspectRef = GL33.glGetUniformLocation(id, "iaspect");
        thicknessRef = GL33.glGetUniformLocation(id, "thickness");
    }

    void bindParams(GL3 gl, double aspect, double _thickness) {
        iaspect[0] = (float) (1 / aspect);
        GL33.glUniform1fv(iaspectRef, iaspect);
        thickness[0] = (float) (0.5 * _thickness);
        GL33.glUniform1fv(thicknessRef, thickness);
    }

    void bindMVP(GL3 gl) {
        GL33.glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
