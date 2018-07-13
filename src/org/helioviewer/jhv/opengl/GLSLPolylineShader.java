package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.math.Transform;

import com.jogamp.opengl.GL2;

class GLSLPolylineShader extends GLSLShader {

    static final GLSLPolylineShader polyline = new GLSLPolylineShader("/glsl/polyline.vert", "/glsl/polyline.geom", "/glsl/polyline.frag");

    private int refModelViewProjectionMatrix;
    private int thicknessRef;
    private int viewportRef;

    private final float[] thickness = {5};
    private final float[] viewport = {1, 1};

    private GLSLPolylineShader(String vertex, String geometry, String fragment) {
        super(vertex, geometry, fragment);
    }

    public static void init(GL2 gl) {
        polyline._init(gl, false);
    }

    public static void dispose(GL2 gl) {
        polyline._dispose(gl);
    }

    @Override
    protected void _after_init(GL2 gl) {
        refModelViewProjectionMatrix = gl.glGetUniformLocation(progID, "ModelViewProjectionMatrix");
        thicknessRef = gl.glGetUniformLocation(progID, "Thickness");
        viewportRef = gl.glGetUniformLocation(progID, "Viewport");
    }

    void bindParams(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
        gl.glUniform2fv(viewportRef, 1, viewport, 0);
        gl.glUniform1fv(thicknessRef, 1, thickness, 0);
    }

    void setViewport(double aspect) {
        viewport[0] = (float) aspect;
    }

    void setThickness(double _thickness) {
        thickness[0] = (float) _thickness;
    }

}
