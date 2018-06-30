package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Viewport;

import com.jogamp.opengl.GL2;

public class FOVShape {

    private static final int SUBDIVISIONS = 24;
    private static final float SIZE_POINT = 0.02f;
    private static final double epsilon = 0.006;

    private final double thickness;

    private final GLSLPolyline line = new GLSLPolyline();
    private final GLSLShape point = new GLSLShape();

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
        FloatBuffer vertexBuffer = BufferUtils.newFloatBuffer(4);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(4);
        BufferUtils.put4f(vertexBuffer, (float) centerX, (float) centerY, (float) centerZ, SIZE_POINT);
        colorBuffer.put(highlight ? BufferUtils.colorRed : BufferUtils.colorBlue);

        vertexBuffer.rewind();
        colorBuffer.rewind();
        point.setData(gl, vertexBuffer, colorBuffer);
    }

    private static double computeZ(double x, double y) {
        double n = 1 - x * x - y * y;
        return n > 0 ? epsilon + Math.sqrt(n) : epsilon;
    }

    private void computeLine(GL2 gl, double distance, boolean highlight) {
        double x, y, z;
        double bw = distance * tanX;
        double bh = distance * tanY;
        float[] color = highlight ? BufferUtils.colorRed : BufferUtils.colorBlue;

        int no_points = 4 * (SUBDIVISIONS + 2) + 1;
        FloatBuffer vertexBuffer = BufferUtils.newFloatBuffer(no_points * 4);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(no_points * 4);

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + 2 * bw / SUBDIVISIONS * i + centerX;
            y = bh + centerY;
            z = computeZ(x, y);
            if (i == 0) { // first
                BufferUtils.put4f(vertexBuffer, (float) x, (float) y, (float) z, 1);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put4f(vertexBuffer, (float) x, (float) y, (float) z, 1);
            colorBuffer.put(i % 2 == 0 ? color : BufferUtils.colorWhite);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw + centerX;
            y = bh - 2 * bh / SUBDIVISIONS * i + centerY;
            z = computeZ(x, y);
            BufferUtils.put4f(vertexBuffer, (float) x, (float) y, (float) z, 1);
            colorBuffer.put(i % 2 == 0 ? color : BufferUtils.colorWhite);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw - 2 * bw / SUBDIVISIONS * i + centerX;
            y = -bh + centerY;
            z = computeZ(x, y);
            BufferUtils.put4f(vertexBuffer, (float) x, (float) y, (float) z, 1);
            colorBuffer.put(i % 2 == 0 ? color : BufferUtils.colorWhite);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + centerX;
            y = -bh + 2 * bh / SUBDIVISIONS * i + centerY;
            z = computeZ(x, y);
            BufferUtils.put4f(vertexBuffer, (float) x, (float) y, (float) z, 1);
            colorBuffer.put(i % 2 == 0 ? color : BufferUtils.colorWhite);
            if (i == SUBDIVISIONS) { // last
                BufferUtils.put4f(vertexBuffer, (float) x, (float) y, (float) z, 1);
                colorBuffer.put(BufferUtils.colorNull);
            }
        }

        vertexBuffer.rewind();
        colorBuffer.rewind();
        line.setData(gl, vertexBuffer, colorBuffer);
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
