package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

public class GLSLShape extends VAO implements GLSLVertexReceiver {

    private static final int size0 = 4;
    private static final int size1 = 4;
    public static final int stride = 4 * size0 + size1;

    private int count;

    public GLSLShape(boolean _dynamic) {
        super(2, _dynamic, new VAA[]{new VAA(0, size0, false, 0, 0, 0), new VAA(1, size1, true, 0, 0, 0)});
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

    private void setVertexRepeatable(Buffer vertexBuffer, Buffer colorBuffer) {
        if (count == 0)
            return;
        vbo[0].setBufferData(vertexBuffer.capacity(), vertexBuffer);
        vbo[1].setBufferData(colorBuffer.capacity(), colorBuffer);
    }

    public void renderPoints(double factor) {
        if (count == 0)
            return;

        GLSLShapeShader.point.use();
        GLSLShapeShader.point.bindParams(factor);
        GLSLShapeShader.point.bindMVP();

        bind();
        GL.glDrawArrays(GL.POINTS, 0, count);
    }

    public void renderShape(int mode) {
        if (count == 0)
            return;

        GLSLShapeShader.shape.use();
        GLSLShapeShader.shape.bindMVP();

        bind();
        GL.glDrawArrays(mode, 0, count);
    }

}
