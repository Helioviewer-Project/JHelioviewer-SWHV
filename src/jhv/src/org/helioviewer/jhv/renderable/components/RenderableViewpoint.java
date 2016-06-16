package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;

import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraOptionsPanel;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;

import com.jogamp.opengl.GL2;

public class RenderableViewpoint extends AbstractRenderable {

    private final CameraOptionsPanel optionsPanel;
    private static final double epsilon = 0.01;

    private static final float lineWidth = 2;

    private static final float[] color1 = new float[] { Color.BLUE.getRed() / 255f, Color.BLUE.getGreen() / 255f, Color.BLUE.getBlue() / 255f };
    private static final float[] color2 = new float[] { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f };

    private String timeString = null;

    public RenderableViewpoint() {
        optionsPanel = new CameraOptionsPanel();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        double width = camera.getViewpoint().distance * Math.tan(optionsPanel.getFOVAngle());
        double height = width;
        double scale = 1.;

        gl.glPushMatrix();
        gl.glMultMatrixd(camera.getViewpoint().orientation.toMatrix().transpose().m, 0);
        {
            gl.glLineWidth(lineWidth);

            gl.glBegin(GL2.GL_LINE_LOOP);

            double x, y, z, n;
            double bw = width * scale / 2.;
            double bh = height * scale / 2.;
            int subdivisions = 10;
            for (int i = 0; i <= subdivisions; i++) {
                if (i % 2 == 0) {
                    gl.glColor3f(color1[0], color1[1], color1[2]);
                } else {
                    gl.glColor3f(color2[0], color2[1], color2[2]);
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
                    gl.glColor3f(color1[0], color1[1], color1[2]);
                } else {
                    gl.glColor3f(color2[0], color2[1], color2[2]);
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
                    gl.glColor3f(color1[0], color1[1], color1[2]);
                } else {
                    gl.glColor3f(color2[0], color2[1], color2[2]);
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
                    gl.glColor3f(color1[0], color1[1], color1[2]);
                } else {
                    gl.glColor3f(color2[0], color2[1], color2[2]);
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

    public void fireTimeUpdated(JHVDate time) {
        timeString = time.toString();
        ImageViewerGui.getCarringtonStatusPanel().update(time);
        ImageViewerGui.getRenderableContainer().fireTimeUpdated(this);
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
