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
    private static final VAO vao = new VAO(0, 4);

    public static void render(GL2 gl) {
        vao.bind(gl);
        gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, 4);
        vao.unbind(gl);
    }

    static void init(GL2 gl) {
        vao.generate(gl);
        vao.setData4(gl, vertexBuffer);
    }

    static void dispose(GL2 gl) {
        vao.delete(gl);
    }

}
