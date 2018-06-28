package org.helioviewer.jhv.layers;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLSLPolyline;
import org.helioviewer.jhv.opengl.GLSLShape;

import com.jogamp.opengl.GL2;

class GridMath {

    private static final int SUBDIVISIONS = 360;

    private static final float[] radialLineColor = BufferUtils.colorDarkGray;
    private static final float[] axisNorthColor = BufferUtils.colorRed;
    private static final float[] axisSouthColor = BufferUtils.colorBlue;
    private static final float[] earthLineColor = BufferUtils.colorYellow;

    private static final float[] color1 = BufferUtils.colorRed;
    private static final float[] color2 = BufferUtils.colorGreen;

    private static final float earthPointSize = 0.01f;

    private static final int LINEAR_STEPS = 1;

    private static final float AXIS_START = (float) (1. * Sun.Radius);
    private static final float AXIS_STOP = (float) (1.2 * Sun.Radius);

    private static final double EARTH_CIRCLE_RADIUS = Sun.Radius;
    private static final double GRID_RADIUS = Sun.Radius;

    private static final int TENS_RADIUS = 3;
    private static final int END_RADIUS = TENS_RADIUS * 10;
    private static final int START_RADIUS = 2;

    static final int FLAT_STEPS_THETA = 24;
    static final int FLAT_STEPS_RADIAL = 10;

