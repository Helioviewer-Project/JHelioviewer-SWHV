package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLSLShape extends GLSLArrays {

    private int count;

    public GLSLShape() {
        super(new int[]{4, 4});
    }

    public void setData(GL2 gl, FloatBuffer position, FloatBuffer color) {
        int plen = position.limit() / attribLens[0];
        if (plen * attribLens[0] != position.limit() || plen != color.limit() / attribLens[1]) {
            Log.error("Something is wrong with the attributes of this GLShape");
            return;
        }
        vaos[0].setData4(gl, position);
        vaos[1].setData4(gl, color);
        count = plen;
    }

    public void renderPoints(GL2 gl, double factor) {
        if (count == 0)
            return;

        GLSLShapeShader.point.bind(gl);
        GLSLShapeShader.point.setFactor(factor);
        GLSLShapeShader.point.bindParams(gl);

        bindVAOs(gl);
        gl.glDrawArrays(GL2.GL_POINTS, 0, count);

        GLSLShader.unbind(gl);
    }

    public void renderShape(GL2 gl, int mode) {
        if (count == 0)
            return;

        GLSLShapeShader.shape.bind(gl);
        GLSLShapeShader.shape.bindParams(gl);

        bindVAOs(gl);
        gl.glDrawArrays(mode, 0, count);

        GLSLShader.unbind(gl);
    }

}
