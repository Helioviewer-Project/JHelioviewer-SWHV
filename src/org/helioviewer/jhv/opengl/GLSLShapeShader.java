package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL3;
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

    public static void init(GL3 gl) {
        point._init(gl, false);
        shape._init(gl, false);
    }

    public static void dispose(GL3 gl) {
        point._dispose(gl);
        shape._dispose(gl);
    }

    @Override
    protected void initUniforms(GL3 gl, int id) {
        refModelViewProjectionMatrix = GL33.glGetUniformLocation(id, "ModelViewProjectionMatrix");
        factorRef = GL33.glGetUniformLocation(id, "factor");
    }

    void bindParams(GL3 gl, double _factor) {
        factor[0] = (float) _factor;
        GL33.glUniform1fv(factorRef, factor);
    }

    void bindMVP(GL3 gl) {
        GL33.glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
