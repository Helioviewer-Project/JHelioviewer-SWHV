package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.CameraOptionsPanel;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.io.LoadPosition;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.time.JHVDate;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

public class ViewpointLayer extends AbstractLayer implements MouseListener {

    private static final double thickness = 0.002;
    private static final float planetSize = 0.2f;

    private final FOVShape fov = new FOVShape();
    private final GLShape planets = new GLShape();
    private final CameraOptionsPanel optionsPanel;

    private String timeString = null;

    public ViewpointLayer(JSONObject jo) {
        optionsPanel = new CameraOptionsPanel(jo);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        double tan = Math.tan(optionsPanel.getFOVAngle()) / 2;
        fov.setTAngles(tan, tan);
        double pointFactor = GLInfo.pixelScale[0] / (2 * camera.getFOV());
        Position viewpoint = camera.getViewpoint();

        gl.glPushMatrix();
        gl.glMultMatrixd(viewpoint.toQuat().toMatrix().transpose().m, 0);
        {
            Set<Map.Entry<LoadPosition, Position>> positions = Display.getUpdateViewpoint().getPositions();
            if (!positions.isEmpty()) {
                renderPlanets(gl, positions, pointFactor);
            }
            fov.render(gl, viewpoint.distance, vp.aspect, pointFactor, false);
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
        Set<Map.Entry<LoadPosition, Position>> positions = Display.getUpdateViewpoint().getPositions();
        if (!positions.isEmpty()) {
            mouseX = e.getX();
            mouseY = e.getY();
            Camera camera = Display.getCamera();
            Vec3 v = CameraHelper.getVectorFromPlane(camera, Display.getActiveViewport(), mouseX, mouseY, Quat.ZERO, true);
            if (v == null)
                return;

            double width = camera.getWidth(), minDist = 10;
            String name = null;
            for (Map.Entry<LoadPosition, Position> entry : positions) {
                Position p = entry.getValue();
                double deltaX = Math.abs(p.distance * Math.cos(p.lon) - v.x);
                double deltaY = Math.abs(p.distance * Math.sin(p.lon) - v.y);
                double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / width;
                if (dist < minDist) {
                    minDist = dist;
                    name = entry.getKey().getTarget().toString();
                }
            }
            if (!text.isEmpty()) {
                text.clear();
                Display.display();
            }
            if (minDist < 0.01) {
                text.add(name);
                Display.display();
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

    @Nullable
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
        fov.init(gl);
        planets.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        fov.dispose(gl);
        planets.dispose(gl);
    }

    @Override
    public void serialize(JSONObject jo) {
        optionsPanel.serialize(jo);
    }

    private void renderPlanets(GL2 gl, Set<Map.Entry<LoadPosition, Position>> positions, double pointFactor) {
        int size = positions.size();
        FloatBuffer planetPosition = BufferUtils.newFloatBuffer(4 * size);
        FloatBuffer planetColor = BufferUtils.newFloatBuffer(4 * size);

        for (Map.Entry<LoadPosition, Position> entry : positions) {
            Position p = entry.getValue();
            double theta = p.lat;
            double phi = p.lon;

            double y = p.distance * Math.cos(theta) * Math.sin(phi);
            double x = p.distance * Math.cos(theta) * Math.cos(phi);
            double z = p.distance * Math.sin(theta);

            BufferUtils.put4f(planetPosition, (float) x, (float) y, (float) z, planetSize);
            planetColor.put(entry.getKey().getTarget().getColor());
        }

        planetPosition.rewind();
        planetColor.rewind();
        planets.setData(gl, planetPosition, planetColor);
        planets.renderPoints(gl, pointFactor);
    }

}
