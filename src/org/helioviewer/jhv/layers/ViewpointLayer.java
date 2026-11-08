package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportMath;
import org.helioviewer.jhv.input.InputController;
import org.helioviewer.jhv.input.InputMouseListener;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.Transform;

import org.json.JSONObject;

public class ViewpointLayer extends AbstractLayer {

    private static final double LINEWIDTH_ORBIT = 2 * GLSLLine.LINEWIDTH_BASIC;
    private static final double LINEWIDTH_SPIRAL = 2 * GLSLLine.LINEWIDTH_BASIC;
    private static final double SPIRAL_RADIUS = 3 * Sun.MeanEarthDistance;
    private static final int SPIRAL_DIVISIONS = 64;
    private static final int SPIRAL_ARMS = 9;

    private final GLSLLine orbits = new GLSLLine(true);
    private final GLSLShape planets = new GLSLShape(true);
    private final ViewpointOrbitWorker orbitWorker = new ViewpointOrbitWorker(this::orbitsReady);
    private ViewpointOrbitWorker.Parameters uploadedParameters;
    private ViewpointOrbitWorker.Prepared readyOrbits;

    private final GLSLLine spiral = new GLSLLine(true);
    private final BufVertex spiralBuf = new BufVertex(SPIRAL_ARMS * (2 * SPIRAL_DIVISIONS + 1 + 2));
    private final byte[] spiralColor = Colors.ReducedGreen.bytes();

    private final double[] lati = new double[3];
    private final double[] hoverPoint = new double[3];
    private final PositionResponse.Interpolated latiInterpolated = new PositionResponse.Interpolated();
    private final PositionResponse.Interpolated hoverInterpolated = new PositionResponse.Interpolated();

    private final ViewpointLayerOptions options;
    private final HoverListener hoverListener = new HoverListener();

    private final class HoverListener implements InputMouseListener {
        @Override
        public void mouseMoved(PointerEvent e) {
            handleMouseMoved(e);
        }

        @Override
        public void mouseExited(PointerEvent e) {
            handleMouseExited();
        }
    }

    public ViewpointLayer(JSONObject jo) {
        options = new ViewpointLayerOptions(jo);
    }

    private double getRelativeLongitude(ViewpointLayerOptionsExpert equatorial, long time, long start, long end) {
        double relativeLon = 0;

        PositionLoad control = equatorial.getHighlightedLoad();
        if (control != null && equatorial.isRelative()) {
            PositionResponse response = control.getResponse();
            if (response != null) {
                response.interpolateLatitudinal(time, start, end, lati, latiInterpolated);
                relativeLon = lati[1];
            }
        }
        return relativeLon;
    }

    @Override
    public void render(MapView mv, Viewport vp) {
        if (vp.idx == 0 && options.isLocation())
            options.sky.compute(mv, vp, enabled);

        if (!enabled)
            return;
        if (options.isLocation()) {
            options.sky.render(mv);
            return;
        }
        if (!options.isHeliospheric())
            return;

        long time = Player.getTime().milli;
        long start = Player.getStartTime();
        long end = Player.getEndTime();

        ViewpointLayerOptionsExpert equatorial = options.getEquatorialOptions();
        PositionLoad control = equatorial.getHighlightedLoad();
        double relativeLon = 0;
        int spiralSpeed = 0;

        if (control != null) {
            PositionResponse response = control.getResponse();
            if (response != null) {
                response.interpolateLatitudinal(time, start, end, lati, latiInterpolated);
                spiralSpeed = equatorial.getSpiralSpeed(); // only if we have control point
                relativeLon = equatorial.isRelative() ? lati[1] : 0;
            }
        }

        double pointFactor = temperedPointFactor(vp, mv.cameraWidth(vp));
        Position viewpoint = mv.viewpoint();

        Transform.pushView();
        Transform.rotateViewInverse(Quat.createXY(viewpoint.lat, viewpoint.lon + relativeLon));

        if (spiralSpeed > 0)
            renderSpiral(vp, lati, spiralSpeed);

        List<PositionLoad> positionLoads = equatorial.getSelectedLoads();
        if (!positionLoads.isEmpty()) {
            GL.glDisable(GL.DEPTH_TEST);
            renderPlanets(vp, positionLoads, pointFactor, time, start, end);
            GL.glEnable(GL.DEPTH_TEST);
        }

        Transform.popView();
    }

    @Override
    public void renderScale(MapView mv, Viewport vp) {
        if (!mv.isHpc() || !options.isLocation())
            return;
        options.sky.compute(mv, vp, enabled);
        if (enabled)
            options.sky.render(mv);
    }

    private static final int MOUSE_OFFSET_X = 25;
    private static final int MOUSE_OFFSET_Y = 25;
    private final List<String> hoverText = new ArrayList<>();
    private int mouseX, mouseY;

