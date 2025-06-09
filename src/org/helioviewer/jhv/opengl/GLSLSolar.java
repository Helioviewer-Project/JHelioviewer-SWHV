package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL3;

public class GLSLSolar extends VAO1 {

    private static final FloatBuffer vertx = FloatBuffer.wrap(new float[]{-1, -1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, 1, 1, 0, 1});

    GLSLSolar() {
        super(false, new VAA[]{new VAA(0, 4, false, 0, 0, 0)});
    }

    public void render(GL3 gl) {
        bind(gl);
        gl.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void init(GL3 gl) {
        super.init(gl);
        vbo.setBufferData(gl, 4 * 16, 4 * 16, vertx);
    }

}
