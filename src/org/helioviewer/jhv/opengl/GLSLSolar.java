package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL2;

public class GLSLSolar {

    private static final FloatBuffer vertexBuffer = FloatBuffer.wrap(new float[]{
            -1, -1, 0, 1,
            1, -1, 0, 1,
            -1, 1, 0, 1,
            1, 1, 0, 1
    });
    private static final VBO vertexVBO = VBO.gen_float_VBO(0, 4);

    public static void render(GL2 gl) {
        vertexVBO.bindArray(gl);
        gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, 4);
    }

    static void init(GL2 gl) {
        vertexVBO.init(gl);
        vertexVBO.setData4(gl, vertexBuffer);
    }

    static void dispose(GL2 gl) {
        vertexVBO.dispose(gl);
    }

}
