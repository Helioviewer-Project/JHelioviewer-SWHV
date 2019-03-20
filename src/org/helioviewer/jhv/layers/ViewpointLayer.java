package org.helioviewer.jhv.layers;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Carrington;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLSLLine;
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
    private static final double LINEWIDTH_FOV = GLSLLine.LINEWIDTH_BASIC;
    private static final double LINEWIDTH_ORBIT = 2 * GLSLLine.LINEWIDTH_BASIC;
    private static final double LINEWIDTH_SPIRAL = 2 * GLSLLine.LINEWIDTH_BASIC;
    private static final float SIZE_PLANET = 5;

    private static final double RAD_PER_SEC = (2 * Math.PI) / (Carrington.CR_SIDEREAL * 86400);
    private static final double SPIRAL_RADIUS = 3 * Sun.MeanEarthDistance;
    private static final int SPIRAL_DIVISIONS = 64;
    private static final int SPIRAL_ARMS = 9;

    private final FOVShape fov = new FOVShape();
    private final byte[] fovColor = Colors.Blue;
    private final GLSLLine fovLine = new GLSLLine(true);
    private final BufVertex fovBuf = new BufVertex((4 * (FOVShape.SUBDIVISIONS + 1) + 2) * GLSLLine.stride);
    private final GLSLShape center = new GLSLShape(true);
    private final BufVertex centerBuf = new BufVertex(GLSLShape.stride);

    private final GLSLLine orbits = new GLSLLine(true);
    private final BufVertex orbitBuf = new BufVertex(3276 * GLSLLine.stride); // pre-allocate 64k
    private final GLSLShape planets = new GLSLShape(true);
    private final BufVertex planetBuf = new BufVertex(8 * GLSLShape.stride);

    private final GLSLLine spiral = new GLSLLine(true);
    private final BufVertex spiralBuf = new BufVertex(SPIRAL_ARMS * (2 * SPIRAL_DIVISIONS + 1 + 2) * GLSLLine.stride);
    private final byte[] spiralColor = Colors.Green;

    private final ViewpointLayerOptions optionsPanel;

    private String timeString = null;

    public ViewpointLayer(JSONObject jo) {
        optionsPanel = new ViewpointLayerOptions(jo);
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        Position viewpoint = camera.getViewpoint();
        double halfSide = 0.5 * viewpoint.distance * Math.tan(optionsPanel.getFOVAngle());

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());
        boolean far = Camera.useWideProjection(viewpoint.distance);
        if (far) {
            Transform.pushProjection();
            camera.projectionOrthoWide(vp.aspect);
        }

        renderFOV(gl, vp, halfSide, pixFactor);
        renderSpiral(gl, vp, viewpoint, optionsPanel.getSpiralSpeed());

        Collection<LoadPosition> loadPositions = camera.getUpdateViewpoint().getLoadPositions();
        if (!loadPositions.isEmpty()) {
            gl.glDisable(GL2.GL_DEPTH_TEST);
            renderPlanets(gl, vp, loadPositions, pixFactor);
            gl.glEnable(GL2.GL_DEPTH_TEST);
        }

        if (far) {
            Transform.popProjection();
        }
        Transform.popView();
    }

    private void renderFOV(GL2 gl, Viewport vp, double halfSide, double pointFactor) {
        fov.putCenter(centerBuf, fovColor);
        center.setData(gl, centerBuf);
        center.renderPoints(gl, pointFactor);

        fov.putLine(halfSide, halfSide, fovBuf, fovColor);
        fovLine.setData(gl, fovBuf);
        fovLine.render(gl, vp.aspect, LINEWIDTH_FOV);
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

            double width = camera.getCameraWidth() / 2, minDist = 5; // TBD
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
                MovieDisplay.display();
            }
            if (minDist < 0.01) {
                text.add(name);
                MovieDisplay.display();
            }
        }
    }

    private Vec3 spiralControl = null;

    @Override
    public void mouseClicked(MouseEvent e) {
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
            spiralControl = v;
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
    public void mouseWheelMoved(MouseEvent e) {
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
        return timeString;
    }

    public void fireTimeUpdated(JHVDate time) {
        timeString = time.toString();
        JHVFrame.getCarringtonStatusPanel().update(time);
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
        fovLine.init(gl);
        center.init(gl);
        orbits.init(gl);
        planets.init(gl);
        spiral.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        fovLine.dispose(gl);
        center.dispose(gl);
        orbits.dispose(gl);
        planets.dispose(gl);
        spiral.dispose(gl);
    }

    @Override
    public void serialize(JSONObject jo) {
        optionsPanel.serialize(jo);
    }

    private static long getStep(double dist) { // decrease interpolation step proportionally with distance, stop at 3au
        return (long) (DELTA_ORBIT * (dist > DELTA_CUTOFF ? DELTA_CUTOFF : dist));
    }

    private final float[] xyzw = {0, 0, 0, 1};

    private void renderPlanets(GL2 gl, Viewport vp, Collection<LoadPosition> loadPositions, double pointFactor) {
        long time = Movie.getTime().milli, start = Movie.getStartTime(), end = Movie.getEndTime();
        for (LoadPosition loadPosition : loadPositions) {
            PositionResponse response = loadPosition.getResponse();
            if (response == null)
                continue;

            byte[] color = loadPosition.getTarget().getColor();
            long t = start;

            double dist = response.getInterpolated(xyzw, t, start, end);
            orbitBuf.putVertex(xyzw[0], xyzw[1], xyzw[2], xyzw[3], Colors.Null);
            orbitBuf.repeatVertex(color);

            long delta = getStep(dist);
            while (t < time) {
                t += delta;
                if (t > time)
                    t = time;
                dist = response.getInterpolated(xyzw, t, start, end);
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

    private void renderSpiral(GL2 gl, Viewport vp, Position viewpoint, int speed) {
        if (speed == 0)
            return;

        double sr = speed * Sun.RadiusKMeterInv / RAD_PER_SEC;
        // control point
        double rad0, lon0, lat0;

        if (spiralControl == null) {
            Position p0 = Sun.getEarth(viewpoint.time);
            rad0 = p0.distance;
            lon0 = 0;
            lat0 = 0;
        } else {
            rad0 = spiralControl.x;
            lon0 = spiralControl.y;
            lat0 = 0;
        }

        for (int j = 0; j < SPIRAL_ARMS; j++) {
            double lona = lon0 + j * (2 * Math.PI / SPIRAL_ARMS); // arm longitude
            // before control point
            for (int i = 0; i < SPIRAL_DIVISIONS; i++) {
                double rad = (Sun.Radius + (rad0 - Sun.Radius) * i / (double) SPIRAL_DIVISIONS);
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
                double lon = lona - (rad - rad0) / sr;
                if (i == SPIRAL_DIVISIONS) {
                    spiralPutVertex(rad, lon, lat0, spiralColor);
                    spiralBuf.repeatVertex(Colors.Null);
                } else {
                    spiralPutVertex(rad, lon, lat0, spiralColor);
                }
            }
        }

        spiral.setData(gl, spiralBuf);
        spiral.render(gl, vp.aspect, LINEWIDTH_SPIRAL);
    }

}
