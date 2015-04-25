package org.helioviewer.jhv.renderable;

import java.awt.Color;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraOptionsPanel;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;

import com.jogamp.opengl.GL2;

public class RenderableCamera implements Renderable {

    private final Component optionsPanel;
    private final RenderableType type = new RenderableType("Camera");
    private static final double epsilon = 0.01;
    private static final Color firstcolor = Color.BLUE;
    private static final Color secondcolor = Color.WHITE;
    private boolean isVisible = false;
    private String timeString = "N/A";

    public RenderableCamera() {
        this.optionsPanel = new GL3DCameraOptionsPanel(Displayer.getActiveCamera());
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void render(GL2 gl) {
        if (!isVisible)
            return;

        GL3DCamera activeCamera = Displayer.getActiveCamera();
        double width = activeCamera.getZTranslation() * Math.tan(activeCamera.getFOVAngleToDraw());
        double height = width;
        double scale = 1.;

        gl.glPushMatrix();
        gl.glMultMatrixd(activeCamera.getLocalRotation().toMatrix().transpose().m, 0);
        {
            gl.glLineWidth(2.5f);
            gl.glBegin(GL2.GL_LINE_LOOP);

            double bw = width * scale / 2.;
            double bh = height * scale / 2.;
            int subdivisions = 10;
            for (int i = 0; i <= subdivisions; i++) {
                if (i % 2 == 0) {
                    gl.glColor3d(firstcolor.getRed() / 255., firstcolor.getGreen() / 255., firstcolor.getBlue() / 255.);
                } else {
                    gl.glColor3d(secondcolor.getRed() / 255., secondcolor.getGreen() / 255., secondcolor.getBlue() / 255.);
                }
                double x = -bw + 2 * bw / subdivisions * i;
                double y = bh;
                double z = epsilon;
                if (x * x + y * y < 1) {
                    z += Math.sqrt(1 - x * x - y * y);
                }
                gl.glVertex3d(x, y, z);
            }
            for (int i = 0; i <= subdivisions; i++) {
                if (i % 2 == 0) {
                    gl.glColor3d(firstcolor.getRed() / 255., firstcolor.getGreen() / 255., firstcolor.getBlue() / 255.);
                } else {
                    gl.glColor3d(secondcolor.getRed() / 255., secondcolor.getGreen() / 255., secondcolor.getBlue() / 255.);
                }
                double x = bw;
                double y = bh - 2 * bh / subdivisions * i;
                double z = epsilon;
                if (x * x + y * y < 1) {
                    z += Math.sqrt(1 - x * x - y * y);
                }
                gl.glVertex3d(x, y, z);
            }
            for (int i = 0; i <= subdivisions; i++) {
                if (i % 2 == 0) {
                    gl.glColor3d(firstcolor.getRed() / 255., firstcolor.getGreen() / 255., firstcolor.getBlue() / 255.);
                } else {
                    gl.glColor3d(secondcolor.getRed() / 255., secondcolor.getGreen() / 255., secondcolor.getBlue() / 255.);
                }
                double x = bw - 2 * bw / subdivisions * i;
                double y = -bh;
                double z = epsilon;
                if (x * x + y * y < 1) {
                    z += Math.sqrt(1 - x * x - y * y);
                }
                gl.glVertex3d(x, y, z);
            }
            for (int i = 0; i <= subdivisions; i++) {
                if (i % 2 == 0) {
                    gl.glColor3d(firstcolor.getRed() / 255., firstcolor.getGreen() / 255., firstcolor.getBlue() / 255.);
                } else {
                    gl.glColor3d(secondcolor.getRed() / 255., secondcolor.getGreen() / 255., secondcolor.getBlue() / 255.);
                }
                double x = -bw;
                double y = -bh + 2 * bh / subdivisions * i;
                double z = epsilon;
                if (x * x + y * y < 1) {
                    z += Math.sqrt(1 - x * x - y * y);
                }
                gl.glVertex3d(x, y, z);
            }
            gl.glEnd();
        }
        gl.glPopMatrix();
    }

    @Override
    public void remove(GL2 gl) {
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
        return this.timeString;
    }

    public void setTimeString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        this.timeString = sdf.format(date);
    }

}
