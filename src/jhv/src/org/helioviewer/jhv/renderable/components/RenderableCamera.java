package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;
import java.util.Date;

import org.helioviewer.base.datetime.TimeUtils;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.camera.GL3DCameraOptionsPanel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.gui.Renderable;
import org.helioviewer.jhv.renderable.gui.RenderableType;

import com.jogamp.opengl.GL2;

public class RenderableCamera implements Renderable {

    private final Component optionsPanel;
    private final RenderableType type = new RenderableType("Camera");
    private static final double epsilon = 0.01;

    private static final Color firstcolor = Color.BLUE;
    private static final float oneRed = firstcolor.getRed() / 255f;
    private static final float oneGreen = firstcolor.getGreen() / 255f;
    private static final float oneBlue = firstcolor.getBlue() / 255f;

    private static final Color secondcolor = Color.WHITE;
    private static final float twoRed = secondcolor.getRed() / 255f;
    private static final float twoGreen = secondcolor.getGreen() / 255f;
    private static final float twoBlue = secondcolor.getBlue() / 255f;

    private boolean isVisible = false;
    private String timeString = null;

    public RenderableCamera() {
        this.optionsPanel = new GL3DCameraOptionsPanel(Layers.getActiveCamera());
    }

    @Override
    public void render(GL2 gl) {
        if (!isVisible)
            return;

        GL3DCamera activeCamera = Layers.getActiveCamera();
        double width = activeCamera.getZTranslation() * Math.tan(activeCamera.getFOVAngleToDraw());
        double height = width;
        double scale = 1.;

        gl.glPushMatrix();
        gl.glMultMatrixd(activeCamera.getLocalRotation().toMatrix().transpose().m, 0);
        {
            gl.glLineWidth(2.5f);

            gl.glDisable(GL2.GL_TEXTURE_2D);
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
            gl.glEnable(GL2.GL_TEXTURE_2D);
        }
        gl.glPopMatrix();
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public RenderableType getType() {
        return this.type;
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "Camera";
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    @Override
    public String getTimeString() {
        return timeString;
    }

    public void setTimeString(Date date) {
        timeString = TimeUtils.utcDateFormat.format(date);
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public boolean isActiveImageLayer() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void dispose(GL2 gl) {
    }

}
