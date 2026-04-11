package org.helioviewer.jhv.opengl;

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

    public static void init() {
        line._init(false);
    }

    public static void dispose() {
        line._dispose();
    }

    @Override
    protected void initUniforms(int id) {
        refModelViewProjectionMatrix = GL.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        iaspectRef = GL.glGetUniformLocation(id, "iaspect");
        thicknessRef = GL.glGetUniformLocation(id, "thickness");
    }

    void bindParams(double aspect, double _thickness) {
        iaspect[0] = (float) (1 / aspect);
        GL.glUniform1fv(iaspectRef, iaspect);
        thickness[0] = (float) (0.5 * _thickness);
        GL.glUniform1fv(thicknessRef, thickness);
    }

    void bindMVP() {
        GL.glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
