package org.helioviewer.gl3d.camera;

import java.awt.Color;
import java.awt.Component;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.plugin.renderable.Renderable;
import org.helioviewer.jhv.plugin.renderable.RenderableType;

public class RenderableCamera implements Renderable {

    private final Component optionsPanel;
    private final RenderableType type = new RenderableType("Camera");
    private static final double epsilon = 0.01;
    private static final Color firstcolor = Color.BLUE;
    private static final Color secondcolor = Color.WHITE;
    private boolean isVisible = false;

    public RenderableCamera() {
        this.optionsPanel = new GL3DCameraOptionsPanel(GL3DState.getActiveCamera());
    }

    @Override
    public void init(GL3DState state) {
    }

    @Override
    public void render(GL3DState state) {
        if (!isVisible)
            return;

        double width = GL3DState.getActiveCamera().getZTranslation() * Math.tan(GL3DState.getActiveCamera().getFOVAngleToDraw());
        double height = width;
        double scale = 1.;
        GL2 gl = state.gl;
        gl.glPushMatrix();
        gl.glMultMatrixd(GL3DState.getActiveCamera().getLocalRotation().toMatrix().transpose().m, 0);
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
    public void remove(GL3DState state) {
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
        return "N/A";
    }

}
