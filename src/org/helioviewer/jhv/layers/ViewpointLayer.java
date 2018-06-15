package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.FloatArray;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.CameraOptionsPanel;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.io.LoadPosition;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.time.JHVDate;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

public class ViewpointLayer extends AbstractLayer implements MouseListener {

    private static final long ORBIT_DELTA_INTERPOLATE = 3 * 60 * 1000;
    private static final double fovThickness = 0.002;
    private static final double orbitThickness = 0.002;
    private static final float planetSize = 5f;

    private final FOVShape fov = new FOVShape(fovThickness);
    private final GLSLLine orbits = new GLSLLine();
    private final GLSLShape planets = new GLSLShape();
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
        double pixFactor = vp.height / (2 * camera.getWidth());
        Position viewpoint = camera.getViewpoint();

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());
        {
            Collection<LoadPosition> loadPositions = Display.getUpdateViewpoint().getLoadPositions();
            if (!loadPositions.isEmpty()) {
                renderPlanets(gl, loadPositions, vp.aspect);
            }
            fov.render(gl, viewpoint.distance, vp.aspect, pixFactor, false);
        }
        Transform.popView();
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
        Collection<LoadPosition> loadPositions = Display.getUpdateViewpoint().getLoadPositions();
        if (!loadPositions.isEmpty()) {
            mouseX = e.getX();
            mouseY = e.getY();
            Camera camera = Display.getCamera();
            Vec3 v = CameraHelper.getVectorFromPlane(camera, Display.getActiveViewport(), mouseX, mouseY, Quat.ZERO, true);
            if (v == null)
                return;

            long time = Movie.getTime().milli, start = Movie.getStartTime(), end = Movie.getEndTime();

            double width = camera.getWidth(), minDist = 5;
            String name = null;
            for (LoadPosition loadPosition : loadPositions) {
                if (!loadPosition.isLoaded())
                    continue;

                Vec3 p = loadPosition.getInterpolatedHG(time, start, end);
                double deltaX = Math.abs(p.x * Math.cos(p.y) - v.x);
                double deltaY = Math.abs(p.x * Math.sin(p.y) - v.y);
                double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / width;
                if (dist < minDist) {
                    minDist = dist;
                    name = loadPosition.getTarget().toString();
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

        if (enabled) {
            ImageViewerGui.getInputController().addPlugin(this);
            optionsPanel.syncViewpoint();
        } else {
            ImageViewerGui.getInputController().removePlugin(this);
            Display.setViewpointUpdate(UpdateViewpoint.observer);
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
        ImageViewerGui.getInputController().removePlugin(this);
        optionsPanel.deactivate();
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
    public boolean isDownloading() {
        return optionsPanel.isDownloading();
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
        fov.init(gl);
        orbits.init(gl);
        planets.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        fov.dispose(gl);
        orbits.dispose(gl);
        planets.dispose(gl);
    }

    @Override
    public void serialize(JSONObject jo) {
        optionsPanel.serialize(jo);
    }

    private void renderPlanets(GL2 gl, Collection<LoadPosition> loadPositions, double aspect) {
        int size = loadPositions.size();
        FloatBuffer planetPosition = BufferUtils.newFloatBuffer(4 * size);
        FloatBuffer planetColor = BufferUtils.newFloatBuffer(4 * size);
        FloatArray orbitPosition = new FloatArray();
        FloatArray orbitColor = new FloatArray();

        long t, time = Movie.getTime().milli, start = Movie.getStartTime(), end = Movie.getEndTime();

        for (LoadPosition loadPosition : loadPositions) {
            if (!loadPosition.isLoaded())
                continue;

            float[] color = loadPosition.getTarget().getColor();

            Vec3 v = loadPosition.getInterpolatedHG(time, start, end);
            float x = (float) (v.x * Math.cos(v.z) * Math.cos(v.y));
            float y = (float) (v.x * Math.cos(v.z) * Math.sin(v.y));
            float z = (float) (v.x * Math.sin(v.z));
            BufferUtils.put4f(planetPosition, x, y, z, planetSize);
            planetColor.put(color);

            t = start;
            loadPosition.getInterpolatedArray(orbitPosition, t, start, end);
            orbitColor.put4f(BufferUtils.colorNull);
            orbitPosition.repeat3f();
            orbitColor.put4f(color);

            while (t < time) {
                t += ORBIT_DELTA_INTERPOLATE;
                if (t > time)
                    t = time;
                loadPosition.getInterpolatedArray(orbitPosition, t, start, end);
                orbitColor.put4f(color);
            }
            orbitPosition.repeat3f();
            orbitColor.put4f(BufferUtils.colorNull);
        }

        if (orbitPosition.length() >= 2 * 3) {
            orbits.setData(gl, orbitPosition.toBuffer(), orbitColor.toBuffer());
            orbits.render(gl, aspect, orbitThickness);
         }

        planetPosition.rewind();
        planetColor.rewind();
        planets.setData(gl, planetPosition, planetColor);
        planets.renderPoints(gl, GLInfo.pixelScale[0]);
    }

}
