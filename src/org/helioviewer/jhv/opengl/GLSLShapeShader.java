package org.helioviewer.jhv.opengl;

import org.lwjgl.opengl.GL33;

import org.helioviewer.jhv.camera.Transform;

class GLSLShapeShader extends GLSLShader {

    static final GLSLShapeShader point = new GLSLShapeShader("/glsl/point.vert", "/glsl/point.frag");
    static final GLSLShapeShader shape = new GLSLShapeShader("/glsl/shape.vert", "/glsl/shape.frag");

    private int refModelViewProjectionMatrix;
    private int factorRef;

    private final float[] factor = {1};

    private GLSLShapeShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init() {
        point._init(null, false);
        shape._init(null, false);
    }

    public static void dispose() {
        point._dispose(null);
        shape._dispose(null);
    }

    @Override
    protected void initUniforms(com.jogamp.opengl.GL3 gl, int id) {
        refModelViewProjectionMatrix = GL33.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        factorRef = GL33.glGetUniformLocation(id, "factor");
    }

    void bindParams(double _factor) {
        factor[0] = (float) _factor;
        GL33.glUniform1fv(factorRef, factor);
    }

    void bindMVP() {
        GL33.glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
