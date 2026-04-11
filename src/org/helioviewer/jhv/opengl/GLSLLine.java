package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

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
    public void setVertexRepeatable(BufVertex vexBuf) {
        count = vexBuf.getCount();
        if (count == 0)
            return;

        Buffer buffer;
        buffer = vexBuf.toVertexBuffer();
        vbo[0].setBufferData(buffer.limit(), buffer.capacity(), buffer);
        buffer = vexBuf.toColorBuffer();
        vbo[1].setBufferData(buffer.limit(), buffer.capacity(), buffer);

        count--;
    }

    public void renderLine(double aspect, double thickness) {
        if (count == 0)
            return;

        GLSLLineShader.line.use();
        GLSLLineShader.line.bindParams(aspect, thickness);
        GLSLLineShader.line.bindMVP();

        bind();
        GL.glDrawArraysInstanced(GL.TRIANGLE_STRIP, 0, 4, count);
    }

}
