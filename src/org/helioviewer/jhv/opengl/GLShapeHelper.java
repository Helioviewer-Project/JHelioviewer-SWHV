package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;

import com.jogamp.opengl.GL2;

public class GLShapeHelper {

    private final FloatBuffer positionBuffer;
    private final FloatBuffer colorBuffer;
    private final float[] color;

    public GLShapeHelper(int vertices, float[] _color) {
        positionBuffer = BufferUtils.newFloatBuffer(4 * vertices);
        colorBuffer = BufferUtils.newFloatBuffer(4 * vertices);
        color = _color;
    }

    public void addVertex(double x, double y, double z) {
        BufferUtils.put4f(positionBuffer, (float) x, (float) y, (float) z, 1);
        colorBuffer.put(color);
    }

    public GLShape getShape(GL2 gl) {
        GLShape shape = new GLShape();
        shape.init(gl);
        positionBuffer.rewind();
        colorBuffer.rewind();
        shape.setData(gl, positionBuffer, colorBuffer);
        return shape;
    }

}
