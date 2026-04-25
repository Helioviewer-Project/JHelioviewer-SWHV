package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.camera.Transform;
import org.helioviewer.jhv.display.Viewport;

class GLSLLineShader extends GLSLShader {

    static final GLSLLineShader line = new GLSLLineShader("/glsl/line.vert", "/glsl/line.frag");

    private int refModelViewProjectionMatrix;
    private int viewportOriginRef;
    private int viewportSizeRef;
    private int thicknessRef;

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
        viewportOriginRef = GL.glGetUniformLocation(id, "viewportOrigin");
        viewportSizeRef = GL.glGetUniformLocation(id, "viewportSize");
        thicknessRef = GL.glGetUniformLocation(id, "thickness");
    }

    void bindParams(Viewport vp, double _thickness) {
        GL.glUniform2f(viewportOriginRef, vp.x, vp.yGL);
        GL.glUniform2f(viewportSizeRef, vp.width, vp.height);
        GL.glUniform1f(thicknessRef, (float) (0.5 * _thickness));
    }

    void bindMVP() {
        GL.glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
