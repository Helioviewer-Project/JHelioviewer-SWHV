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
        super(new int[]{4});
    }

    public void render(GL2 gl) {
        bindVAO(gl);
        gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void init(GL2 gl) {
        super.init(gl);
        vbos[0].setData4(gl, vertexBuffer);
    }

}
