package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Colors;

public class FOVShape {

    public static final int RECT_SUBDIVS = 24;
    public static final int CIRC_SUBDIVS = 90;
    private static final float SIZE_POINT = 0.01f;
    private static final double epsilon = 0.006;

    private double centerX = 0;
    private double centerY = 0;
    private double centerZ = computeZ(centerX, centerY);

    public void setCenter(double _centerX, double _centerY) {
        centerX = _centerX;
        centerY = _centerY;
        centerZ = computeZ(centerX, centerY);
    }

    public void putCenter(BufVertex buf, byte[] color) {
        buf.putVertex((float) centerX, (float) centerY, (float) centerZ, SIZE_POINT, color);
    }

    private static double computeZ(double x, double y) {
        double n = 1 - x * x - y * y;
        return n > 0 ? epsilon + Math.sqrt(n) : epsilon;
    }

    public void putRectLine(double bw, double bh, BufVertex buf, byte[] color) {
        double x, y, z;

        for (int i = 0; i <= RECT_SUBDIVS; i++) {
            x = -bw + 2 * bw / RECT_SUBDIVS * i + centerX;
            y = bh + centerY;
            z = computeZ(x, y);
            if (i == 0) { // first
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
            buf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= RECT_SUBDIVS; i++) {
            x = bw + centerX;
            y = bh - 2 * bh / RECT_SUBDIVS * i + centerY;
            z = computeZ(x, y);
            buf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= RECT_SUBDIVS; i++) {
            x = bw - 2 * bw / RECT_SUBDIVS * i + centerX;
            y = -bh + centerY;
            z = computeZ(x, y);
            buf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= RECT_SUBDIVS; i++) {
            x = -bw + centerX;
            y = -bh + 2 * bh / RECT_SUBDIVS * i + centerY;
            z = computeZ(x, y);
            buf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
            if (i == RECT_SUBDIVS) { // last
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
        }
    }

    public void putCircLine(double r, BufVertex buf, byte[] color) {
        for (int i = 0; i <= CIRC_SUBDIVS; i++) {
            double t = i * 2. * Math.PI / CIRC_SUBDIVS;
            double x = centerX + Math.sin(t) * r;
            double y = centerY + Math.cos(t) * r;
            double z = computeZ(x, y);

            if (i == 0) {
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
            buf.putVertex((float) x, (float) y, (float) z, 1, color);
            if (i == CIRC_SUBDIVS) {
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
        }
    }

}
