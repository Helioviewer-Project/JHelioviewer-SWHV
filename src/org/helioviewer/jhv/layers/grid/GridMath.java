package org.helioviewer.jhv.layers.grid;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;

import com.jogamp.opengl.GL2;

public class GridMath {

    private static final int SUBDIVISIONS = 360;

    private static final byte[] radialLineColor = Colors.DarkGray;
    private static final byte[] axisNorthColor = Colors.Red;
    private static final byte[] axisSouthColor = Colors.Blue;
    private static final byte[] earthLineColor = Colors.Yellow;

    private static final byte[] color1 = Colors.Red;
    private static final byte[] color2 = Colors.Green;

    private static final float earthPointSize = 0.01f;

    private static final int LINEAR_STEPS = 1;

    private static final float AXIS_START = (float) Sun.Radius;
    private static final float AXIS_STOP = (float) (1.2 * Sun.Radius);

    public static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private static final double GRID_RADIUS = Sun.Radius + LINEWIDTH; // avoid intersecting solar surface
    private static final double EARTH_CIRCLE_RADIUS = GRID_RADIUS * 1.006;

    private static final int TENS_RADIUS = 3;
    private static final int END_RADIUS = TENS_RADIUS * 10;
    private static final int START_RADIUS = 2;

    public static final int FLAT_STEPS_THETA = 24;
    public static final int FLAT_STEPS_RADIAL = 10;

    public static void initAxes(GL2 gl, GLSLLine axesLine) {
        BufVertex vexBuf = new BufVertex(8 * GLSLLine.stride);

        vexBuf.putVertex(0, -AXIS_STOP, 0, 1, Colors.Null);
        vexBuf.repeatVertex(axisSouthColor);

        vexBuf.putVertex(0, -AXIS_START, 0, 1, axisSouthColor);
        vexBuf.repeatVertex(Colors.Null);

        vexBuf.putVertex(0, AXIS_START, 0, 1, Colors.Null);
        vexBuf.repeatVertex(axisNorthColor);

        vexBuf.putVertex(0, AXIS_STOP, 0, 1, axisNorthColor);
        vexBuf.repeatVertex(Colors.Null);

        axesLine.setVertex(gl, vexBuf);
    }

    public static void initEarthPoint(GL2 gl, GLSLShape earthPoint) {
        BufVertex vexBuf = new BufVertex(GLSLShape.stride);
        vexBuf.putVertex(0, 0, (float) EARTH_CIRCLE_RADIUS, earthPointSize, earthLineColor);
        earthPoint.setVertex(gl, vexBuf);
    }

