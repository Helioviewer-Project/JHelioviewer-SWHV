package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL2;

public class GLSLSolar {

    private static final float R = 32; // > C3
    private static final FloatBuffer vertexBuffer = FloatBuffer.wrap(new float[] {
        -R, -R, 0, 1, R, R, 0, 1, -R, R, 0, 1,
        R, R, 0, 1, -R, -R, 0, 1, R, -R, 0, 1
    });
    private static final int vertexSize = vertexBuffer.limit();
    private static final VBO vertexVBO = VBO.gen_float_VBO(0, 4);

    public static void render(GL2 gl) {
        bindVBOs(gl);
        gl.glDrawArrays(GL2.GL_TRIANGLES, 0, vertexSize);
    }

    private static void bindVBOs(GL2 gl) {
        vertexVBO.bindArray(gl);
    }

    static void init(GL2 gl) {
        vertexVBO.init(gl);
        vertexVBO.setData4(gl, vertexBuffer);
    }

    static void dispose(GL2 gl) {
        vertexVBO.dispose(gl);
    }

}
