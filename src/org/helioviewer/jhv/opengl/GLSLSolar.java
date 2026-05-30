package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;

public class GLSLSolar extends VAO1 {

    public static final GLSLSolar quad = new GLSLSolar();

    private static final FloatBuffer vertx = BufferUtils.newFloatBuffer(16).put(new float[]{-1, -1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, 1, 1, 0, 1}).flip();

    GLSLSolar() {
        super(false, new VAA[]{new VAA(0, 4, false, 0, 0, 0)});
    }

    public void render() {
        bind();
        GL.glDrawArrays(GL.TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void init() {
        super.init();
        vbo.setBufferData(4 * 16, vertx);
    }

}
