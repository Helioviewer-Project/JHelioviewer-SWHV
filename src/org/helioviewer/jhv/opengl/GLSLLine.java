package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.display.Viewport;

public class GLSLLine extends VAO implements GLSLVertexReceiver {

    public static final double LINEWIDTH_BASIC = 0.002;

    private static final int size0 = 4;
    private static final int size1 = 4;
    public static final int stride = 4 * size0 + size1;

    private int count;

    public GLSLLine(boolean _dynamic) {
        super(2, _dynamic, new VAA[]{
                new VAA(0, size0, false, 0, 0, 1), new VAA(1, size1, true, 0, 0, 1),
                new VAA(2, size0, false, 0, 4 * size0, 1), new VAA(3, size1, true, 0, size1, 1),
                new VAA(4, size0, false, 0, 8 * size0, 1), new VAA(5, size1, true, 0, 2 * size1, 1),
                new VAA(6, size0, false, 0, 12 * size0, 1), new VAA(7, size1, true, 0, 3 * size1, 1)});
    }

    @Override
    public void setVertexRepeatable(BufVertex vexBuf) {
        count = vexBuf.getCount();
        setVertexRepeatable(vexBuf.toVertexBuffer(), vexBuf.toColorBuffer());
    }

    @Override
    public void setVertexRepeatable(DirectBufVertex vexBuf) {
        count = vexBuf.count();
        setVertexRepeatable(vexBuf.vertexBuffer(), vexBuf.colorBuffer());
    }

    private void setVertexRepeatable(ByteBuffer vertexBuffer, ByteBuffer colorBuffer) {
        if (count == 0)
            return;
        vbo[0].setBufferData(vertexBuffer.capacity(), vertexBuffer);
        vbo[1].setBufferData(colorBuffer.capacity(), colorBuffer);
        if (count < 4) {
            Log.warn("GLSLLine requires at least two visible vertices padded by transparent sentinels; count=" + count + ", emitter=" + getEmitter());
            count = 0;
        } else
            count -= 3;
    }

    private static String getEmitter() {
        String self = GLSLLine.class.getName();
        String receiver = GLSLVertexReceiver.class.getName();
        return StackWalker.getInstance().walk(frames -> frames
                .filter(frame -> !frame.getClassName().equals(self) && !frame.getClassName().equals(receiver))
                .findFirst()
                .map(frame -> frame.getClassName() + "." + frame.getMethodName())
                .orElse("|unknown|"));
    }

    public void renderLine(Viewport vp, double thickness) {
        if (count == 0)
            return;

        GLSLLineShader.line.use();
        GLSLLineShader.line.bindParams(vp, thickness);

        bind();
        // Keep depth testing, but do not let translucent AA fringe pixels write depth.
        GL.glDepthMask(false);
        GL.glDrawArraysInstanced(GL.TRIANGLE_STRIP, 0, 4, count);
        GL.glDepthMask(true);
    }

}
