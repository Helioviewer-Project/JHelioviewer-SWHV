package org.helioviewer.jhv.opengl;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import org.helioviewer.jhv.camera.Transform;

import com.jogamp.opengl.GL3;

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
        refModelViewProjectionMatrix = glGetUniformLocation(id, "ModelViewProjectionMatrix");
        iaspectRef = glGetUniformLocation(id, "iaspect");
        thicknessRef = glGetUniformLocation(id, "thickness");
    }

    void bindParams(GL3 gl, double aspect, double _thickness) {
        iaspect[0] = (float) (1 / aspect);
        glUniform1fv(iaspectRef, iaspect);
        thickness[0] = (float) (0.5 * _thickness);
        glUniform1fv(thicknessRef, thickness);
    }

    void bindMVP(GL3 gl) {
        glUniformMatrix4fv(refModelViewProjectionMatrix, false, Transform.get());
    }

}
