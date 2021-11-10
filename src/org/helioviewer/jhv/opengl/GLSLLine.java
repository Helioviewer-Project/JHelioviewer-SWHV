package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL2;

public class GLSLLine extends VAO implements GLSLVertexReceiver {

    public static final double LINEWIDTH_BASIC = 0.002;

    private static final int size0 = 4;
    private static final int size1 = 4;
    public static final int stride = 4 * size0 + size1;

    private int count;

    public GLSLLine(boolean _dynamic) {
        super(2, _dynamic, new VAA[]{
                new VAA(0, size0, false, 0, 0, 1), new VAA(1, size1, true, 0, 0, 1),
                new VAA(2, size0, false, 0, 4 * size0, 1), new VAA(3, size1, true, 0, size1, 1)});
    }

    @Override
    public void setVertex(GL2 gl, BufVertex buf) {
        count = buf.getCount();
        if (count == 0)
            return;

        Buffer buffer;
        buffer = buf.toVertexBuffer();
        vbo[0].setBufferData(gl, buffer.limit(), buffer.capacity(), buffer);
        buffer = buf.toColorBuffer();
        vbo[1].setBufferData(gl, buffer.limit(), buffer.capacity(), buffer);
        buf.clear();

        count--;
    }

    public void render(GL2 gl, double aspect, double thickness) {
        if (count == 0)
            return;

        GLSLLineShader.line.use(gl);
        GLSLLineShader.line.bindParams(gl, aspect, thickness);
        GLSLLineShader.line.bindMVP(gl);

        bind(gl);
        gl.glDrawArraysInstanced(GL2.GL_TRIANGLE_STRIP, 0, 4, count);
    }

}
