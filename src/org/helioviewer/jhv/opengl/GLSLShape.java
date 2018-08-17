package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLShape extends VAO {

    private final int attribLens0 = 4;
    private final int attribLens1 = 4;
    private int count;

    public GLSLShape() {
        super(2, new VAA[]{new VAA(0, 4, 0, 0), new VAA(1, 4, 0, 0)});
    }

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer color) {
        int plen = position.limit() / attribLens0;
        if (plen * attribLens0 != position.limit() || plen != color.limit() / attribLens1) {
            Log.error("Something is wrong with the attributes of this GLShape");
            return;
        }
        vbo[0].setData4(gl, position);
        vbo[1].setData4(gl, color);
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
