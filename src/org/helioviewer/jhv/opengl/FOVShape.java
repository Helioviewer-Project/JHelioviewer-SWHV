package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Colors;

public class FOVShape {

    public static final int RECT_SUBDIVS = 32;
    public static final int CIRC_SUBDIVS = 90;
    private static final float SIZE_POINT = 0.02f;
    private static final double epsilon = 0.006;

    private double centerX = 0;
    private double centerY = 0;
    private double lineWidth = epsilon;

    public void setCenter(double _centerX, double _centerY) {
        centerX = _centerX;
        centerY = _centerY;
    }

    public void setLineWidth(double _lineWidth) {
        lineWidth = _lineWidth;
    }

    public void putCenter(boolean flat, byte[] color, BufVertex vexBuf) {
        double centerZ = lineZ(centerX, centerY, flat);
        vexBuf.putVertex((float) centerX, (float) centerY, (float) centerZ, SIZE_POINT, color);
    }

    public static double computeZ(double x, double y, boolean flat) {
        return computeZ(x, y, flat, epsilon);
    }

    public static double computeZ(double x, double y, boolean flat, double _lineWidth) {
        if (flat)
            return 0;
        double radius = 1 + Math.max(epsilon, _lineWidth);
        double n = radius * radius - x * x - y * y;
        return n > 0 ? Math.sqrt(n) : 0;
    }

    private double lineZ(double x, double y, boolean flat) {
        return computeZ(x, y, flat, lineWidth);
    }

    public void putRectLine(double bw, double bh, boolean flat, byte[] color, BufVertex vexBuf) {
        double x, y, z;

        for (int i = 0; i <= RECT_SUBDIVS; i++) {
            x = -bw + 2 * bw / RECT_SUBDIVS * i + centerX;
            y = bh + centerY;
            z = lineZ(x, y, flat);
            if (i == 0) { // first
                vexBuf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 1; i <= RECT_SUBDIVS; i++) {
            x = bw + centerX;
            y = bh - 2 * bh / RECT_SUBDIVS * i + centerY;
            z = lineZ(x, y, flat);
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 1; i <= RECT_SUBDIVS; i++) {
            x = bw - 2 * bw / RECT_SUBDIVS * i + centerX;
            y = -bh + centerY;
            z = lineZ(x, y, flat);
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 1; i <= RECT_SUBDIVS; i++) {
            x = -bw + centerX;
            y = -bh + 2 * bh / RECT_SUBDIVS * i + centerY;
            z = lineZ(x, y, flat);
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
            if (i == RECT_SUBDIVS) { // last
                vexBuf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
        }
    }

    public void putCircLine(double r, boolean flat, byte[] color, BufVertex vexBuf) {
        for (int i = 0; i <= CIRC_SUBDIVS; i++) {
            double t = i * 2. * Math.PI / CIRC_SUBDIVS;
            double x = centerX + Math.sin(t) * r;
            double y = centerY + Math.cos(t) * r;
            double z = lineZ(x, y, flat);

            if (i == 0) {
                vexBuf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
            if (i == CIRC_SUBDIVS) {
                vexBuf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
        }
    }

}
