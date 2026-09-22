package org.helioviewer.jhv.layers.grid;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;

public class GridMath {
    private static final int SUBDIVISIONS = 360;

    private static final byte[] radialLineColor = Colors.DarkGray.bytes();
    private static final byte[] axisNorthColor = Colors.Red.bytes();
    private static final byte[] axisSouthColor = Colors.Blue.bytes();
    private static final byte[] earthLineColor = Colors.Yellow.bytes();

    private static final float earthPointSize = 0.02f;

    private static final int LINEAR_STEPS = 1;

    private static final float AXIS_START = (float) Sun.Radius;
    private static final float AXIS_STOP = (float) (1.2 * Sun.Radius);

    public static final double LINEWIDTH = GLSLLine.LINEWIDTH_BASIC;
    private static final double GRID_RADIUS = Sun.Radius + 2 * LINEWIDTH; // avoid intersecting solar surface
    private static final double EARTH_CIRCLE_RADIUS = GRID_RADIUS * 1.006;

    private static final int TENS_RADIUS = 3;
    private static final int END_RADIUS = TENS_RADIUS * 10;
    private static final int START_RADIUS = 2;

    public static void initAxes(GLSLLine axesLine) {
        BufVertex vexBuf = new BufVertex(8);

        vexBuf.startLine(0, -AXIS_STOP, 0, 1, axisSouthColor);
        vexBuf.putVertex(0, -AXIS_START, 0, 1, axisSouthColor);
        vexBuf.endLine();

        vexBuf.startLine(0, AXIS_START, 0, 1, axisNorthColor);
        vexBuf.putVertex(0, AXIS_STOP, 0, 1, axisNorthColor);
        vexBuf.endLine();

        axesLine.uploadAndClear(vexBuf);
    }

    public static void initEarthPoint(GLSLShape earthPoint) {
        BufVertex vexBuf = new BufVertex(1);
        vexBuf.putVertex(0, 0, (float) EARTH_CIRCLE_RADIUS, earthPointSize, earthLineColor);
        earthPoint.uploadAndClear(vexBuf);
    }

    public static void initEarthCircles(GLSLLine earthCircleLine) {
        int no_points = 2 * (SUBDIVISIONS + 3);
        BufVertex vexBuf = new BufVertex(no_points);
        GLHelper.emitCircle(EARTH_CIRCLE_RADIUS, SUBDIVISIONS, 0, SUBDIVISIONS, Quat.X90, earthLineColor, earthLineColor, vexBuf);
        GLHelper.emitCircle(EARTH_CIRCLE_RADIUS, SUBDIVISIONS, 0, SUBDIVISIONS, Quat.Y90, earthLineColor, earthLineColor, vexBuf);
        earthCircleLine.uploadAndClear(vexBuf);
    }

    public static void initRadialCircles(GLSLLine radialCircleLine, GLSLLine radialThickLine, double unit, double step) {
        int no_lines = (int) Math.ceil(360 / step);
        int no_points = (END_RADIUS - START_RADIUS + 1 - TENS_RADIUS) * (SUBDIVISIONS + 3) + (LINEAR_STEPS + 3) * no_lines;
        BufVertex circleBuf = new BufVertex(no_points);
        int no_points_thick = TENS_RADIUS * (SUBDIVISIONS + 3);
        BufVertex thickBuf = new BufVertex(no_points_thick);

        for (int i = START_RADIUS; i <= END_RADIUS; i++) {
            BufVertex targetBuf = i % 10 == 0 ? thickBuf : circleBuf;

            for (int j = 0; j <= SUBDIVISIONS; j++) {
                double a = 2 * Math.PI * j / SUBDIVISIONS;
                float x = (float) (i * unit * Math.cos(a));
                float y = (float) (i * unit * Math.sin(a));

                if (j == 0)
                    targetBuf.startLine(x, y, 0, 1, radialLineColor);
                else
                    targetBuf.putVertex(x, y, 0, 1, radialLineColor);
                if (j == SUBDIVISIONS)
                    targetBuf.endLine();
            }
        }

        double i = 0;
        for (int j = 0; j < no_lines; j++) {
            i += step;
            Quat q = Quat.createAxisZ((Math.PI / 180) * i);

            for (int k = 0; k <= LINEAR_STEPS; k++) {
                double radius = (START_RADIUS + k * (END_RADIUS - START_RADIUS) / (double) LINEAR_STEPS) * unit;
                Vec3 rotv = q.rotateVector(new Vec3(radius, 0, 0));

                if (k == 0)
                    circleBuf.startLine(rotv, radialLineColor);
                else
                    circleBuf.putVertex(rotv, radialLineColor);
                if (k == LINEAR_STEPS)
                    circleBuf.endLine();
            }
        }

        radialCircleLine.uploadAndClear(circleBuf);
        radialThickLine.uploadAndClear(thickBuf);
    }

    public static void initGrid(GLSLLine gridLine, double lonstepDegrees, double latstepDegrees, byte[] color) {
        // meridians at multiples of lonstep in (-180, 180], parallels at multiples of latstep in (-90, 90), as the labels
        int lonMin = 1 - (int) Math.ceil(180 / lonstepDegrees);
        int lonMax = (int) (180 / lonstepDegrees);
        int latMax = (int) Math.ceil(90 / latstepDegrees) - 1;
        int HALFDIVISIONS = SUBDIVISIONS / 2;
        int no_points = (lonMax - lonMin + 1 + 2 * latMax + 1) * (HALFDIVISIONS + 3);
        BufVertex vexBuf = new BufVertex(no_points);

        for (int j = lonMin; j <= lonMax; j++) {
            Quat q = Quat.createAxisY(Math.PI / 2 + Math.PI + (Math.PI / 180) * lonstepDegrees * j);
            for (int i = 0; i <= HALFDIVISIONS; i++) {
                double a = -Math.PI / 2 + Math.PI * i / HALFDIVISIONS;
                Vec3 rotv = q.rotateVector(new Vec3(GRID_RADIUS * Math.cos(a), GRID_RADIUS * Math.sin(a), 0));

                if (i == 0)
                    vexBuf.startLine(rotv, color);
                else
                    vexBuf.putVertex(rotv, color);
                if (i == HALFDIVISIONS)
                    vexBuf.endLine();
            }
        }
        for (int j = -latMax; j <= latMax; j++) {
            double scale = Math.cos((Math.PI / 180.) * (90 - latstepDegrees * j));
            double radialScale = Math.sqrt(1. - scale * scale);
            for (int i = 0; i <= HALFDIVISIONS; i++) {
                double a = 2 * Math.PI * i / HALFDIVISIONS;
                Vec3 v = new Vec3(
                        GRID_RADIUS * radialScale * Math.sin(a),
                        GRID_RADIUS * scale,
                        GRID_RADIUS * radialScale * Math.cos(a));

                if (i == 0)
                    vexBuf.startLine(v, color);
                else
                    vexBuf.putVertex(v, color);
                if (i == HALFDIVISIONS)
                    vexBuf.endLine();
            }
        }

        gridLine.uploadAndClear(vexBuf);
    }

}
