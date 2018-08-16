package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;

public class GLSLPolyline extends VAO {

    private int count;

    public GLSLPolyline() {
        super(new int[]{4, 4});
    }

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer color) {
        int plen = position.limit() / attribLens[0];
        if (plen * attribLens[0] != position.limit() || plen != color.limit() / attribLens[1]) {
            Log.error("Something is wrong with the attributes of this GLSLPolyline");
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

        bindVAO(gl);
        gl.glDrawArrays(GL3.GL_LINE_STRIP_ADJACENCY, 0, count);

        GLSLShader.unbind(gl);
    }

}
