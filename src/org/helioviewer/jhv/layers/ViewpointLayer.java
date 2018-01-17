package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

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
import org.helioviewer.jhv.opengl.GLLine;
import org.helioviewer.jhv.opengl.GLPoint;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.time.JHVDate;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

public class ViewpointLayer extends AbstractLayer implements MouseListener {

    private static final int SUBDIVISIONS = 12;
    private static final FloatBuffer positionBuffer = BufferUtils.newFloatBuffer((4 * (SUBDIVISIONS + 2)) * 3);
    private static final FloatBuffer colorBuffer = BufferUtils.newFloatBuffer((4 * (SUBDIVISIONS + 2)) * 4);
    private static final double epsilon = 0.001;
    private static final double thickness = 0.002;
    private static final float centerSize = 0.2f;

    private static final float[] color1 = BufferUtils.colorBlue;
    private static final float[] color2 = BufferUtils.colorWhite;

    private final GLLine line = new GLLine();
    private final GLPoint center = new GLPoint();
    private final GLPoint planets = new GLPoint();
    private final CameraOptionsPanel optionsPanel;

    private String timeString = null;

    public ViewpointLayer(JSONObject jo) {
        optionsPanel = new CameraOptionsPanel(jo);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        double width = camera.getViewpoint().distance * Math.tan(optionsPanel.getFOVAngle());
        computeLine(gl, width);

        gl.glPushMatrix();
        gl.glMultMatrixd(camera.getViewpoint().orientation.toMatrix().transpose().m, 0);
        {
            if (Displayer.getUpdateViewpoint() == UpdateViewpoint.equatorial) {
                computePlanets(gl, UpdateViewpoint.equatorial.getPositions());
                planets.render(gl, 1 / camera.getFOV());
            }
            center.render(gl, 1 / camera.getFOV());
            line.render(gl, vp.aspect, thickness);
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

        if (enabled)
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
        ImageViewerGui.getLayers().fireTimeUpdated(this);
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
        line.init(gl);
        planets.init(gl);
        center.init(gl);
        computeCenter(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        line.dispose(gl);
        planets.dispose(gl);
        center.dispose(gl);
    }

    @Override
    public void serialize(JSONObject jo) {
        optionsPanel.serialize(jo);
    }

    private void computeLine(GL2 gl, double size) {
        double x, y, z, n;
        double bw = size / 2.;
        double bh = size / 2.;

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw + 2 * bw / SUBDIVISIONS * i;
            y = bh;
            z = epsilon;
            n = 1 - x * x - y * y;
            if (n > 0) {
                z += Math.sqrt(n);
            }
            if (i == 0) {
                BufferUtils.put3f(positionBuffer, (float) x, (float) y, (float) z);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(positionBuffer, (float) x, (float) y, (float) z);
            colorBuffer.put(i % 2 == 0 ? color1 : color2);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw;
            y = bh - 2 * bh / SUBDIVISIONS * i;
            z = epsilon;
            n = 1 - x * x - y * y;
            if (n > 0) {
                z += Math.sqrt(n);
            }
            if (i == 0) {
                BufferUtils.put3f(positionBuffer, (float) x, (float) y, (float) z);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(positionBuffer, (float) x, (float) y, (float) z);
            colorBuffer.put(i % 2 == 0 ? color1 : color2);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = bw - 2 * bw / SUBDIVISIONS * i;
            y = -bh;
            z = epsilon;
            n = 1 - x * x - y * y;
            if (n > 0) {
                z += Math.sqrt(n);
            }
            if (i == 0) {
                BufferUtils.put3f(positionBuffer, (float) x, (float) y, (float) z);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(positionBuffer, (float) x, (float) y, (float) z);
            colorBuffer.put(i % 2 == 0 ? color1 : color2);
        }

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            x = -bw;
            y = -bh + 2 * bh / SUBDIVISIONS * i;
            z = epsilon;
            n = 1 - x * x - y * y;
            if (n > 0) {
                z += Math.sqrt(n);
            }
            if (i == 0) {
                BufferUtils.put3f(positionBuffer, (float) x, (float) y, (float) z);
                colorBuffer.put(BufferUtils.colorNull);
            }
            BufferUtils.put3f(positionBuffer, (float) x, (float) y, (float) z);
            colorBuffer.put(i % 2 == 0 ? color1 : color2);
        }

        positionBuffer.rewind();
        colorBuffer.rewind();
        line.setData(gl, positionBuffer, colorBuffer);
    }

    private void computeCenter(GL2 gl) {
        FloatBuffer centerPosition = BufferUtils.newFloatBuffer(4);
        FloatBuffer centerColor = BufferUtils.newFloatBuffer(4);

        BufferUtils.put4f(centerPosition, 0, 0, (float) (1 + epsilon), centerSize);
        centerColor.put(color1);

        centerPosition.rewind();
        centerColor.rewind();
        center.setData(gl, centerPosition, centerColor);
    }

    private void computePlanets(GL2 gl, Set<Map.Entry<LoadPosition, Position.L>> positions) {
        int size = positions.size();
        FloatBuffer planetPosition = BufferUtils.newFloatBuffer(4 * size);
        FloatBuffer planetColor = BufferUtils.newFloatBuffer(4 * size);

        for (Map.Entry<LoadPosition, Position.L> entry : UpdateViewpoint.equatorial.getPositions()) {
            Position.L p = entry.getValue();
            double theta = p.lat;
            double phi = p.lon;

            double y = p.rad * Math.cos(theta) * Math.sin(phi);
            double x = p.rad * Math.cos(theta) * Math.cos(phi);
            double z = p.rad * Math.sin(theta);

            BufferUtils.put4f(planetPosition, (float) x, (float) y, (float) z, centerSize);
            planetColor.put(entry.getKey().getTarget().getColor());
        }

        planetPosition.rewind();
        planetColor.rewind();
        planets.setData(gl, planetPosition, planetColor);
    }

}
