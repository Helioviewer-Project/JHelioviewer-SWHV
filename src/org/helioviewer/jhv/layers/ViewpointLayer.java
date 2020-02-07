package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.CameraListener;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.time.JHVTime;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class ViewpointLayer extends AbstractLayer implements CameraListener, MouseListener, MouseMotionListener {

    private static final double DELTA_ORBIT = 10 * 60 * 1000 * Sun.MeanEarthDistanceInv;
    private static final double DELTA_CUTOFF = 3 * Sun.MeanEarthDistance;
    private static final double LINEWIDTH_ORBIT = 2 * GLSLLine.LINEWIDTH_BASIC;
    private static final double LINEWIDTH_SPIRAL = 2 * GLSLLine.LINEWIDTH_BASIC;
    private static final float SIZE_PLANET = 5;

    private static final double RAD_PER_SEC = (2 * Math.PI) / (Carrington.CR_SIDEREAL * 86400);
    private static final double SPIRAL_RADIUS = 3 * Sun.MeanEarthDistance;
    private static final int SPIRAL_DIVISIONS = 64;
    private static final int SPIRAL_ARMS = 9;

    private final GLSLLine orbits = new GLSLLine(true);
    private final BufVertex orbitBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64k
    private final GLSLShape planets = new GLSLShape(true);
    private final BufVertex planetBuf = new BufVertex(8 * GLSLShape.stride);

    private final GLSLLine spiral = new GLSLLine(true);
    private final BufVertex spiralBuf = new BufVertex(SPIRAL_ARMS * (2 * SPIRAL_DIVISIONS + 1 + 2) * GLSLLine.stride);
    private final byte[] spiralColor = Colors.ReducedGreen;

    private final ViewpointLayerOptions optionsPanel;

    private JHVTime viewpointTime = Sun.StartEarth.time;

    public ViewpointLayer(JSONObject jo) {
        optionsPanel = new ViewpointLayerOptions(jo);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;
        if (!optionsPanel.isHeliospheric())
            return;

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        Position viewpoint = camera.getViewpoint();

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());
        boolean far = Camera.useWideProjection(viewpoint.distance);
        if (far) {
            Transform.pushProjection();
            camera.projectionOrthoWide(vp.aspect);
        }

        renderSpiral(gl, vp, optionsPanel.getHighlightedLoad(), optionsPanel.getSpiralSpeed());

        List<PositionLoad> positionLoads = PositionLoad.get(camera.getUpdateViewpoint());
        if (!positionLoads.isEmpty()) {
            gl.glDisable(GL2.GL_DEPTH_TEST);
            renderPlanets(gl, vp, positionLoads, pixFactor);
            gl.glEnable(GL2.GL_DEPTH_TEST);
        }

        if (far) {
            Transform.popProjection();
        }
        Transform.popView();
    }

    private static final int MOUSE_OFFSET_X = 25;
    private static final int MOUSE_OFFSET_Y = 25;
    private final ArrayList<String> text = new ArrayList<>();
    private int mouseX, mouseY;

    @Override
    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        GLText.drawTextFloat(vp, text, mouseX + MOUSE_OFFSET_X, mouseY + MOUSE_OFFSET_Y);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (!optionsPanel.isHeliospheric())
            return;

        Camera camera = Display.getCamera();
        List<PositionLoad> positionLoads = PositionLoad.get(camera.getUpdateViewpoint());
        if (positionLoads.isEmpty())
            return;

        mouseX = e.getX();
        mouseY = e.getY();
        Vec3 v = CameraHelper.getVectorFromPlane(camera, Display.getActiveViewport(), mouseX, mouseY, Quat.ZERO, true);
        if (v == null)
            return;

        long time = Movie.getTime().milli, start = Movie.getStartTime(), end = Movie.getEndTime();

        double[] lat = new double[3];
        double width = camera.getCameraWidth() / 2, minDist = 5; // TBD
        String name = null;
        for (PositionLoad positionLoad : positionLoads) {
            PositionResponse response = positionLoad.getResponse();
            if (response == null)
                continue;

            response.interpolateLatitudinal(time, start, end, lat);
            double deltaX = Math.abs(lat[0] * Math.cos(lat[1]) - v.x);
            double deltaY = Math.abs(lat[0] * Math.sin(lat[1]) - v.y);
            double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / width;
            if (dist < minDist) {
                minDist = dist;
                name = positionLoad.getTarget().toString();
            }
        }
        if (!text.isEmpty()) {
            text.clear();
            MovieDisplay.display();
        }
        if (minDist < 0.01) {
            text.add(name);
            MovieDisplay.display();
        }
    }

    private Vec3 customControl = null;

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!optionsPanel.isHeliospheric())
            return;

        if (e.isControlDown()) {
            Vec3 v = null;
            if (!e.isShiftDown()) { // ctrl-shift-click to reset control point
                v = CameraHelper.getVectorFromPlane(Display.getCamera(), Display.getActiveViewport(), e.getX(), e.getY(), Quat.ZERO, true);
                if (v != null) {
                    double lon = 0, lat = 0;
                    double rad = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
                    if (rad > 0) {
                        lon = Math.atan2(v.y, v.x);
                        // lat = Math.asin(v.z / rad); unneeded
                    }
                    v.x = MathUtils.clip(rad, 0, SPIRAL_RADIUS);
                    v.y = lon;
                    v.z = lat;
                }
            }
            customControl = v;
            MovieDisplay.display();
        }
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
    public void setEnabled(boolean _enabled) {
        super.setEnabled(_enabled);

        if (enabled) {
            JHVFrame.getInputController().addPlugin(this);
            optionsPanel.activate();
            optionsPanel.syncViewpoint();
        } else {
            JHVFrame.getInputController().removePlugin(this);
            optionsPanel.deactivate();
            Display.getCamera().setViewpointUpdate(UpdateViewpoint.observer);
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
        JHVFrame.getInputController().removePlugin(this);
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
        return viewpointTime.toString();
    }

    @Override
    public void timeChanged(JHVTime _viewpointTime) {
        viewpointTime = _viewpointTime;
        JHVFrame.getCarringtonStatusPanel().update(viewpointTime);
        JHVFrame.getLayers().fireTimeUpdated(this);
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
        orbits.init(gl);
        planets.init(gl);
        spiral.init(gl);
        Display.getCamera().addListener(this);
    }

    @Override
    public void dispose(GL2 gl) {
        orbits.dispose(gl);
        planets.dispose(gl);
        spiral.dispose(gl);
        Display.getCamera().removeListener(this);
    }

    @Override
    public void serialize(JSONObject jo) {
        optionsPanel.serialize(jo);
    }

    private static long getStep(double dist) { // decrease interpolation step proportionally with distance, stop at 3au
        return (long) (DELTA_ORBIT * Math.min(dist, DELTA_CUTOFF));
    }

    private final float[] xyzw = {0, 0, 0, 1};

    private void renderPlanets(GL2 gl, Viewport vp, List<PositionLoad> positionLoads, double pointFactor) {
        long time = Movie.getTime().milli, start = Movie.getStartTime(), end = Movie.getEndTime();
        for (PositionLoad positionLoad : positionLoads) {
            PositionResponse response = positionLoad.getResponse();
            if (response == null)
                continue;

            byte[] color = positionLoad.getTarget().getColor();
            long t = start;

            double dist = response.interpolateRectangular(t, start, end, xyzw);
            orbitBuf.putVertex(xyzw[0], xyzw[1], xyzw[2], xyzw[3], Colors.Null);
            orbitBuf.repeatVertex(color);

            long delta = getStep(dist);
            while (t < time) {
                t += delta;
                if (t > time)
                    t = time;
                dist = response.interpolateRectangular(t, start, end, xyzw);
                orbitBuf.putVertex(xyzw[0], xyzw[1], xyzw[2], xyzw[3], color);
                delta = getStep(dist);
            }
            orbitBuf.repeatVertex(Colors.Null);
            planetBuf.putVertex(xyzw[0], xyzw[1], xyzw[2], SIZE_PLANET, color);
        }

        orbits.setData(gl, orbitBuf);
        orbits.render(gl, vp.aspect, LINEWIDTH_ORBIT);

        planets.setData(gl, planetBuf);
        planets.renderPoints(gl, pointFactor);
    }

    private void spiralPutVertex(double rad, double lon, double lat, byte[] color) {
        float x = (float) (rad * Math.cos(lat) * Math.cos(lon));
        float y = (float) (rad * Math.cos(lat) * Math.sin(lon));
        float z = (float) (rad * Math.sin(lat));
        spiralBuf.putVertex(x, y, z, 1, color);
    }

    private void renderSpiral(GL2 gl, Viewport vp, PositionLoad control, int speed) {
        if (speed == 0)
            return;

        double sr = speed * (Sun.RadiusKMeterInv / RAD_PER_SEC);
        // control point
        double rad0, lon0, lat0;

        if (customControl == null) {
            if (control == null)
                return;
            PositionResponse response = control.getResponse();
            if (response == null)
                return;

            double[] lat = new double[3];
            long time = Movie.getTime().milli, start = Movie.getStartTime(), end = Movie.getEndTime();
            response.interpolateLatitudinal(time, start, end, lat);

            rad0 = lat[0];
            lon0 = lat[1];
            lat0 = lat[2];
        } else {
            rad0 = customControl.x;
            lon0 = customControl.y;
            lat0 = 0;
        }

        for (int j = 0; j < SPIRAL_ARMS; j++) {
            double lona = lon0 + j * (2 * Math.PI / SPIRAL_ARMS); // arm longitude
            // before control point
            for (int i = 0; i < SPIRAL_DIVISIONS; i++) {
                double rad = (Sun.Radius + (rad0 - Sun.Radius) * i / (double) SPIRAL_DIVISIONS);
                if (rad > SPIRAL_RADIUS)
                    break;
                double lon = lona - (rad - rad0) / sr;
                if (i == 0) {
                    spiralPutVertex(rad, lon, lat0, Colors.Null);
                    spiralBuf.repeatVertex(spiralColor);
                } else {
                    spiralPutVertex(rad, lon, lat0, spiralColor);
                }
            }
            // after control point
            for (int i = 0; i <= SPIRAL_DIVISIONS; i++) {
                double rad = (rad0 + (SPIRAL_RADIUS - rad0) * i / (double) SPIRAL_DIVISIONS);
                if (rad > SPIRAL_RADIUS)
                    break;
                double lon = lona - (rad - rad0) / sr;
                spiralPutVertex(rad, lon, lat0, spiralColor);
            }
            spiralBuf.repeatVertex(Colors.Null);
        }

        spiral.setData(gl, spiralBuf);
        spiral.render(gl, vp.aspect, LINEWIDTH_SPIRAL);
    }

}
