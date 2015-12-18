package org.helioviewer.jhv.opengl;

import com.jogamp.opengl.GL2;

public class GLSLLineShader extends GLSLShader {
    public static GLSLLineShader line = new GLSLLineShader("/data/vertex.glsl", "/data/fragmentortho.glsl");

    public GLSLLineShader(String vertex, String fragment) {
        super(vertex, fragment);
    }

    public static void init(GL2 gl) {
        line._init(gl);
    }

    public static void dispose(GL2 gl) {
        line._dispose(gl);
    }

    @Override
    protected void _after_init(GL2 gl) {
    }
}
