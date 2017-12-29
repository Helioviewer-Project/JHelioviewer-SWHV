package org.helioviewer.jhv.layers;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLLine;

import com.jogamp.opengl.GL2;

class GridMath {

    private static final int SUBDIVISIONS = 360;

    private static final float[] axisNorthColor = BufferUtils.colorRed;
    private static final float[] axisSouthColor = BufferUtils.colorBlue;
    private static final float[] earthLineColor = BufferUtils.colorYellow;
    private static final float[] radialLineColor = BufferUtils.colorWhite;
    private static final float[] color1 = BufferUtils.colorRed;
    private static final float[] color2 = BufferUtils.colorGreen;

    private static final float AXIS_START = (float) (1. * Sun.Radius);
    private static final float AXIS_STOP = (float) (1.2 * Sun.Radius);

    private static final double EARTH_CIRCLE_RADIUS = Sun.Radius;
    private static final double GRID_RADIUS = Sun.Radius;

    private static final int TENS_RADIUS = 3;
    private static final int END_RADIUS = TENS_RADIUS * 10;
    private static final int START_RADIUS = 2;

    static final int FLAT_STEPS_THETA = 24;
    static final int FLAT_STEPS_RADIAL = 10;

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

    static void initRadialCircles(GL2 gl, GLLine radialCircleLine, GLLine radialThickLine, double unit, double step) {
        int no_lines = (int) Math.ceil(360 / step);

        int no_points = (END_RADIUS - START_RADIUS + 1 - TENS_RADIUS) * (SUBDIVISIONS + 3) + 4 * no_lines;
        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(no_points * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(no_points * 4);
        FloatBuffer positionThick = BufferUtils.newFloatBuffer(TENS_RADIUS * (SUBDIVISIONS + 3) * 3);
        FloatBuffer colorThick = BufferUtils.newFloatBuffer(TENS_RADIUS * (SUBDIVISIONS + 3) * 4);

        Vec3 v = new Vec3();
        for (int i = START_RADIUS; i <= END_RADIUS; i++) {
            for (int j = 0; j <= SUBDIVISIONS; j++) {
                v.x = i * unit * Math.cos(2 * Math.PI * j / SUBDIVISIONS);
                v.y = i * unit * Math.sin(2 * Math.PI * j / SUBDIVISIONS);
                v.z = 0.;
                if (i % 10 == 0) {
                    if (j == 0) {
                        BufferUtils.put3f(positionThick, v);
                        colorThick.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(positionThick, v);
                    colorThick.put(radialLineColor);
                    if (j == SUBDIVISIONS) {
                        BufferUtils.put3f(positionThick, v);
                        colorThick.put(BufferUtils.colorNull);
                    }
                } else {
                    if (j == 0) {
                        BufferUtils.put3f(positionBuffer, v);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(positionBuffer, v);
                    colorBuffer.put(radialLineColor);
                    if (j == SUBDIVISIONS) {
                        BufferUtils.put3f(positionBuffer, v);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                }
            }
        }

        double i = 0;
        for (int j = 0; j < no_lines; j++) {
            i += step;
            Quat q = Quat.createRotation((Math.PI / 180) * i, new Vec3(0, 0, 1));

            v.set(START_RADIUS * unit, 0, 0);
            Vec3 rotv1 = q.rotateVector(v);
            BufferUtils.put3f(positionBuffer, rotv1);
            colorBuffer.put(BufferUtils.colorNull);
            BufferUtils.put3f(positionBuffer, rotv1);
            colorBuffer.put(radialLineColor);

            v.set(END_RADIUS * unit, 0, 0);
            Vec3 rotv2 = q.rotateVector(v);
            BufferUtils.put3f(positionBuffer, rotv2);
            colorBuffer.put(radialLineColor);
            BufferUtils.put3f(positionBuffer, rotv2);
            colorBuffer.put(BufferUtils.colorNull);
        }
        positionBuffer.flip();
        colorBuffer.flip();
        positionThick.flip();
        colorThick.flip();

        radialCircleLine.setData(gl, positionBuffer, colorBuffer);
        radialThickLine.setData(gl, positionThick, colorThick);
    }

    static void initFlatGrid(GL2 gl, GLLine flatLine, double aspect) {
        float w = (float) aspect;
        float h = 1;

        int plen = 4 * ((FLAT_STEPS_THETA + 1) + (FLAT_STEPS_RADIAL + 1));
        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(plen * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(plen * 4);

        for (int i = 0; i <= FLAT_STEPS_THETA; i++) {
            float start = -w / 2 + i * w / FLAT_STEPS_THETA;
            BufferUtils.put3f(positionBuffer, start, -h / 2, 0);
            colorBuffer.put(BufferUtils.colorNull);

            BufferUtils.put3f(positionBuffer, start, -h / 2, 0);
            if (i == FLAT_STEPS_THETA / 2) {
                colorBuffer.put(color2);
                colorBuffer.put(color2);
            } else {
                colorBuffer.put(color1);
                colorBuffer.put(color1);
            }
            BufferUtils.put3f(positionBuffer, start, h / 2, 0);

            BufferUtils.put3f(positionBuffer, start, h / 2, 0);
            colorBuffer.put(BufferUtils.colorNull);
        }
        for (int i = 0; i <= FLAT_STEPS_RADIAL; i++) {
            float start = -h / 2 + i * h / FLAT_STEPS_RADIAL;
            BufferUtils.put3f(positionBuffer, -w / 2, start, 0);
            colorBuffer.put(BufferUtils.colorNull);

            BufferUtils.put3f(positionBuffer, -w / 2, start, 0);
            if (i == FLAT_STEPS_RADIAL / 2) {
                colorBuffer.put(color2);
                colorBuffer.put(color2);
            } else {
                colorBuffer.put(color1);
                colorBuffer.put(color1);
            }
            BufferUtils.put3f(positionBuffer, w / 2, start, 0);

            BufferUtils.put3f(positionBuffer, w / 2, start, 0);
            colorBuffer.put(BufferUtils.colorNull);
        }
        positionBuffer.flip();
        colorBuffer.flip();
        flatLine.setData(gl, positionBuffer, colorBuffer);
    }

    static void initGrid(GL2 gl, GLLine gridLine, double lonstepDegrees, double latstepDegrees) {
        int no_lon_steps = ((int) Math.ceil(360 / lonstepDegrees)) / 2 + 1;
        int no_lat_steps = ((int) Math.ceil(180 / latstepDegrees)) / 2;
        int HALFDIVISIONS = SUBDIVISIONS / 2;

        int no_points = 2 * (no_lat_steps + no_lon_steps) * (HALFDIVISIONS + 3);
        FloatBuffer positionBuffer = BufferUtils.newFloatBuffer(no_points * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(no_points * 4);

        Vec3 v = new Vec3();
        double rotation;
        for (int j = 0; j < no_lon_steps; j++) {
            for (int k = -1; k <= 1; k += 2) {
                rotation = lonstepDegrees * j * k;
                Quat q = Quat.createRotation(Math.PI / 2 + Math.PI + (Math.PI / 180) * rotation, new Vec3(0, 1, 0));
                for (int i = 0; i <= HALFDIVISIONS; i++) {
                    v.x = GRID_RADIUS * Math.cos(-Math.PI / 2 + Math.PI * i / HALFDIVISIONS);
                    v.y = GRID_RADIUS * Math.sin(-Math.PI / 2 + Math.PI * i / HALFDIVISIONS);
                    v.z = 0.;
                    Vec3 rotv = q.rotateVector(v);
                    if (i == 0) {
                        BufferUtils.put3f(positionBuffer, rotv);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(positionBuffer, rotv);
                    if (i % 2 == 0) {
                        colorBuffer.put(color1);
                    } else {
                        colorBuffer.put(color2);
                    }

                    if (i == HALFDIVISIONS) {
                        BufferUtils.put3f(positionBuffer, rotv);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                }
            }
        }

        for (int j = 0; j < no_lat_steps; j++) {
            for (int k = -1; k <= 1; k += 2) {
                rotation = latstepDegrees * j * k;
                for (int i = 0; i <= HALFDIVISIONS; i++) {
                    double scale = Math.cos(Math.PI / 180. * (90 - rotation));
                    v.y = GRID_RADIUS * scale;
                    v.x = GRID_RADIUS * Math.sqrt(1. - scale * scale) * Math.sin(2 * Math.PI * i / HALFDIVISIONS);
                    v.z = GRID_RADIUS * Math.sqrt(1. - scale * scale) * Math.cos(2 * Math.PI * i / HALFDIVISIONS);
                    if (i == 0) {
                        BufferUtils.put3f(positionBuffer, v);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(positionBuffer, v);
                    if (i % 2 == 0) {
                        colorBuffer.put(color1);
                    } else {
                        colorBuffer.put(color2);
                    }
                    if (i == HALFDIVISIONS) {
                        BufferUtils.put3f(positionBuffer, v);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                }
            }
        }

        positionBuffer.flip();
        colorBuffer.flip();
        gridLine.setData(gl, positionBuffer, colorBuffer);
    }

}
