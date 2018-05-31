package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.math.Vec3;

import com.jogamp.opengl.GL2;

public class FOVShape {

    private static final int SUBDIVISIONS = 24;
    private static final double epsilon = 0.006;
    private static final float pointSize = 0.1f;

    private static final float[] color1 = BufferUtils.colorBlue;
    private static final float[] color2 = BufferUtils.colorWhite;

    private final FloatBuffer pointPosition = BufferUtils.newFloatBuffer(4);
    private final FloatBuffer pointColor = BufferUtils.newFloatBuffer(4);
    private final FloatBuffer linePosition = BufferUtils.newFloatBuffer((4 * (SUBDIVISIONS + 2)) * 3);
    private final FloatBuffer lineColor = BufferUtils.newFloatBuffer((4 * (SUBDIVISIONS + 2)) * 4);

    private final GLLine line = new GLLine();
    private final GLShape point = new GLShape();

    private final Vec3 center;
    private double tanX;
    private double tanY;

    public FOVShape(Vec3 c) {
        center = new Vec3(c.x, c.y, computeZ(c.x, c.y));
    }

    public void setAngles(double angleX, double angleY) {
        tanX = Math.tan(angleX) / 2.;
        tanY = Math.tan(angleY) / 2.;
    }

    private void computeCenter(GL2 gl) {
        BufferUtils.put4f(pointPosition, (float) center.x, (float) center.y, (float) center.z, pointSize);
        pointColor.put(color1);

        pointPosition.rewind();
        pointColor.rewind();
        point.setData(gl, pointPosition, pointColor);
    }

    private static double computeZ(double x, double y) {
        double n = 1 - x * x - y * y;
        return n > 0 ? epsilon + Math.sqrt(n) : epsilon;
    }

    private void computeLine(GL2 gl, double distance) {
        double x, y, z;
        double bw = distance * tanX;
        double bh = distance * tanY;

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + 2 * bw / SUBDIVISIONS * i + center.x;
            y = bh + center.y;
            z = computeZ(x, y);
            if (i == 0) {
                BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
                lineColor.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
            lineColor.put(i % 2 == 0 ? color1 : color2);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw + center.x;
            y = bh - 2 * bh / SUBDIVISIONS * i + center.y;
            z = computeZ(x, y);
            if (i == 0) {
                BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
                lineColor.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
            lineColor.put(i % 2 == 0 ? color1 : color2);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw - 2 * bw / SUBDIVISIONS * i + center.x;
            y = -bh + center.y;
            z = computeZ(x, y);
            if (i == 0) {
                BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
                lineColor.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
            lineColor.put(i % 2 == 0 ? color1 : color2);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + center.x;
            y = -bh + 2 * bh / SUBDIVISIONS * i + center.y;
            z = computeZ(x, y);
            if (i == 0) {
                BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
                lineColor.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(linePosition, (float) x, (float) y, (float) z);
            lineColor.put(i % 2 == 0 ? color1 : color2);
        }

        linePosition.rewind();
        lineColor.rewind();
        line.setData(gl, linePosition, lineColor);
    }

    public void render(GL2 gl, double distance, double aspect, double thickness, double pointFactor) {
        point.renderPoints(gl, pointFactor);
        computeLine(gl, distance);
        line.render(gl, aspect, thickness);
    }

    public void init(GL2 gl) {
        line.init(gl);
        point.init(gl);
        computeCenter(gl);
    }

    public void dispose(GL2 gl) {
        line.dispose(gl);
        point.dispose(gl);
    }

}
