package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.base.Colors;

public class FOVShape {

    public static final int SUBDIVISIONS = 24;
    private static final float SIZE_POINT = 0.02f;
    private static final double epsilon = 0.006;

    private double centerX = 0;
    private double centerY = 0;
    private double centerZ = computeZ(centerX, centerY);
    private double tanX;
    private double tanY;

    public void setTAngles(double _tanX, double _tanY) {
        tanX = _tanX;
        tanY = _tanY;
    }

    public void setCenter(double _centerX, double _centerY) {
        centerX = _centerX;
        centerY = _centerY;
        centerZ = computeZ(centerX, centerY);
    }

    public void putCenter(Buf buf, byte[] color) {
        buf.put4f((float) centerX, (float) centerY, (float) centerZ, SIZE_POINT).put4b(color);
    }

    private static double computeZ(double x, double y) {
        double n = 1 - x * x - y * y;
        return n > 0 ? epsilon + Math.sqrt(n) : epsilon;
    }

    public void putLine(double distance, Buf buf, byte[] color) {
        double x, y, z;
        double bw = distance * tanX;
        double bh = distance * tanY;

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + 2 * bw / SUBDIVISIONS * i + centerX;
            y = bh + centerY;
            z = computeZ(x, y);
            if (i == 0) { // first
                buf.put4f((float) x, (float) y, (float) z, 1).put4b(Colors.Null);
            }
            buf.put4f((float) x, (float) y, (float) z, 1);
            buf.put4b(i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw + centerX;
            y = bh - 2 * bh / SUBDIVISIONS * i + centerY;
            z = computeZ(x, y);
            buf.put4f((float) x, (float) y, (float) z, 1);
            buf.put4b(i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw - 2 * bw / SUBDIVISIONS * i + centerX;
            y = -bh + centerY;
            z = computeZ(x, y);
            buf.put4f((float) x, (float) y, (float) z, 1);
            buf.put4b(i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + centerX;
            y = -bh + 2 * bh / SUBDIVISIONS * i + centerY;
            z = computeZ(x, y);
            buf.put4f((float) x, (float) y, (float) z, 1);
            buf.put4b(i % 2 == 0 ? color : Colors.White);
            if (i == SUBDIVISIONS) { // last
                buf.put4f((float) x, (float) y, (float) z, 1).put4b(Colors.Null);
            }
        }
    }

}
