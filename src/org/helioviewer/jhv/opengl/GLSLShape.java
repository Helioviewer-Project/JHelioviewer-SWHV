package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLShape extends VAO {

    private static final int size0 = 4;
    private static final int size1 = 4;
    private int count;

    public GLSLShape() {
        super(2, new VAA[]{new VAA(0, size0, false, 0, 0), new VAA(1, size1, true, 0, 0)});
    }

    public void setData(GL2 gl, FloatBuffer position, ByteBuffer color) {
        int plen = position.limit() / size0;
        if (plen * size0 != position.limit() || plen != color.limit() / size1) {
            Log.error("Something is wrong with the attributes of this GLShape");
            return;
        }
        vbo[0].setData(gl, position, 4);
        vbo[1].setData(gl, color, 1);
        count = plen;
    }

    public void renderPoints(GL2 gl, double factor) {
        if (count == 0)
            return;

        GLSLShapeShader.point.bind(gl);
        GLSLShapeShader.point.setFactor(factor);
        GLSLShapeShader.point.bindParams(gl);

        bind(gl);
        gl.glDrawArrays(GL2.GL_POINTS, 0, count);

        GLSLShader.unbind(gl);
    }

    public void renderShape(GL2 gl, int mode) {
        if (count == 0)
            return;

        GLSLShapeShader.shape.bind(gl);
        GLSLShapeShader.shape.bindParams(gl);

        bind(gl);
        gl.glDrawArrays(mode, 0, count);

        GLSLShader.unbind(gl);
    }

}