    private static double temperedPointFactor(Viewport vp, double width) {
        double pixelScale = Display.pixelScale[1];
        return pixelScale * Math.cbrt(ViewportMath.getPixelFactor(vp, width) / pixelScale);
    }

    @Override
    public void renderFullFloat(Viewport vp) {
        if (!enabled)
            return;
        GLText.drawTextFloat(vp, hoverText, mouseX + MOUSE_OFFSET_X, mouseY + MOUSE_OFFSET_Y);
    }

    private void clearHoverTextIfNeeded() {
        if (!hoverText.isEmpty()) {
            hoverText.clear();
            DisplayController.display();
        }
    }

    private void handleMouseMoved(PointerEvent e) {
        if (options.isLocation()) {
            hoverLocation(e);
            return;
        }
        if (!options.isHeliospheric()) {
            clearHoverTextIfNeeded();
            return;
        }

        ViewpointLayerOptionsExpert equatorial = options.getEquatorialOptions();
        List<PositionLoad> positionLoads = equatorial.getSelectedLoads();
        if (positionLoads.isEmpty()) {
            clearHoverTextIfNeeded();
            return;
        }

        MapView mv = GLRenderer.getMapView();
        long time = Player.getTime().milli, start = Player.getStartTime(), end = Player.getEndTime();
        double relativeLon = getRelativeLongitude(equatorial, time, start, end);

        mouseX = e.x();
        mouseY = e.y();

        Viewport vp = Display.getActiveViewport();
        double width = mv.cameraWidth(vp);
        double mousePlaneX = ViewportMath.computeUpX(vp, width, mv.cameraTranslationX(), mouseX);
        double mousePlaneY = ViewportMath.computeUpY(vp, width, mv.cameraTranslationY(), mouseY);
        Quat dragRotation = mv.dragRotation();

        double halfWidth = width / 2;
        double hoverThreshold2 = (0.01 * halfWidth) * (0.01 * halfWidth);
        double cosRelativeLon = Math.cos(-relativeLon);
        double sinRelativeLon = Math.sin(-relativeLon);
        double minDist2 = Double.MAX_VALUE;

        String name = null;
        for (PositionLoad positionLoad : positionLoads) {
            PositionResponse response = positionLoad.getResponse();
            if (response == null)
                continue;

            response.interpolateRectangular(time, start, end, hoverPoint, hoverInterpolated);
            if (relativeLon != 0) {
                double x = hoverPoint[0];
                double y = hoverPoint[1];
                hoverPoint[0] = x * cosRelativeLon - y * sinRelativeLon;
                hoverPoint[1] = x * sinRelativeLon + y * cosRelativeLon;
            }
            dragRotation.qxv(hoverPoint, hoverPoint);

            double deltaX = hoverPoint[0] - mousePlaneX;
            double deltaY = hoverPoint[1] - mousePlaneY;
            double dist2 = deltaX * deltaX + deltaY * deltaY;
            if (dist2 < minDist2) {
                minDist2 = dist2;
                name = positionLoad.target().toString();
            }
        }
        clearHoverTextIfNeeded();
        if (name != null && minDist2 < hoverThreshold2) {
            hoverText.add(name);
            DisplayController.display();
        }
    }

    private void hoverLocation(PointerEvent e) {
        mouseX = e.x();
        mouseY = e.y();
        String name = enabled ? options.sky.hover(GLRenderer.getMapView(), Display.getActiveViewport(), mouseX, mouseY) : null;
        clearHoverTextIfNeeded();
        if (name != null) {
            hoverText.add(name);
            DisplayController.display();
        }
    }

    private void handleMouseExited() {
        clearHoverTextIfNeeded();
    }

