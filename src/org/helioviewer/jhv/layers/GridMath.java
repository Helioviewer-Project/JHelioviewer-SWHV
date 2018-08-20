package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;

import com.jogamp.opengl.GL2;

class GridMath {

    private static final int SUBDIVISIONS = 180;

    private static final byte[] radialLineColor = Colors.DarkGray;
    private static final byte[] axisNorthColor = Colors.Red;
    private static final byte[] axisSouthColor = Colors.Blue;
    private static final byte[] earthLineColor = Colors.Yellow;

    private static final byte[] color1 = Colors.Red;
    private static final byte[] color2 = Colors.Green;

    private static final float earthPointSize = 0.02f;

    private static final int LINEAR_STEPS = 1;

    private static final float AXIS_START = (float) (1. * Sun.Radius);
    private static final float AXIS_STOP = (float) (1.2 * Sun.Radius);

    private static final double GRID_RADIUS = Sun.Radius;
    private static final double EARTH_CIRCLE_RADIUS = GRID_RADIUS * 1.006;

    private static final int TENS_RADIUS = 3;
    private static final int END_RADIUS = TENS_RADIUS * 10;
    private static final int START_RADIUS = 2;

    static final int FLAT_STEPS_THETA = 24;
    static final int FLAT_STEPS_RADIAL = 10;

    static void initAxes(GL2 gl, GLSLLine axesLine) {
        Buf vexBuf = new Buf(8 * GLSLLine.stride);

        vexBuf.put4f(0, -AXIS_STOP, 0, 1).put4b(Colors.Null);
        vexBuf.repeat4f().put4b(axisSouthColor);

        vexBuf.put4f(0, -AXIS_START, 0, 1).put4b(axisSouthColor);
        vexBuf.repeat4f().put4b(Colors.Null);

        vexBuf.put4f(0, AXIS_START, 0, 1).put4b(Colors.Null);
        vexBuf.repeat4f().put4b(axisNorthColor);

        vexBuf.put4f(0, AXIS_STOP, 0, 1).put4b(axisNorthColor);
        vexBuf.repeat4f().put4b(Colors.Null);

        axesLine.setData(gl, vexBuf);
    }

    static void initEarthPoint(GL2 gl, GLSLShape earthPoint) {
        Buf vexBuf = new Buf(GLSLShape.stride);
        vexBuf.put4f(0, 0, (float) EARTH_CIRCLE_RADIUS, earthPointSize).put4b(earthLineColor);
        earthPoint.setData(gl, vexBuf);
    }

    static void initEarthCircles(GL2 gl, GLSLLine earthCircleLine) {
        int no_points = 2 * (SUBDIVISIONS + 3);
        Buf vexBuf = new Buf(no_points * GLSLLine.stride);

        Vec3 rotv = new Vec3(), v = new Vec3();
        Quat q = Quat.createRotation(Math.PI / 2, Vec3.XAxis);
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            v.x = EARTH_CIRCLE_RADIUS * Math.cos(2 * Math.PI * i / SUBDIVISIONS);
            v.y = EARTH_CIRCLE_RADIUS * Math.sin(2 * Math.PI * i / SUBDIVISIONS);
            v.z = 0.;
            rotv = q.rotateVector(v);
            if (i == 0) {
                vexBuf.put4f(rotv).put4b(Colors.Null);
            }
            vexBuf.put4f(rotv).put4b(earthLineColor);
        }
        vexBuf.put4f(rotv).put4b(Colors.Null);

        v = new Vec3();
        q = Quat.createRotation(Math.PI / 2, Vec3.YAxis);
        for (int i = 0; i <= SUBDIVISIONS; i++) {
            v.x = EARTH_CIRCLE_RADIUS * Math.cos(2 * Math.PI * i / SUBDIVISIONS);
            v.y = EARTH_CIRCLE_RADIUS * Math.sin(2 * Math.PI * i / SUBDIVISIONS);
            v.z = 0.;
            rotv = q.rotateVector(v);
            if (i == 0) {
                vexBuf.put4f(rotv).put4b(Colors.Null);
            }
            vexBuf.put4f(rotv).put4b(earthLineColor);
        }
        vexBuf.put4f(rotv).put4b(Colors.Null);

