package org.helioviewer.jhv.opengl;

import java.awt.Point;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.Quat;

import com.jogamp.opengl.GL3;

public class GLHelper {

    public static void emitCircle(double radius, int subdivisions, int startStep, int endStep, Quat rotation, byte[] evenColor, byte[] oddColor, BufVertex vexBuf) {
        double[] point = {0, 0, 0}, rotated = {0, 0, 0};

        for (int i = startStep; i <= endStep; i++) {
            double angle = 2 * Math.PI * i / subdivisions;
            point[0] = radius * Math.cos(angle);
            point[1] = radius * Math.sin(angle);
            point[2] = 0.0;

            float x, y, z;
            if (rotation != null) {
                rotation.qxv(point, rotated);
                x = (float) rotated[0];
                y = (float) rotated[1];
                z = (float) rotated[2];
            } else {
                x = (float) point[0];
                y = (float) point[1];
                z = 0;
            }

            if (i == startStep)
                vexBuf.putVertex(x, y, z, 1, Colors.Null);
            vexBuf.putVertex(x, y, z, 1, i % 2 == 0 ? evenColor : oddColor);
            if (i == endStep)
                vexBuf.putVertex(x, y, z, 1, Colors.Null);
        }
    }

    public static void initCircleFront(GL3 gl, GLSLShape circle, double x, double y, double r, int segments, byte[] color) {
        int no_points = 2 * (segments + 1);
        BufVertex vexBuf = new BufVertex(no_points * GLSLShape.stride);
        for (int i = 0; i <= segments; ++i) {
            double t = 2 * Math.PI * i / segments;
            vexBuf.putVertex((float) (x + Math.sin(t) * r), (float) (y + Math.cos(t) * r), 0, 1, color);
            vexBuf.putVertex((float) x, (float) y, 0, 1, color);
        }
        circle.setVertex(gl, vexBuf);
    }

    public static void initRectangleFront(GL3 gl, GLSLShape rectangle, double x0, double y0, double w, double h, byte[] color) {
        BufVertex vexBuf = new BufVertex(4 * GLSLShape.stride);
        vexBuf.putQuad2DStrip((float) x0, (float) y0, (float) (x0 + w), (float) (y0 + h), color);
        rectangle.setVertex(gl, vexBuf);
    }

    public static Point GL2AWTPoint(int x, int y) {
        return new Point((int) (x / GLInfo.pixelScale[0] + .5), (int) (y / GLInfo.pixelScale[1] + .5));
    }

}
