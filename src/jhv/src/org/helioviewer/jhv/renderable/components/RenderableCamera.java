package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraOptionsPanel;
import org.helioviewer.jhv.camera.Viewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;

import com.jogamp.opengl.GL2;

public class RenderableCamera extends AbstractRenderable {

    private final CameraOptionsPanel optionsPanel;
    private static final double epsilon = 0.01;

    private static final Color firstcolor = Color.BLUE;
    private static final float oneRed = firstcolor.getRed() / 255f;
    private static final float oneGreen = firstcolor.getGreen() / 255f;
    private static final float oneBlue = firstcolor.getBlue() / 255f;

    private static final Color secondcolor = Color.WHITE;
    private static final float twoRed = secondcolor.getRed() / 255f;
    private static final float twoGreen = secondcolor.getGreen() / 255f;
    private static final float twoBlue = secondcolor.getBlue() / 255f;

    private String timeString = null;

    public RenderableCamera() {
        optionsPanel = new CameraOptionsPanel(Displayer.getCamera());
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.index])
            return;

        double width = camera.getDistance() * Math.tan(optionsPanel.getFOVAngle());
        double height = width;
        double scale = 1.;

        gl.glPushMatrix();
        gl.glMultMatrixd(camera.getOrientation().toMatrix().transpose().m, 0);
        {
            GLHelper.lineWidth(gl, 1);

            gl.glBegin(GL2.GL_LINE_LOOP);

            double x, y, z, n;
            double bw = width * scale / 2.;
            double bh = height * scale / 2.;
            int subdivisions = 10;
            for (int i = 0; i <= subdivisions; i++) {
                if (i % 2 == 0) {
                    gl.glColor3f(oneRed, oneGreen, oneBlue);
                } else {
                    gl.glColor3f(twoRed, twoGreen, twoBlue);
                }
                x = -bw + 2 * bw / subdivisions * i;
                y = bh;
                z = epsilon;
                n = 1 - x * x - y * y;
                if (n > 0) {
                    z += Math.sqrt(n);
                }
                gl.glVertex3f((float) x, (float) y, (float) z);
            }
            for (int i = 0; i <= subdivisions; i++) {
                if (i % 2 == 0) {
                    gl.glColor3f(oneRed, oneGreen, oneBlue);
                } else {
                    gl.glColor3f(twoRed, twoGreen, twoBlue);
                }
                x = bw;
                y = bh - 2 * bh / subdivisions * i;
                z = epsilon;
                n = 1 - x * x - y * y;
                if (n > 0) {
                    z += Math.sqrt(n);
                }
                gl.glVertex3f((float) x, (float) y, (float) z);
            }
            for (int i = 0; i <= subdivisions; i++) {
                if (i % 2 == 0) {
                    gl.glColor3f(oneRed, oneGreen, oneBlue);
                } else {
                    gl.glColor3f(twoRed, twoGreen, twoBlue);
                }
                x = bw - 2 * bw / subdivisions * i;
                y = -bh;
                z = epsilon;
                n = 1 - x * x - y * y;
                if (n > 0) {
                    z += Math.sqrt(n);
                }
                gl.glVertex3f((float) x, (float) y, (float) z);
            }
            for (int i = 0; i <= subdivisions; i++) {
                if (i % 2 == 0) {
                    gl.glColor3f(oneRed, oneGreen, oneBlue);
                } else {
                    gl.glColor3f(twoRed, twoGreen, twoBlue);
                }
                x = -bw;
                y = -bh + 2 * bh / subdivisions * i;
                z = epsilon;
                n = 1 - x * x - y * y;
                if (n > 0) {
                    z += Math.sqrt(n);
                }
                gl.glVertex3f((float) x, (float) y, (float) z);
            }
            gl.glEnd();
        }
        gl.glPopMatrix();
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Viewpoint";
    }

    @Override
    public String getTimeString() {
        return timeString;
    }

    public void setTimeString(String _timeString) {
        timeString = _timeString;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void dispose(GL2 gl) {
    }

}
