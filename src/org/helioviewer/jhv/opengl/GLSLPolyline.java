package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;

public class GLSLPolyline {

    private final int[] vboAttribLens = { 4, 4 };
    private final VBO[] vbos = new VBO[2];

    private int count;
    private boolean inited = false;

    public void setData(GL2 gl, FloatBuffer points, FloatBuffer colors) {
        count = 0;
        int plen = points.limit() / vboAttribLens[0];
        if (plen * vboAttribLens[0] != points.limit() || plen != colors.limit() / vboAttribLens[1]) {
            Log.error("Something is wrong with the vertices or colors from this GLSLPolyline");
            return;
        }
        vbos[0].setData4(gl, points);
        vbos[1].setData4(gl, colors);
        count = plen;
    }

    public void render(GL2 gl, Viewport vp, double thickness) {
        if (count == 0)
            return;

        GLSLPolylineShader.polyline.bind(gl);
        GLSLPolylineShader.polyline.setThickness(thickness);
        GLSLPolylineShader.polyline.setViewport(vp.aspect);
        GLSLPolylineShader.polyline.bindParams(gl);

        bindVBOs(gl);
        gl.glDrawArrays(GL3.GL_LINE_STRIP_ADJACENCY, 0, count);
        unbindVBOs(gl);

        GLSLShader.unbind(gl);
    }

    private void bindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.bindArray(gl);
        }
    }

    private void unbindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.unbindArray(gl);
        }
    }

    private void initVBOs(GL2 gl) {
        for (int i = 0; i < vboAttribLens.length; i++) {
            vbos[i] = VBO.gen_float_VBO(i, vboAttribLens[i]);
            vbos[i].init(gl);
        }
    }

    private void disposeVBOs(GL2 gl) {
        for (int i = 0; i < vbos.length; i++) {
            if (vbos[i] != null) {
                vbos[i].dispose(gl);
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