    public static void initEarthCircles(GL2 gl, GLSLLine earthCircleLine) {
        int no_points = 2 * (SUBDIVISIONS + 3);
        BufVertex vexBuf = new BufVertex(no_points * GLSLLine.stride);
        Vec3 rotv = new Vec3(), v = new Vec3();

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double a = 2 * Math.PI * i / SUBDIVISIONS;
            v.x = EARTH_CIRCLE_RADIUS * Math.cos(a);
            v.y = EARTH_CIRCLE_RADIUS * Math.sin(a);
            v.z = 0.;
            rotv = Quat.X90.rotateVector(v);
            if (i == 0) {
                vexBuf.putVertex(rotv, Colors.Null);
            }
            vexBuf.putVertex(rotv, earthLineColor);
        }
        vexBuf.putVertex(rotv, Colors.Null);

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            double a = 2 * Math.PI * i / SUBDIVISIONS;
            v.x = EARTH_CIRCLE_RADIUS * Math.cos(a);
            v.y = EARTH_CIRCLE_RADIUS * Math.sin(a);
            v.z = 0.;
            rotv = Quat.Y90.rotateVector(v);
            if (i == 0) {
                vexBuf.putVertex(rotv, Colors.Null);
            }
            vexBuf.putVertex(rotv, earthLineColor);
        }
        vexBuf.putVertex(rotv, Colors.Null);

        earthCircleLine.setVertex(gl, vexBuf);
    }

    public static void initRadialCircles(GL2 gl, GLSLLine radialCircleLine, GLSLLine radialThickLine, double unit, double step) {
        int no_lines = (int) Math.ceil(360 / step);
        int no_points = (END_RADIUS - START_RADIUS + 1 - TENS_RADIUS) * (SUBDIVISIONS + 3) + (LINEAR_STEPS + 3) * no_lines;
        BufVertex circleBuf = new BufVertex(no_points * GLSLLine.stride);
        int no_points_thick = TENS_RADIUS * (SUBDIVISIONS + 3);
        BufVertex thickBuf = new BufVertex(no_points_thick * GLSLLine.stride);

        for (int i = START_RADIUS; i <= END_RADIUS; i++) {
            BufVertex targetBuf = i % 10 == 0 ? thickBuf : circleBuf;

            for (int j = 0; j <= SUBDIVISIONS; j++) {
                double a = 2 * Math.PI * j / SUBDIVISIONS;
                float x = (float) (i * unit * Math.cos(a));
                float y = (float) (i * unit * Math.sin(a));

                if (j == 0) {
                    targetBuf.putVertex(x, y, 0, 1, Colors.Null);
                }
                targetBuf.putVertex(x, y, 0, 1, radialLineColor);
                if (j == SUBDIVISIONS) {
                    targetBuf.putVertex(x, y, 0, 1, Colors.Null);
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
                    circleBuf.putVertex(rotv, Colors.Null);
                }
                circleBuf.putVertex(rotv, radialLineColor);
                if (k == LINEAR_STEPS) {
                    circleBuf.putVertex(rotv, Colors.Null);
                }
            }
        }

        radialCircleLine.setVertex(gl, circleBuf);
        radialThickLine.setVertex(gl, thickBuf);
    }

    public static void initFlatGrid(GL2 gl, GLSLLine flatLine, double aspect) {
        int no_points = (LINEAR_STEPS + 3) * (FLAT_STEPS_THETA + 1 + FLAT_STEPS_RADIAL + 1);
        BufVertex vexBuf = new BufVertex(no_points * GLSLLine.stride);

        for (int i = 0; i <= FLAT_STEPS_THETA; i++) {
            float start = (float) (aspect * (-0.5 + i / (double) FLAT_STEPS_THETA));
            for (int k = 0; k <= LINEAR_STEPS; k++) {
                float v = (float) (-0.5 + k / (double) LINEAR_STEPS);

                if (k == 0) {
                    vexBuf.putVertex(start, v, 0, 1, Colors.Null);
                }
                vexBuf.putVertex(start, v, 0, 1, i == FLAT_STEPS_THETA / 2 ? color2 : color1);
                if (k == LINEAR_STEPS) {
                    vexBuf.putVertex(start, v, 0, 1, Colors.Null);
                }
            }
        }
        for (int i = 0; i <= FLAT_STEPS_RADIAL; i++) {
            float start = (float) (-0.5 + i / (double) FLAT_STEPS_RADIAL);
            for (int k = 0; k <= LINEAR_STEPS; k++) {
                float v = (float) (aspect * (-0.5 + k / (double) LINEAR_STEPS));

                if (k == 0) {
                    vexBuf.putVertex(v, start, 0, 1, Colors.Null);
                }
                vexBuf.putVertex(v, start, 0, 1, i == FLAT_STEPS_RADIAL / 2 ? color2 : color1);
                if (k == LINEAR_STEPS) {
                    vexBuf.putVertex(v, start, 0, 1, Colors.Null);
                }
            }
        }

        flatLine.setVertex(gl, vexBuf);
    }

    public static void initGrid(GL2 gl, GLSLLine gridLine, double lonstepDegrees, double latstepDegrees) {
        int no_lon_steps = ((int) Math.ceil(360 / lonstepDegrees)) / 2 + 1;
        int no_lat_steps = ((int) Math.ceil(180 / latstepDegrees)) / 2;
        int HALFDIVISIONS = SUBDIVISIONS / 2;
        int no_points = 2 * (no_lat_steps + no_lon_steps) * (HALFDIVISIONS + 3);
        BufVertex vexBuf = new BufVertex(no_points * GLSLLine.stride);

        Vec3 v = new Vec3();
        double rotation;
        for (int j = 0; j < no_lon_steps; j++) {
            for (int k = -1; k <= 1; k += 2) {
                rotation = lonstepDegrees * j * k;
                Quat q = Quat.createRotation(Math.PI / 2 + Math.PI + (Math.PI / 180) * rotation, Vec3.YAxis);
                for (int i = 0; i <= HALFDIVISIONS; i++) {
                    double a = -Math.PI / 2 + Math.PI * i / HALFDIVISIONS;
                    v.x = GRID_RADIUS * Math.cos(a);
                    v.y = GRID_RADIUS * Math.sin(a);
                    v.z = 0.;
                    Vec3 rotv = q.rotateVector(v);

                    if (i == 0) {
                        vexBuf.putVertex(rotv, Colors.Null);
                    }
                    vexBuf.putVertex(rotv, i % 2 == 0 ? color1 : color2);
                    if (i == HALFDIVISIONS) {
                        vexBuf.putVertex(rotv, Colors.Null);
                    }
                }
            }
        }
        for (int j = 0; j < no_lat_steps; j++) {
            for (int k = -1; k <= 1; k += 2) {
                rotation = latstepDegrees * j * k;
                double scale = Math.cos((Math.PI / 180.) * (90 - rotation));
                for (int i = 0; i <= HALFDIVISIONS; i++) {
                    double a = 2 * Math.PI * i / HALFDIVISIONS;
                    v.y = GRID_RADIUS * scale;
                    v.x = GRID_RADIUS * Math.sqrt(1. - scale * scale) * Math.sin(a);
                    v.z = GRID_RADIUS * Math.sqrt(1. - scale * scale) * Math.cos(a);

                    if (i == 0) {
                        vexBuf.putVertex(v, Colors.Null);
                    }
                    vexBuf.putVertex(v, i % 2 == 0 ? color1 : color2);
                    if (i == HALFDIVISIONS) {
                        vexBuf.putVertex(v, Colors.Null);
                    }
                }
            }
        }

        gridLine.setVertex(gl, vexBuf);
    }

}
