package org.helioviewer.jhv.renderable.components;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.opengl.GLLine;

import com.jogamp.opengl.GL2;

class RenderableGridMath {

    private static final int SUBDIVISIONS = 360;

    private static final float[] axisNorthColor = BufferUtils.colorRed;
    private static final float[] axisSouthColor = BufferUtils.colorBlue;
    private static final float[] earthLineColor = BufferUtils.colorYellow;

    private static final float AXIS_START = (float) (1. * Sun.Radius);
    private static final float AXIS_STOP = (float) (1.2 * Sun.Radius);

    private static final double EARTH_CIRCLE_RADIUS = Sun.Radius;

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

    static void initEarthCircles(GL2 gl, GLLine earthCircleLine) {
        int no_points = 2 * (SUBDIVISIONS + 3);
        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(no_points * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(no_points * 4);

        Vec3 rotv = new Vec3(), v = new Vec3();
        Quat q = Quat.createRotation(Math.PI / 2, new Vec3(1, 0, 0));
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            v.x = EARTH_CIRCLE_RADIUS * Math.cos(2 * Math.PI * i / SUBDIVISIONS);
            v.y = EARTH_CIRCLE_RADIUS * Math.sin(2 * Math.PI * i / SUBDIVISIONS);
            v.z = 0.;
            rotv = q.rotateVector(v);
            if (i == 0) {
                BufferUtils.put3f(positionBuffer, rotv);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(positionBuffer, rotv);
            colorBuffer.put(earthLineColor);
        }

        BufferUtils.put3f(positionBuffer, rotv);
        colorBuffer.put(BufferUtils.colorNull);

        v = new Vec3();
        q = Quat.createRotation(Math.PI / 2, new Vec3(0, 1, 0));
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            v.x = EARTH_CIRCLE_RADIUS * Math.cos(2 * Math.PI * i / SUBDIVISIONS);
            v.y = EARTH_CIRCLE_RADIUS * Math.sin(2 * Math.PI * i / SUBDIVISIONS);
            v.z = 0.;
            rotv = q.rotateVector(v);
            if (i == 0) {
                BufferUtils.put3f(positionBuffer, rotv);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(positionBuffer, rotv);
            colorBuffer.put(earthLineColor);
        }

        BufferUtils.put3f(positionBuffer, rotv);
        colorBuffer.put(BufferUtils.colorNull);

        positionBuffer.flip();
        colorBuffer.flip();
        earthCircleLine.setData(gl, positionBuffer, colorBuffer);
    }

}
