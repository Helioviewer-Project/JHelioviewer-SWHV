package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL2;

public class GLSLSolar extends VTAO {

    private static final FloatBuffer vertexBuffer = FloatBuffer.wrap(new float[]{
            -1, -1, 0, 1,
            1, -1, 0, 1,
            -1, 1, 0, 1,
            1, 1, 0, 1
    });

    GLSLSolar() {
        super(GL2.GL_TEXTURE0 + GLTexture.Unit.THREE.ordinal(), new int[]{4});
    }

    public void render(GL2 gl) {
        bind(gl);
        gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void init(GL2 gl) {
        super.init(gl);
        vtbos[0].setData4(gl, vertexBuffer);
    }

}
