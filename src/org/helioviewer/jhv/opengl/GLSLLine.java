package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLLine extends VAO {

    private static final int size0 = 4;
    private static final int size1 = 4;
    public static final int stride = 4 * size0 + size1;
    private int count;

    public GLSLLine() {
        super(new VAA[]{
                new VAA(0, size0, false, stride, 0, 1), new VAA(1, size1, true, stride, 4 * size0, 1),
                new VAA(2, size0, false, stride, stride, 1), new VAA(3, size1, true, stride, stride + 4 * size0, 1)});
    }

    public void setData(GL2 gl, Buf buf) {
        int plen = buf.getFloats() / size0;
        if (plen * size0 != buf.getFloats() || plen != buf.getBytes4()) {
            Log.error("Something is wrong with the attributes of this GLSLLine");
            return;
        }
        vbo.setData(gl, buf);
        count = plen - 1;
    }

    public void render(GL2 gl, Viewport vp, double thickness) {
        if (count == 0)
            return;

        GLSLLineShader.line.bind(gl);
        GLSLLineShader.line.setThickness(thickness);
        GLSLLineShader.line.setViewport(vp.aspect);
        GLSLLineShader.line.bindParams(gl);

        bind(gl);
        gl.glDrawArraysInstanced(GL2.GL_TRIANGLE_STRIP, 0, 4, count);

        GLSLShader.unbind(gl);
    }

}
