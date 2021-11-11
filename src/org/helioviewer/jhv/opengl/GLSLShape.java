package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.GL2;

public class GLSLShape extends VAO implements GLSLVertexReceiver {

    private static final int size0 = 4;
    private static final int size1 = 4;
    public static final int stride = 4 * size0 + size1;

    private int count;

    public GLSLShape(boolean _dynamic) {
        super(2, _dynamic, new VAA[]{new VAA(0, size0, false, 0, 0, 0), new VAA(1, size1, true, 0, 0, 0)});
    }

    @Override
    public void setVertexRepeatable(GL2 gl, BufVertex buf) {
        count = buf.getCount();
        if (count == 0)
            return;

        Buffer buffer;
        buffer = buf.toVertexBuffer();
        vbo[0].setBufferData(gl, buffer.limit(), buffer.capacity(), buffer);
        buffer = buf.toColorBuffer();
        vbo[1].setBufferData(gl, buffer.limit(), buffer.capacity(), buffer);
    }

    public void renderPoints(GL2 gl, double factor) {
        if (count == 0)
            return;

        GLSLShapeShader.point.use(gl);
        GLSLShapeShader.point.bindParams(gl, factor);
        GLSLShapeShader.point.bindMVP(gl);

        bind(gl);
        gl.glDrawArrays(GL2.GL_POINTS, 0, count);
    }

    public void renderShape(GL2 gl, int mode) {
        if (count == 0)
            return;

        GLSLShapeShader.shape.use(gl);
        GLSLShapeShader.shape.bindMVP(gl);

        bind(gl);
        gl.glDrawArrays(mode, 0, count);
    }

}
