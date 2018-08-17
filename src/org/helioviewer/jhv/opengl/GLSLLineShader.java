package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.math.Transform;

import com.jogamp.opengl.GL2;

class GLSLLineShader extends GLSLShader {

    static final GLSLLineShader line = new GLSLLineShader("/glsl/line.vert", "/glsl/line.frag");

    private int refModelViewProjectionMatrix;
    private int thicknessRef;
    private int viewportRef;

    private final float[] thickness = {5};
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
    protected void _after_init(GL2 gl) {
        refModelViewProjectionMatrix = gl.glGetUniformLocation(progID, "ModelViewProjectionMatrix");
        thicknessRef = gl.glGetUniformLocation(progID, "thickness");
        viewportRef = gl.glGetUniformLocation(progID, "viewport");

        bind(gl);
        setTextureUnit(gl, "vertexBuffer", GLTexture.Unit.THREE);
        setTextureUnit(gl, "colorBuffer", GLTexture.Unit.FOUR);
        unbind(gl);
    }

    void bindParams(GL2 gl) {
        gl.glUniformMatrix4fv(refModelViewProjectionMatrix, 1, false, Transform.get());
        gl.glUniform4fv(viewportRef, 1, viewport, 0);
        gl.glUniform1fv(thicknessRef, 1, thickness, 0);
    }

    void setViewport(double aspect) {
        viewport[0] = (float) aspect;
    }

    void setThickness(double _thickness) {
        thickness[0] = (float) _thickness;
    }

}