    @Override
    public void setEnabled(boolean _enabled) {
        setEnabled(_enabled, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
    }

    private void setEnabled(boolean _enabled, DisplayController.ViewpointApplyMode mode) {
        boolean wasEnabled = enabled;
        super.setEnabled(_enabled && options.getCameraMode() != ViewpointLayerOptions.CameraMode.Observer);

        if (enabled) {
            InputController.addListener(hoverListener);
            options.activate();
            options.applyCurrentViewpoint(mode);
        } else {
            hoverText.clear();
            clearOrbitWorker();
            InputController.removeListener(hoverListener);
            options.deactivate();
            if (wasEnabled && Layers.getViewpointLayer() == this)
                DisplayController.setViewpointUpdate(UpdateViewpoint.observer, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
        }
    }

    public ViewpointLayerOptions.CameraMode getCameraMode() {
        return enabled ? options.getCameraMode() : ViewpointLayerOptions.CameraMode.Observer;
    }

    public void setCameraMode(ViewpointLayerOptions.CameraMode mode) {
        if (getCameraMode() == mode)
            return;
        if (mode == ViewpointLayerOptions.CameraMode.Observer) {
            setEnabled(false);
            options.setCameraMode(mode, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
        } else {
            options.setCameraMode(mode, DisplayController.ViewpointApplyMode.RESET);
            if (!enabled)
                setEnabled(true, DisplayController.ViewpointApplyMode.RESET);
        }
        Layers.fireLayerUpdated(this);
        DisplayController.render();
    }

    void applyCurrentViewpoint() {
        if (enabled)
            options.applyCurrentViewpoint(DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
        else
            DisplayController.setViewpointUpdate(UpdateViewpoint.observer, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
    }

    @Override
    public String getName() {
        return "Viewpoint";
    }

    @Override
    public void init() {
        orbits.init();
        planets.init();
        options.sky.init();
        spiral.init();
    }

    @Override
    public void dispose() {
        clearOrbitWorker();
        orbits.dispose();
        planets.dispose();
        options.sky.dispose();
        spiral.dispose();
    }

    @Override
    public void serialize(JSONObject jo) {
        options.serialize(jo);
    }

    public ViewpointLayerOptions getOptions() {
        return options;
    }

    private void renderPlanets(Viewport vp, List<PositionLoad> positionLoads, double pointFactor, long time, long start, long end) {
        ViewpointOrbitWorker.Parameters parameters = createOrbitParameters(positionLoads, time, start, end);
        if (parameters.entries().isEmpty()) {
            clearOrbitWorker();
            return;
        }

        uploadReadyOrbits(parameters);

        if (!parameters.equals(uploadedParameters))
            orbitWorker.submit(parameters);

        if (parameters.compatibleWith(uploadedParameters)) {
            orbits.renderLine(vp, LINEWIDTH_ORBIT);
            planets.renderPoints(pointFactor);
        }
    }

    private static ViewpointOrbitWorker.Parameters createOrbitParameters(List<PositionLoad> positionLoads, long time, long start, long end) {
        ArrayList<ViewpointOrbitWorker.Entry> entries = new ArrayList<>(positionLoads.size());
        for (PositionLoad positionLoad : positionLoads) {
            PositionResponse response = positionLoad.getResponse();
            if (response != null)
                entries.add(new ViewpointOrbitWorker.Entry(positionLoad, response, positionLoad.target().getTrajectoryColor(Display.whiteBackground)));
        }
        return new ViewpointOrbitWorker.Parameters(entries, time, start, end);
    }

    private void uploadReadyOrbits(ViewpointOrbitWorker.Parameters parameters) {
        if (readyOrbits == null)
            return;

        if (parameters.compatibleWith(readyOrbits.parameters())) {
            orbits.upload(readyOrbits.orbitVertices());
            planets.upload(readyOrbits.planetVertices());
            uploadedParameters = readyOrbits.parameters();
        }
        readyOrbits = null;
    }

    private void orbitsReady(ViewpointOrbitWorker.Prepared prepared) {
        readyOrbits = prepared;
        DisplayController.display();
    }

    private void clearOrbitWorker() {
        readyOrbits = null;
        uploadedParameters = null;
        orbitWorker.cancel();
    }

    private void spiralPutVertex(double rad, double lon, double lat, byte[] color, boolean startLine) {
        float x = (float) (rad * Math.cos(lat) * Math.cos(lon));
        float y = (float) (rad * Math.cos(lat) * Math.sin(lon));
        float z = (float) (rad * Math.sin(lat));
        if (startLine)
            spiralBuf.startLine(x, y, z, 1, color);
        else
            spiralBuf.putVertex(x, y, z, 1, color);
    }

    private void renderSpiral(Viewport vp, double[] spiralLati, int speed) {
        double rad0 = spiralLati[0];
        double lon0 = spiralLati[1];
        double lat0 = spiralLati[2];

        double sr = speed * (Sun.RadiusKMeterInv / Sun.RotationRate);
        for (int j = 0; j < SPIRAL_ARMS; j++) {
            double lona = lon0 + j * (2 * Math.PI / SPIRAL_ARMS); // arm longitude
            // before control point
            for (int i = 0; i < SPIRAL_DIVISIONS; i++) {
                double rad = (Sun.Radius + (rad0 - Sun.Radius) * i / (double) SPIRAL_DIVISIONS);
                if (rad > SPIRAL_RADIUS)
                    break;
                double lon = lona - (rad - rad0) / sr;
                spiralPutVertex(rad, lon, lat0, spiralColor, i == 0);
            }
            // after control point
            for (int i = 0; i <= SPIRAL_DIVISIONS; i++) {
                double rad = (rad0 + (SPIRAL_RADIUS - rad0) * i / (double) SPIRAL_DIVISIONS);
                if (rad > SPIRAL_RADIUS)
                    break;
                double lon = lona - (rad - rad0) / sr;
                spiralPutVertex(rad, lon, lat0, spiralColor, false);
            }
            spiralBuf.endLine();
        }

        spiral.uploadAndClear(spiralBuf);
        spiral.renderLine(vp, LINEWIDTH_SPIRAL);
    }
}
