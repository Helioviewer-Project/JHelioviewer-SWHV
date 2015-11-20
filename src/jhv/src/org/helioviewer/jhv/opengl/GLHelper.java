package org.helioviewer.jhv.opengl;

import java.awt.Dimension;
import java.awt.Point;

import com.jogamp.opengl.GL2;

public class GLHelper {

    public static void drawCircleFront(GL2 gl, double x, double y, double r, int segments) {
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glVertex2f((float) x, (float) y);
        for (int n = 0; n <= segments; ++n) {
            double t = -2 * Math.PI * n / segments;
            gl.glVertex2f((float) (x + Math.sin(t) * r), (float) (y + Math.cos(t) * r));
        }
        gl.glEnd();
    }

    public static void drawCircleBack(GL2 gl, double x, double y, double r, int segments) {
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glVertex2f((float) x, (float) y);
        for (int n = 0; n <= segments; ++n) {
            double t = 2 * Math.PI * n / segments;
            gl.glVertex2f((float) (x + Math.sin(t) * r), (float) (y + Math.cos(t) * r));
        }
        gl.glEnd();
    }

    public static void drawRectangleFront(GL2 gl, double x0, double y0, double w, double h) {
        float x1 = (float) (x0 + w);
        float y1 = (float) (y0 + h);

        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex2f((float) x0, (float) -y0);
        gl.glVertex2f((float) x0, -y1);
        gl.glVertex2f(x1, -y1);
        gl.glVertex2f(x1, (float) -y0);
        gl.glEnd();
    }

    public static void drawRectangleBack(GL2 gl, double x0, double y0, double w, double h) {
        float x1 = (float) (x0 + w);
        float y1 = (float) (y0 + h);

        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex2f((float) x0, (float) -y0);
        gl.glVertex2f(x1, (float) -y0);
        gl.glVertex2f(x1, -y1);
        gl.glVertex2f((float) x0, -y1);
        gl.glEnd();
    }

    public static boolean unitScale = false;

    public static void lineWidth(GL2 gl, double w) {
        gl.glLineWidth((float) (w * (unitScale ? 1 : GLInfo.pixelScaleFloat[0])));
    }

    public static Point GL2AWTPoint(int x, int y) {
        return new Point((int) (x / GLInfo.pixelScaleFloat[0]), (int) (y / GLInfo.pixelScaleFloat[1]));
    }

    public static Dimension GL2AWTDimension(int x, int y) {
        return new Dimension((int) (x / GLInfo.pixelScaleFloat[0]), (int) (y / GLInfo.pixelScaleFloat[1]));
    }

}
