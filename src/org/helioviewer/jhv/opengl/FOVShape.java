package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.display.Viewport;

import com.jogamp.opengl.GL2;

public class FOVShape {

    private static final int SUBDIVISIONS = 24;
    private static final double epsilon = 0.006;
    private static final float pointSize = 0.01f;

    private final double thickness;

    private final FloatBuffer pointPosition = BufferUtils.newFloatBuffer(4);
    private final FloatBuffer pointColor = BufferUtils.newFloatBuffer(4);
    private final FloatBuffer linePosition = BufferUtils.newFloatBuffer((4 * (SUBDIVISIONS + 2) + 1) * 3);
    private final FloatBuffer lineColor = BufferUtils.newFloatBuffer((4 * (SUBDIVISIONS + 2) + 1) * 4);

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
        BufferUtils.put4f(pointPosition, (float) centerX, (float) centerY, (float) centerZ, pointSize);
        pointColor.put(highlight ? BufferUtils.colorRed : BufferUtils.colorBlue);

        pointPosition.rewind();
        pointColor.rewind();
        point.setData(gl, pointPosition, pointColor);
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

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + 2 * bw / SUBDIVISIONS * i + centerX;
            y = bh + centerY;
            z = computeZ(x, y);
            if (i == 0) {
                BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
                lineColor.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
            lineColor.put(i % 2 == 0 ? color : BufferUtils.colorWhite);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw + centerX;
            y = bh - 2 * bh / SUBDIVISIONS * i + centerY;
            z = computeZ(x, y);
            if (i == 0) {
                BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
                lineColor.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
            lineColor.put(i % 2 == 0 ? color : BufferUtils.colorWhite);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw - 2 * bw / SUBDIVISIONS * i + centerX;
            y = -bh + centerY;
            z = computeZ(x, y);
            if (i == 0) {
                BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
                lineColor.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
            lineColor.put(i % 2 == 0 ? color : BufferUtils.colorWhite);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + centerX;
            y = -bh + 2 * bh / SUBDIVISIONS * i + centerY;
            z = computeZ(x, y);
            if (i == 0) {
                BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
                lineColor.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
            lineColor.put(i % 2 == 0 ? color : BufferUtils.colorWhite);
            if (i == SUBDIVISIONS) {
                BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
                lineColor.put(BufferUtils.colorNull);
            }
        }

        linePosition.rewind();
        lineColor.rewind();
        line.setData(gl, linePosition, lineColor);
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
