package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLLine extends VAO {

    private static final int elems0 = 4;
    private static final int elems1 = 4;
    private int count;

    public GLSLLine() {
        super(2, new VAA[]{new VAA(0, elems0, 0, 1), new VAA(1, elems1, 0, 1), new VAA(2, elems0, 16, 1), new VAA(3, elems1, 16, 1)});
    }

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer color) {
        int plen = position.limit() / elems0;
        if (plen * elems0 != position.limit() || plen != color.limit() / elems1) {
            Log.error("Something is wrong with the attributes of this GLSLPolyline");
            return;
        }
        vbo[0].setData4(gl, position);
        vbo[1].setData4(gl, color);
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
