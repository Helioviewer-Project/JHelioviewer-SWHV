package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLLine extends VAO {

    public static final double LINEWIDTH_BASIC = 0.002;

    private static final int size0 = 4;
    private static final int size1 = 4;
    public static final int stride = 4 * size0 + size1;
    private int count;

    public GLSLLine(boolean _dynamic) {
        super(_dynamic, new VAA[]{
                new VAA(0, size0, false, stride, 0, 1), new VAA(1, size1, true, stride, 4 * size0, 1),
                new VAA(2, size0, false, stride, stride, 1), new VAA(3, size1, true, stride, stride + 4 * size0, 1)});
    }

    public void setData(GL2 gl, Buf buf) {
        if ((count = buf.getFloats() / size0) == 0)
            return;
        if (count * size0 != buf.getFloats() || count != buf.getBytes4()) {
            Log.error("Something is wrong with the attributes of this GLSLLine");
            return;
        }
        vbo.setData(gl, buf);
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
