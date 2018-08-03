package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;

public class GLSLPolyline {

    private final int[] attribLens = {4, 4};
    private final VBO[] vbos = new VBO[2];

    private int count;
    private boolean inited = false;

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer color) {
        count = 0;
        int plen = position.limit() / attribLens[0];
        if (plen * attribLens[0] != position.limit() || plen != color.limit() / attribLens[1]) {
            Log.error("Something is wrong with the vertices or colors from this GLSLPolyline");
            return;
        }
        vbos[0].setData4(gl, position);
        vbos[1].setData4(gl, color);
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

        GLSLShader.unbind(gl);
    }

    private void bindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.bind(gl);
        }
    }

    private void initVBOs(GL2 gl) {
        for (int i = 0; i < attribLens.length; i++) {
            vbos[i] = new VBO(i, attribLens[i]);
            vbos[i].generate(gl);
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
