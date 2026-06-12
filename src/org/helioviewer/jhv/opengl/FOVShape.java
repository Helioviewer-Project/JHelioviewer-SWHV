package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Colors;

public class FOVShape {

    public static final int RECT_SUBDIVS = 32;
    public static final int CIRC_SUBDIVS = 90;
    private static final float SIZE_POINT = 0.02f;
    private static final double epsilon = 0.006;

    public static void putCenter(double centerX, double centerY, boolean flat, double lineWidth, byte[] color, BufVertex vexBuf) {
        double centerZ = computeZ(centerX, centerY, flat, lineWidth);
        vexBuf.putVertex((float) centerX, (float) centerY, (float) centerZ, SIZE_POINT, color);
    }

    public static double computeZ(double x, double y, boolean flat, double lineWidth) {
        if (flat)
            return 0;
        double radius = 1 + Math.max(epsilon, lineWidth);
        double n = radius * radius - x * x - y * y;
        return n > 0 ? Math.sqrt(n) : 0;
    }

    public static void putRectLine(double centerX, double centerY, double bw, double bh, boolean flat, double lineWidth, byte[] color, BufVertex vexBuf) {
        double x, y, z;

        for (int i = 0; i <= RECT_SUBDIVS; i++) {
            x = -bw + 2 * bw / RECT_SUBDIVS * i + centerX;
            y = bh + centerY;
            z = computeZ(x, y, flat, lineWidth);
            if (i == 0) { // first
                vexBuf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 1; i <= RECT_SUBDIVS; i++) {
            x = bw + centerX;
            y = bh - 2 * bh / RECT_SUBDIVS * i + centerY;
            z = computeZ(x, y, flat, lineWidth);
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 1; i <= RECT_SUBDIVS; i++) {
            x = bw - 2 * bw / RECT_SUBDIVS * i + centerX;
            y = -bh + centerY;
            z = computeZ(x, y, flat, lineWidth);
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 1; i <= RECT_SUBDIVS; i++) {
            x = -bw + centerX;
            y = -bh + 2 * bh / RECT_SUBDIVS * i + centerY;
            z = computeZ(x, y, flat, lineWidth);
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
            if (i == RECT_SUBDIVS) { // last
                vexBuf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
        }
    }

    public static void putCircLine(double centerX, double centerY, double r, boolean flat, double lineWidth, byte[] color, BufVertex vexBuf) {
        for (int i = 0; i <= CIRC_SUBDIVS; i++) {
            double t = i * 2. * Math.PI / CIRC_SUBDIVS;
            double x = centerX + Math.sin(t) * r;
            double y = centerY + Math.cos(t) * r;
            double z = computeZ(x, y, flat, lineWidth);

            if (i == 0) {
                vexBuf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
            vexBuf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
            if (i == CIRC_SUBDIVS) {
                vexBuf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
        }
    }

    private FOVShape() {}
}
