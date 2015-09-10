package org.helioviewer.jhv.renderable.helpers;

import com.jogamp.opengl.GL2;

public class RenderableHelper {

    public static void drawCircle(GL2 gl, double x, double y, double r, int segments) {
        gl.glDisable(GL2.GL_TEXTURE_2D);
        {
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex2d(x, y);
            for (int n = 0; n <= segments; ++n) {
                double t = 2 * Math.PI * n / segments;
                gl.glVertex2d(x + Math.sin(t) * r, y + Math.cos(t) * r);
            }
            gl.glEnd();
        }
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    public static void drawRectangle(GL2 gl, double x0, double y0, double w, double h) {
        double x1 = x0 + w;
        double y1 = y0 + h;
        gl.glDisable(GL2.GL_TEXTURE_2D);
        {
            gl.glBegin(GL2.GL_QUADS);
            gl.glVertex2d(x0, -y0);
            gl.glVertex2d(x0, -y1);
            gl.glVertex2d(x1, -y1);
            gl.glVertex2d(x1, -y0);
            gl.glEnd();
        }
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }
}
