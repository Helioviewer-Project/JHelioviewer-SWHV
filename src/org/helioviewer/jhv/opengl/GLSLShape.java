package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLShape {

    private final int[] attribLens = {4, 4};
    private final VBO[] vbos = new VBO[2];

    private int count;
    private boolean inited = false;

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer color) {
        count = 0;
        int plen = position.limit() / attribLens[0];
        if (plen * attribLens[0] != position.limit() || plen != color.limit() / attribLens[1]) {
            Log.error("Something is wrong with the vertices or colors from this GLShape");
            return;
        }
        vbos[0].setData4(gl, position);
        vbos[1].setData4(gl, color);
        count = plen;
    }

    public void renderPoints(GL2 gl, double factor) {
        if (count == 0)
            return;

        GLSLShapeShader.point.bind(gl);
        GLSLShapeShader.point.setFactor(factor);
        GLSLShapeShader.point.bindParams(gl);

        bindVBOs(gl);
        gl.glDrawArrays(GL2.GL_POINTS, 0, count);

        GLSLShader.unbind(gl);
    }

    public void renderShape(GL2 gl, int mode) {
        if (count == 0)
            return;

        GLSLShapeShader.shape.bind(gl);
        GLSLShapeShader.shape.bindParams(gl);

        bindVBOs(gl);
        gl.glDrawArrays(mode, 0, count);

        GLSLShader.unbind(gl);
    }

    private void bindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.bindArray(gl);
        }
    }

    private void initVBOs(GL2 gl) {
        for (int i = 0; i < attribLens.length; i++) {
            vbos[i] = new VBO(i, attribLens[i]);
            vbos[i].init(gl);
        }
    }

    private void disposeVBOs(GL2 gl) {
        for (int i = 0; i < vbos.length; i++) {
            if (vbos[i] != null) {
                vbos[i].delete(gl);
                vbos[i] = null;
            }
        }
    }

    public void init(GL2 gl) {
        if (!inited) {
            initVBOs(gl);
            inited = true;
        }
    }

    public void dispose(GL2 gl) {
        if (inited) {
            disposeVBOs(gl);
            inited = false;
        }
    }

}
