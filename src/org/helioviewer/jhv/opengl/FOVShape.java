package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.Buf;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Viewport;

import com.jogamp.opengl.GL2;

public class FOVShape {

    private static final int SUBDIVISIONS = 24;
    private static final float SIZE_POINT = 0.02f;
    private static final double epsilon = 0.006;

    private final double thickness;

    private final GLSLLine line = new GLSLLine();
    private final Buf lineBuf = new Buf((4 * (SUBDIVISIONS + 1) + 2) * GLSLLine.stride);
    private final GLSLShape point = new GLSLShape();
    private final Buf pointBuf = new Buf(GLSLShape.stride);

    private double centerX = 0;
    private double centerY = 0;
    private double centerZ = computeZ(centerX, centerY);
    private double tanX;
    private double tanY;

    public FOVShape(double _thickness) {
        thickness = _thickness;
    }

    public void setTAngles(double _tanX, double _tanY) {
        tanX = _tanX;
        tanY = _tanY;
    }

    public void setCenter(double _centerX, double _centerY) {
        centerX = _centerX;
        centerY = _centerY;
        centerZ = computeZ(centerX, centerY);
    }

    private void computeCenter(GL2 gl, boolean highlight) {
        pointBuf.put4f((float) centerX, (float) centerY, (float) centerZ, SIZE_POINT);
        pointBuf.put4b(highlight ? Colors.Red : Colors.Blue);
        point.setData(gl, pointBuf);
    }

    private static double computeZ(double x, double y) {
        double n = 1 - x * x - y * y;
        return n > 0 ? epsilon + Math.sqrt(n) : epsilon;
    }

    private void computeLine(GL2 gl, double distance, boolean highlight) {
        double x, y, z;
        double bw = distance * tanX;
        double bh = distance * tanY;
        byte[] color = highlight ? Colors.Red : Colors.Blue;

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + 2 * bw / SUBDIVISIONS * i + centerX;
            y = bh + centerY;
            z = computeZ(x, y);
            if (i == 0) { // first
                lineBuf.put4f((float) x, (float) y, (float) z, 1).put4b(Colors.Null);
            }
            lineBuf.put4f((float) x, (float) y, (float) z, 1);
            lineBuf.put4b(i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw + centerX;
            y = bh - 2 * bh / SUBDIVISIONS * i + centerY;
            z = computeZ(x, y);
            lineBuf.put4f((float) x, (float) y, (float) z, 1);
            lineBuf.put4b(i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw - 2 * bw / SUBDIVISIONS * i + centerX;
            y = -bh + centerY;
            z = computeZ(x, y);
            lineBuf.put4f((float) x, (float) y, (float) z, 1);
            lineBuf.put4b(i % 2 == 0 ? color : Colors.White);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + centerX;
            y = -bh + 2 * bh / SUBDIVISIONS * i + centerY;
            z = computeZ(x, y);
            lineBuf.put4f((float) x, (float) y, (float) z, 1);
            lineBuf.put4b(i % 2 == 0 ? color : Colors.White);
            if (i == SUBDIVISIONS) { // last
                lineBuf.put4f((float) x, (float) y, (float) z, 1).put4b(Colors.Null);
            }
        }

        line.setData(gl, lineBuf);
    }

    public void render(GL2 gl, Viewport vp, double distance, double pointFactor, boolean highlight) {
        computeCenter(gl, highlight);
        point.renderPoints(gl, pointFactor);
        computeLine(gl, distance, highlight);
        line.render(gl, vp, thickness);
    }

    public void init(GL2 gl) {
        line.init(gl);
        point.init(gl);
    }

    public void dispose(GL2 gl) {
        line.dispose(gl);
        point.dispose(gl);
    }

}
