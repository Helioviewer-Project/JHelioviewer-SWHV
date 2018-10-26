package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Colors;

public class FOVShape {

    public static final int SUBDIVISIONS = 24;
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

    public void putLine(double bw, double bh, BufVertex buf, byte[] color) {
        double x, y, z;

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + 2 * bw / SUBDIVISIONS * i + centerX;
            y = bh + centerY;
            z = computeZ(x, y);
            if (i == 0) { // first
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
            buf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw + centerX;
            y = bh - 2 * bh / SUBDIVISIONS * i + centerY;
            z = computeZ(x, y);
            buf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw - 2 * bw / SUBDIVISIONS * i + centerX;
            y = -bh + centerY;
            z = computeZ(x, y);
            buf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + centerX;
            y = -bh + 2 * bh / SUBDIVISIONS * i + centerY;
            z = computeZ(x, y);
            buf.putVertex((float) x, (float) y, (float) z, 1, i % 2 == 0 ? color : Colors.White);
            if (i == SUBDIVISIONS) { // last
                buf.putVertex((float) x, (float) y, (float) z, 1, Colors.Null);
            }
        }
    }

}
