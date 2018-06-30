package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.FloatArray;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLSLPolyline;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.position.LoadPosition;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.position.PositionResponse;
import org.helioviewer.jhv.time.JHVDate;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

public class ViewpointLayer extends AbstractLayer implements MouseListener {

    private static final double DELTA_ORBIT = 10 * 60 * 1000 * Sun.MeanEarthDistanceInv;
    private static final double DELTA_CUTOFF = 3 * Sun.MeanEarthDistance;
    private static final double LINEWIDTH_FOV = 0.002;
    private static final double LINEWIDTH_ORBIT = 0.002;
    private static final float SIZE_PLANET = 10;

    private final FOVShape fov = new FOVShape(LINEWIDTH_FOV);
    private final GLSLPolyline orbits = new GLSLPolyline();
    private final GLSLShape planets = new GLSLShape();
    private final ViewpointLayerOptions optionsPanel;

    private String timeString = null;

    public ViewpointLayer(JSONObject jo) {
        optionsPanel = new ViewpointLayerOptions(jo);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        double tan = Math.tan(optionsPanel.getFOVAngle()) / 2;
        fov.setTAngles(tan, tan);
        double pixFactor = vp.height / (2 * camera.getWidth());
        Position viewpoint = camera.getViewpoint();

        boolean far = viewpoint.distance > 100 * Sun.MeanEarthDistance;

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());

        if (!far)
            fov.render(gl, vp, viewpoint.distance, pixFactor, false);

        Transform.pushProjection();
        camera.projectionOrthoFar(vp.aspect);

        if (far)
            fov.render(gl, vp, viewpoint.distance, pixFactor, false);
        {
            Collection<LoadPosition> loadPositions = camera.getUpdateViewpoint().getLoadPositions();
            if (!loadPositions.isEmpty()) {
                renderPlanets(gl, vp, loadPositions);
            }
        }
        Transform.popProjection();
        Transform.popView();
    }

    private static final int MOUSE_OFFSET_X = 25;
    private static final int MOUSE_OFFSET_Y = 25;
    private final ArrayList<String> text = new ArrayList<>();
    private int mouseX, mouseY;

    @Override
    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        GLText.drawText(vp, text, mouseX + MOUSE_OFFSET_X, mouseY + MOUSE_OFFSET_Y);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Camera camera = Display.getCamera();
        Collection<LoadPosition> loadPositions = camera.getUpdateViewpoint().getLoadPositions();
        if (!loadPositions.isEmpty()) {
            mouseX = e.getX();
            mouseY = e.getY();
            Vec3 v = CameraHelper.getVectorFromPlane(camera, Display.getActiveViewport(), mouseX, mouseY, Quat.ZERO, true);
            if (v == null)
                return;

            long time = Movie.getTime().milli, start = Movie.getStartTime(), end = Movie.getEndTime();

            double width = camera.getWidth(), minDist = 5;
            String name = null;
            for (LoadPosition loadPosition : loadPositions) {
                PositionResponse response = loadPosition.getResponse();
                if (response == null)
                    continue;

                Vec3 p = response.getInterpolatedHG(time, start, end);
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
            optionsPanel.activate();
            optionsPanel.syncViewpoint();
        } else {
            ImageViewerGui.getInputController().removePlugin(this);
            optionsPanel.deactivate();
            Display.getCamera().setViewpointUpdate(UpdateViewpoint.observer);
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

    private static long getStep(double dist) { // decrease interpolation step proportionally with distance, stop at 3au
        return (long) (DELTA_ORBIT * (dist > DELTA_CUTOFF ? DELTA_CUTOFF : dist));
    }

    private void renderPlanets(GL2 gl, Viewport vp, Collection<LoadPosition> loadPositions) {
        int size = loadPositions.size();
        FloatBuffer planetPosition = BufferUtils.newFloatBuffer(4 * size);
        FloatBuffer planetColor = BufferUtils.newFloatBuffer(4 * size);
        FloatArray orbitPosition = new FloatArray();
        FloatArray orbitColor = new FloatArray();

        float[] xyz = new float[3];
        float[] xyzw = new float[4];
        xyzw[3] = 1;

        long time = Movie.getTime().milli, start = Movie.getStartTime(), end = Movie.getEndTime();

        for (LoadPosition loadPosition : loadPositions) {
            PositionResponse response = loadPosition.getResponse();
            if (response == null)
                continue;

            float[] color = loadPosition.getTarget().getColor();
            long t = start;

            double dist = response.getInterpolated(xyzw, t, start, end);
            orbitPosition.put4f(xyzw);
            orbitColor.put4f(BufferUtils.colorNull);
            orbitPosition.repeat4f();
            orbitColor.put4f(color);

            long delta = getStep(dist);
            while (t < time) {
                t += delta;
                if (t > time)
                    t = time;
                dist = response.getInterpolated(xyzw, t, start, end);
                orbitPosition.put4f(xyzw);
                orbitColor.put4f(color);
                delta = getStep(dist);
            }
            orbitPosition.repeat4f();
            orbitColor.put4f(BufferUtils.colorNull);

            response.getInterpolated(xyz, time, start, end);
            planetPosition.put(xyz);
            planetPosition.put(SIZE_PLANET);
            planetColor.put(color);
        }

        if (orbitPosition.length() >= 2 * 3) {
            orbits.setData(gl, orbitPosition.toBuffer(), orbitColor.toBuffer());
            orbits.render(gl, vp, LINEWIDTH_ORBIT);
        }

        planetPosition.rewind();
        planetColor.rewind();
        planets.setData(gl, planetPosition, planetColor);
        planets.renderPoints(gl, 1);
    }

}
