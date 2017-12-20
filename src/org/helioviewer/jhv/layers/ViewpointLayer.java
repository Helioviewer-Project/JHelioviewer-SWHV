package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Map;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.CameraOptionsPanel;
import org.helioviewer.jhv.camera.LoadPosition;
import org.helioviewer.jhv.camera.UpdateViewpoint;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.time.JHVDate;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

public class ViewpointLayer extends AbstractLayer implements MouseListener {

    private final CameraOptionsPanel optionsPanel;
    private static final double epsilon = 0.01;

    private static final float lineWidth = 2;

    private static final float[] color1 = BufferUtils.colorBlue;
    private static final float[] color2 = BufferUtils.colorWhite;

    private String timeString = null;

    public ViewpointLayer(JSONObject jo) {
        optionsPanel = new CameraOptionsPanel(jo);
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
            if (Displayer.getUpdateViewpoint() == UpdateViewpoint.equatorial) {
                gl.glPointSize(15f);
                gl.glBegin(GL2.GL_POINTS);
                for (Map.Entry<LoadPosition, Position.L> entry : UpdateViewpoint.equatorial.getPositions()) {
                    float[] c = entry.getKey().getTarget().getColor();
                    gl.glColor3f(c[0], c[1], c[2]);
                    Position.L p = entry.getValue();
                    gl.glVertex3f((float) (p.rad * Math.cos(p.lon)), (float) (p.rad * Math.sin(p.lon)), 0);
                }
                gl.glEnd();
            }

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

    private static final int MOUSE_OFFSET_X = 25;
    private static final int MOUSE_OFFSET_Y = 25;
    private final ArrayList<String> text = new ArrayList<>();
    private int mouseX, mouseY;

    @Override
    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        GLText.drawText(gl, vp, text, mouseX + MOUSE_OFFSET_X, mouseY + MOUSE_OFFSET_Y);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (Displayer.getUpdateViewpoint() == UpdateViewpoint.equatorial) {
            mouseX = e.getX();
            mouseY = e.getY();
            Camera camera = Displayer.getCamera();
            Vec3 v = CameraHelper.getVectorFromPlane(Displayer.getCamera(), Displayer.getActiveViewport(), mouseX, mouseY, Quat.ZERO, true);
            if (v == null)
                return;

            double width = camera.getWidth(), minDist = 10;
            String name = null;
            for (Map.Entry<LoadPosition, Position.L> entry : UpdateViewpoint.equatorial.getPositions()) {
                Position.L p = entry.getValue();
                double deltaX = Math.abs(p.rad * Math.cos(p.lon) - v.x);
                double deltaY = Math.abs(p.rad * Math.sin(p.lon) - v.y);
                double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / width;
                if (dist < minDist) {
                    minDist = dist;
                    name = entry.getKey().getTarget().toString();
                }
            }
            if (!text.isEmpty()) {
                text.clear();
                Displayer.display();
            }
            if (minDist < 0.01) {
                text.add(name);
                Displayer.display();
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
    }

    @Override
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);

        if (_enabled)
            ImageViewerGui.getInputController().addPlugin(this);
        else {
            ImageViewerGui.getInputController().removePlugin(this);
            CameraOptionsPanel.resetViewpoint();
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
        ImageViewerGui.getInputController().removePlugin(this);
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

    @Override
    public void serialize(JSONObject jo) {
        optionsPanel.serialize(jo);
    }

}
