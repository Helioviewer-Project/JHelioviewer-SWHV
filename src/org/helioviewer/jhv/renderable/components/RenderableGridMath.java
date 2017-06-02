package org.helioviewer.jhv.renderable.components;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.opengl.GLLine;

import com.jogamp.opengl.GL2;

class RenderableGridMath {

    private static final float[] axisNorthColor = BufferUtils.colorRed;
    private static final float[] axisSouthColor = BufferUtils.colorBlue;

    private static final float AXIS_START = (float) (1. * Sun.Radius);
    private static final float AXIS_STOP = (float) (1.2 * Sun.Radius);

    static void initAxes(GL2 gl, GLLine axesLine) {
        int plen = 6;
        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(plen * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(plen * 4);

        BufferUtils.put3f(positionBuffer, 0, -AXIS_STOP, 0);
        colorBuffer.put(axisSouthColor);
        BufferUtils.put3f(positionBuffer, 0, -AXIS_START, 0);
        colorBuffer.put(axisSouthColor);

        BufferUtils.put3f(positionBuffer, 0, -AXIS_START, 0);
        colorBuffer.put(BufferUtils.colorNull);
        BufferUtils.put3f(positionBuffer, 0, AXIS_START, 0);
        colorBuffer.put(BufferUtils.colorNull);

        BufferUtils.put3f(positionBuffer, 0, AXIS_START, 0);
        colorBuffer.put(axisNorthColor);
        BufferUtils.put3f(positionBuffer, 0, AXIS_STOP, 0);
        colorBuffer.put(axisNorthColor);

        positionBuffer.flip();
        colorBuffer.flip();
        axesLine.setData(gl, positionBuffer, colorBuffer);
    }

}
