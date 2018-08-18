package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL2;

public class GLSLSolar extends VAO {

    private static final FloatBuffer vertexBuffer = FloatBuffer.wrap(new float[]{
            -1, -1, 0, 1,
            1, -1, 0, 1,
            -1, 1, 0, 1,
            1, 1, 0, 1
    });

    GLSLSolar() {
        super(1, new VAA[]{new VAA(0, 4, false, 0, 0)});
    }

    public void render(GL2 gl) {
        bind(gl);
        gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void init(GL2 gl) {
        super.init(gl);
        vbo[0].setData(gl, vertexBuffer, 4);
    }

}