        earthCircleLine.setData(gl, vexBuf);
    }

    static void initRadialCircles(GL2 gl, GLSLLine radialCircleLine, GLSLLine radialThickLine, double unit, double step) {
        int no_lines = (int) Math.ceil(360 / step);
        int no_points = (END_RADIUS - START_RADIUS + 1 - TENS_RADIUS) * (SUBDIVISIONS + 3) + (LINEAR_STEPS + 3) * no_lines;
        Buf circleBuf = new Buf(no_points * GLSLLine.stride);
        int no_points_thick = TENS_RADIUS * (SUBDIVISIONS + 3);
        Buf thickBuf = new Buf(no_points_thick * GLSLLine.stride);

        for (int i = START_RADIUS; i <= END_RADIUS; i++) {
            Buf targetBuf = i % 10 == 0 ? thickBuf : circleBuf;

            for (int j = 0; j <= SUBDIVISIONS; j++) {
                float x = (float) (i * unit * Math.cos(2 * Math.PI * j / SUBDIVISIONS));
                float y = (float) (i * unit * Math.sin(2 * Math.PI * j / SUBDIVISIONS));

                if (j == 0) {
                    targetBuf.put4f(x, y, 0, 1).put4b(Colors.Null);
                }
                targetBuf.put4f(x, y, 0, 1).put4b(radialLineColor);
                if (j == SUBDIVISIONS) {
                    targetBuf.put4f(x, y, 0, 1).put4b(Colors.Null);
                }
            }
        }

        Vec3 v = new Vec3();
        double i = 0;
        for (int j = 0; j < no_lines; j++) {
            i += step;
            Quat q = Quat.createRotation((Math.PI / 180) * i, Vec3.ZAxis);

            for (int k = 0; k <= LINEAR_STEPS; k++) {
                v.x = (START_RADIUS + k * (END_RADIUS - START_RADIUS) / (double) LINEAR_STEPS) * unit;
                Vec3 rotv = q.rotateVector(v);

                if (k == 0) {
                    circleBuf.put4f(rotv).put4b(Colors.Null);
                }
                circleBuf.put4f(rotv).put4b(radialLineColor);
                if (k == LINEAR_STEPS) {
                    circleBuf.put4f(rotv).put4b(Colors.Null);
                }
            }
        }

        radialCircleLine.setData(gl, circleBuf);
        radialThickLine.setData(gl, thickBuf);
    }

    static void initFlatGrid(GL2 gl, GLSLLine flatLine, double aspect) {
        int no_points = (LINEAR_STEPS + 3) * (FLAT_STEPS_THETA + 1 + FLAT_STEPS_RADIAL + 1);
        Buf vexBuf = new Buf(no_points * GLSLLine.stride);

        for (int i = 0; i <= FLAT_STEPS_THETA; i++) {
            float start = (float) (aspect * (-0.5 + i / (double) FLAT_STEPS_THETA));
            for (int k = 0; k <= LINEAR_STEPS; k++) {
                float v = (float) (-0.5 + k / (double) LINEAR_STEPS);

                if (k == 0) {
                    vexBuf.put4f(start, v, 0, 1).put4b(Colors.Null);
                }
                vexBuf.put4f(start, v, 0, 1);
                vexBuf.put4b(i == FLAT_STEPS_THETA / 2 ? color2 : color1);
                if (k == LINEAR_STEPS) {
                    vexBuf.put4f(start, v, 0, 1).put4b(Colors.Null);
                }
            }
        }
        for (int i = 0; i <= FLAT_STEPS_RADIAL; i++) {
            float start = (float) (-0.5 + i / (double) FLAT_STEPS_RADIAL);
            for (int k = 0; k <= LINEAR_STEPS; k++) {
                float v = (float) (aspect * (-0.5 + k / (double) LINEAR_STEPS));

                if (k == 0) {
                    vexBuf.put4f(v, start, 0, 1).put4b(Colors.Null);
                }
                vexBuf.put4f(v, start, 0, 1);
                vexBuf.put4b(i == FLAT_STEPS_RADIAL / 2 ? color2 : color1);
                if (k == LINEAR_STEPS) {
                    vexBuf.put4f(v, start, 0, 1).put4b(Colors.Null);
                }
            }
        }

        flatLine.setData(gl, vexBuf);
    }

    static void initGrid(GL2 gl, GLSLLine gridLine, double lonstepDegrees, double latstepDegrees) {
        int no_lon_steps = ((int) Math.ceil(360 / lonstepDegrees)) / 2 + 1;
        int no_lat_steps = ((int) Math.ceil(180 / latstepDegrees)) / 2;
        int HALFDIVISIONS = SUBDIVISIONS / 2;
        int no_points = 2 * (no_lat_steps + no_lon_steps) * (HALFDIVISIONS + 3);
        Buf vexBuf = new Buf(no_points * GLSLLine.stride);

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
                        vexBuf.put4f(rotv).put4b(Colors.Null);
                    }
                    vexBuf.put4f(rotv).put4b(i % 2 == 0 ? color1 : color2);
                    if (i == HALFDIVISIONS) {
                        vexBuf.put4f(rotv).put4b(Colors.Null);
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
                        vexBuf.put4f(v).put4b(Colors.Null);
                    }
                    vexBuf.put4f(v).put4b(i % 2 == 0 ? color1 : color2);
                    if (i == HALFDIVISIONS) {
                        vexBuf.put4f(v).put4b(Colors.Null);
                    }
                }
            }
        }

        gridLine.setData(gl, vexBuf);
    }

}