    static void initAxes(GL2 gl, GLSLPolyline axesLine) {
        int plen = 8;
        FloatBuffer vertexBuffer = BufferUtils.newFloatBuffer(plen * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(plen * 4);

        BufferUtils.put3f(vertexBuffer, 0, -AXIS_STOP, 0);
        colorBuffer.put(BufferUtils.colorNull);
        BufferUtils.put3f(vertexBuffer, 0, -AXIS_STOP, 0);
        colorBuffer.put(axisSouthColor);
        BufferUtils.put3f(vertexBuffer, 0, -AXIS_START, 0);
        colorBuffer.put(axisSouthColor);

        BufferUtils.put3f(vertexBuffer, 0, -AXIS_START, 0);
        colorBuffer.put(BufferUtils.colorNull);
        BufferUtils.put3f(vertexBuffer, 0, AXIS_START, 0);
        colorBuffer.put(BufferUtils.colorNull);

        BufferUtils.put3f(vertexBuffer, 0, AXIS_START, 0);
        colorBuffer.put(axisNorthColor);
        BufferUtils.put3f(vertexBuffer, 0, AXIS_STOP, 0);
        colorBuffer.put(axisNorthColor);
        BufferUtils.put3f(vertexBuffer, 0, AXIS_STOP, 0);
        colorBuffer.put(BufferUtils.colorNull);

        vertexBuffer.rewind();
        colorBuffer.rewind();
        axesLine.setData(gl, vertexBuffer, colorBuffer);
    }

    static void initEarthPoint(GL2 gl, GLSLShape earthPoint) {
        FloatBuffer vertexBuffer = BufferUtils.newFloatBuffer(4);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(4);

        BufferUtils.put4f(vertexBuffer, 0, 0, (float) (EARTH_CIRCLE_RADIUS + 0.006), earthPointSize);
        colorBuffer.put(earthLineColor);

        vertexBuffer.rewind();
        colorBuffer.rewind();
        earthPoint.setData(gl, vertexBuffer, colorBuffer);
    }

    static void initEarthCircles(GL2 gl, GLSLPolyline earthCircleLine) {
        int no_points = 2 * (SUBDIVISIONS + 3);
        FloatBuffer vertexBuffer = BufferUtils.newFloatBuffer(no_points * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(no_points * 4);

        Vec3 rotv = new Vec3(), v = new Vec3();
        Quat q = Quat.createRotation(Math.PI / 2, Vec3.XAxis);
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            v.x = EARTH_CIRCLE_RADIUS * Math.cos(2 * Math.PI * i / SUBDIVISIONS);
            v.y = EARTH_CIRCLE_RADIUS * Math.sin(2 * Math.PI * i / SUBDIVISIONS);
            v.z = 0.;
            rotv = q.rotateVector(v);
            if (i == 0) {
                BufferUtils.put3f(vertexBuffer, rotv);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(vertexBuffer, rotv);
            colorBuffer.put(earthLineColor);
        }

        BufferUtils.put3f(vertexBuffer, rotv);
        colorBuffer.put(BufferUtils.colorNull);

        v = new Vec3();
        q = Quat.createRotation(Math.PI / 2, Vec3.YAxis);
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            v.x = EARTH_CIRCLE_RADIUS * Math.cos(2 * Math.PI * i / SUBDIVISIONS);
            v.y = EARTH_CIRCLE_RADIUS * Math.sin(2 * Math.PI * i / SUBDIVISIONS);
            v.z = 0.;
            rotv = q.rotateVector(v);
            if (i == 0) {
                BufferUtils.put3f(vertexBuffer, rotv);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(vertexBuffer, rotv);
            colorBuffer.put(earthLineColor);
        }

        BufferUtils.put3f(vertexBuffer, rotv);
        colorBuffer.put(BufferUtils.colorNull);

        vertexBuffer.rewind();
        colorBuffer.rewind();
        earthCircleLine.setData(gl, vertexBuffer, colorBuffer);
    }

    static void initRadialCircles(GL2 gl, GLSLPolyline radialCircleLine, GLSLPolyline radialThickLine, double unit, double step) {
        int no_lines = (int) Math.ceil(360 / step);
        int no_points = (END_RADIUS - START_RADIUS + 1 - TENS_RADIUS) * (SUBDIVISIONS + 3) + (LINEAR_STEPS + 3) * no_lines;
        FloatBuffer vertexBuffer = BufferUtils.newFloatBuffer(no_points * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(no_points * 4);
        FloatBuffer vertexThick = BufferUtils.newFloatBuffer(TENS_RADIUS * (SUBDIVISIONS + 3) * 3);
        FloatBuffer colorThick = BufferUtils.newFloatBuffer(TENS_RADIUS * (SUBDIVISIONS + 3) * 4);

        for (int i = START_RADIUS; i <= END_RADIUS; i++) {
            for (int j = 0; j <= SUBDIVISIONS; j++) {
                float x = (float) (i * unit * Math.cos(2 * Math.PI * j / SUBDIVISIONS));
                float y = (float) (i * unit * Math.sin(2 * Math.PI * j / SUBDIVISIONS));

                if (i % 10 == 0) {
                    if (j == 0) {
                        BufferUtils.put3f(vertexThick, x, y, 0);
                        colorThick.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(vertexThick, x, y, 0);
                    colorThick.put(radialLineColor);
                    if (j == SUBDIVISIONS) {
                        BufferUtils.put3f(vertexThick, x, y, 0);
                        colorThick.put(BufferUtils.colorNull);
                    }
                } else {
                    if (j == 0) {
                        BufferUtils.put3f(vertexBuffer, x, y, 0);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(vertexBuffer, x, y, 0);
                    colorBuffer.put(radialLineColor);
                    if (j == SUBDIVISIONS) {
                        BufferUtils.put3f(vertexBuffer, x, y, 0);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                }
            }
        }

        Vec3 rotv, v = new Vec3();
        double i = 0;
        for (int j = 0; j < no_lines; j++) {
            i += step;
            Quat q = Quat.createRotation((Math.PI / 180) * i, Vec3.ZAxis);

            for (int k = 0; k <= LINEAR_STEPS; k++) {
                v.set((START_RADIUS + k * (END_RADIUS - START_RADIUS) / (double) LINEAR_STEPS) * unit, 0, 0);
                rotv = q.rotateVector(v);

                if (k == 0) {
                    BufferUtils.put3f(vertexBuffer, rotv);
                    colorBuffer.put(BufferUtils.colorNull);
                }
                BufferUtils.put3f(vertexBuffer, rotv);
                colorBuffer.put(radialLineColor);
                if (k == LINEAR_STEPS) {
                    BufferUtils.put3f(vertexBuffer, rotv);
                    colorBuffer.put(BufferUtils.colorNull);
                }
            }
        }
        vertexBuffer.rewind();
        colorBuffer.rewind();
        vertexThick.rewind();
        colorThick.rewind();

        radialCircleLine.setData(gl, vertexBuffer, colorBuffer);
        radialThickLine.setData(gl, vertexThick, colorThick);
    }

    static void initFlatGrid(GL2 gl, GLSLPolyline flatLine, double aspect) {
        int plen = (LINEAR_STEPS + 3) * (FLAT_STEPS_THETA + 1 + FLAT_STEPS_RADIAL + 1);
        FloatBuffer vertexBuffer = BufferUtils.newFloatBuffer(plen * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(plen * 4);

        for (int i = 0; i <= FLAT_STEPS_THETA; i++) {
            float start = (float) (aspect * (-0.5 + i / (double) FLAT_STEPS_THETA));

            for (int k = 0; k <= LINEAR_STEPS; k++) {
                float v = (float) (-0.5 + k / (double) LINEAR_STEPS);

                if (k == 0) {
                    BufferUtils.put3f(vertexBuffer, start, v, 0);
                    colorBuffer.put(BufferUtils.colorNull);
                }
                BufferUtils.put3f(vertexBuffer, start, v, 0);
                colorBuffer.put(i == FLAT_STEPS_THETA / 2 ? color2 : color1);
                if (k == LINEAR_STEPS) {
                    BufferUtils.put3f(vertexBuffer, start, v, 0);
                    colorBuffer.put(BufferUtils.colorNull);
                }
            }
        }
        for (int i = 0; i <= FLAT_STEPS_RADIAL; i++) {
            float start = (float) (-0.5 + i / (double) FLAT_STEPS_RADIAL);

            for (int k = 0; k <= LINEAR_STEPS; k++) {
                float v = (float) (aspect * (-0.5 + k / (double) LINEAR_STEPS));

                if (k == 0) {
                    BufferUtils.put3f(vertexBuffer, v, start, 0);
                    colorBuffer.put(BufferUtils.colorNull);
                }
                BufferUtils.put3f(vertexBuffer, v, start, 0);
                colorBuffer.put(i == FLAT_STEPS_RADIAL / 2 ? color2 : color1);
                if (k == LINEAR_STEPS) {
                    BufferUtils.put3f(vertexBuffer, v, start, 0);
                    colorBuffer.put(BufferUtils.colorNull);
                }
            }
        }
        vertexBuffer.rewind();
        colorBuffer.rewind();
        flatLine.setData(gl, vertexBuffer, colorBuffer);
    }

    static void initGrid(GL2 gl, GLSLPolyline gridLine, double lonstepDegrees, double latstepDegrees) {
        int no_lon_steps = ((int) Math.ceil(360 / lonstepDegrees)) / 2 + 1;
        int no_lat_steps = ((int) Math.ceil(180 / latstepDegrees)) / 2;
        int HALFDIVISIONS = SUBDIVISIONS / 2;

        int no_points = 2 * (no_lat_steps + no_lon_steps) * (HALFDIVISIONS + 3);
        FloatBuffer vertexBuffer = BufferUtils.newFloatBuffer(no_points * 3);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(no_points * 4);

        Vec3 v = new Vec3();
        double rotation;
        for (int j = 0; j < no_lon_steps; j++) {
            for (int k = -1; k <= 1; k += 2) {
                rotation = lonstepDegrees * j * k;
                Quat q = Quat.createRotation(Math.PI / 2 + Math.PI + (Math.PI / 180) * rotation, Vec3.YAxis);
                for (int i = 0; i <= HALFDIVISIONS; i++) {
                    v.x = GRID_RADIUS * Math.cos(-Math.PI / 2 + Math.PI * i / HALFDIVISIONS);
                    v.y = GRID_RADIUS * Math.sin(-Math.PI / 2 + Math.PI * i / HALFDIVISIONS);
                    v.z = 0.;
                    Vec3 rotv = q.rotateVector(v);

                    if (i == 0) {
                        BufferUtils.put3f(vertexBuffer, rotv);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(vertexBuffer, rotv);
                    colorBuffer.put(i % 2 == 0 ? color1 : color2);
                    if (i == HALFDIVISIONS) {
                        BufferUtils.put3f(vertexBuffer, rotv);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                }
            }
        }
        for (int j = 0; j < no_lat_steps; j++) {
            for (int k = -1; k <= 1; k += 2) {
                rotation = latstepDegrees * j * k;
                for (int i = 0; i <= HALFDIVISIONS; i++) {
                    double scale = Math.cos((Math.PI / 180.) * (90 - rotation));
                    v.y = GRID_RADIUS * scale;
                    v.x = GRID_RADIUS * Math.sqrt(1. - scale * scale) * Math.sin(2 * Math.PI * i / HALFDIVISIONS);
                    v.z = GRID_RADIUS * Math.sqrt(1. - scale * scale) * Math.cos(2 * Math.PI * i / HALFDIVISIONS);

                    if (i == 0) {
                        BufferUtils.put3f(vertexBuffer, v);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                    BufferUtils.put3f(vertexBuffer, v);
                    colorBuffer.put(i % 2 == 0 ? color1 : color2);
                    if (i == HALFDIVISIONS) {
                        BufferUtils.put3f(vertexBuffer, v);
                        colorBuffer.put(BufferUtils.colorNull);
                    }
                }
            }
        }
        vertexBuffer.rewind();
        colorBuffer.rewind();
        gridLine.setData(gl, vertexBuffer, colorBuffer);
    }

}
