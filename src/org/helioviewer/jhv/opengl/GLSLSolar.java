package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL2;

public class GLSLSolar extends VAO {

    private static final float[] vertices = {-1, -1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, 1, 1, 0, 1};

    GLSLSolar() {
        super(1, false, new VAA[]{new VAA(0, 4, false, 0, 0, 0)});
    }

    public void render(GL2 gl) {
        bind(gl);
        gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void init(GL2 gl) {
        super.init(gl);
        FloatBuffer buffer = FloatBuffer.wrap(vertices);
        vbo[0].setBufferData(gl, 4 * buffer.limit(), 4 * buffer.capacity(), buffer);
    }

}
