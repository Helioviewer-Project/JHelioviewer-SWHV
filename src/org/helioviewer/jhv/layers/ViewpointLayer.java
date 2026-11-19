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
import org.helioviewer.jhv.layers.viewpoint.ViewpointLayerOptions;
import org.helioviewer.jhv.layers.viewpoint.ViewpointLayerOptionsExpert;
import org.helioviewer.jhv.layers.viewpoint.ViewpointObjectNames;
import org.helioviewer.jhv.layers.viewpoint.ViewpointOrbitGeometry;
import org.helioviewer.jhv.layers.viewpoint.ViewpointSky;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.Transform;

import org.json.JSONObject;

public class ViewpointLayer extends AbstractLayer {

    private static final double LINEWIDTH_ORBIT = 2 * GLSLLine.LINEWIDTH_BASIC;
    private static final double LINEWIDTH_SPIRAL = 2 * GLSLLine.LINEWIDTH_BASIC;
    private static final double SPIRAL_RADIUS = 3 * Sun.MeanEarthDistance;
    private static final int SPIRAL_DIVISIONS = 64;
    private static final int SPIRAL_ARMS = 9;
    private static final int SPIRAL_POINTS = 2 * SPIRAL_DIVISIONS + 1;
    private static final double[][] SPIRAL_ROTATIONS = spiralRotations();
    private static final byte[] SPIRAL_REFERENCE_COLOR = Colors.ReducedGreen.bytes();
    private static final byte[] SPIRAL_SECONDARY_COLOR = Colors.ReducedGreen.bytes(0.4);

    private final GLSLLine orbits = new GLSLLine(true);
    private final GLSLShape planets = new GLSLShape(true);
    private final ViewpointOrbitGeometry orbitGeometry = new ViewpointOrbitGeometry();
    private ViewpointOrbitGeometry.Parameters uploadedParameters;
    private List<ViewpointOrbitGeometry.Marker> uploadedMarkers = List.of();
    private final ViewpointObjectNames objectNames = new ViewpointObjectNames();

    private final GLSLLine spiral = new GLSLLine(true);
    private final BufVertex spiralBuf = new BufVertex(SPIRAL_ARMS * (SPIRAL_POINTS + 2));
    private final double[][] spiralArm = new double[SPIRAL_POINTS][3];

    private final double[] lati = new double[3];
    private final PositionResponse.Interpolated latiInterpolated = new PositionResponse.Interpolated();

    private final ViewpointLayerOptions options;

    public ViewpointLayer(JSONObject jo) {
        options = new ViewpointLayerOptions(jo);
    }

    @Override
    public void render(MapView mv, Viewport vp) {
        objectNames.clear();
        if (!enabled)
            return;
        if (options.isLocation()) {
            ViewpointSky sky = options.getSky();
            if (vp.idx == 0)
                sky.compute(mv, vp);
            sky.render(mv);
            return;
        }
        if (!options.isHeliospheric())
            return;

        long time = Player.getTime().milli;
        long start = Player.getStartTime();
        long end = Player.getEndTime();

        ViewpointLayerOptionsExpert equatorial = options.getEquatorialOptions();
        List<PositionLoad> positionLoads = equatorial.getSelectedLoads();
        boolean hasOrbits = !positionLoads.isEmpty() && prepareOrbits(positionLoads, time, start, end);
        boolean relative = equatorial.isRelative();
        PositionLoad reference = null;
        double relativeLon = 0;
        int spiralSpeed = equatorial.getSpiralSpeed();

        if (relative || spiralSpeed > 0) {
            PositionLoad control = equatorial.getHighlightedLoad();
            PositionResponse response = control == null ? null : control.getResponse();
            if (response != null) {
                response.interpolateLatitudinal(time, start, end, lati, latiInterpolated);
                relativeLon = relative ? lati[1] : 0;
                reference = control;
            } else {
                spiralSpeed = 0;
            }
        }

        double pointFactor = temperedPointFactor(mv, vp);
        Position viewpoint = mv.viewpoint();
        Quat orbitFrameRotation = relativeLon == 0 ? viewpoint.toQuat() : Quat.createXY(viewpoint.lat, viewpoint.lon + relativeLon);

        Transform.pushView();
        Transform.rotateViewInverse(orbitFrameRotation);

        if (spiralSpeed > 0)
            renderSpiral(vp, lati, spiralSpeed);

        if (hasOrbits) {
            GL.glDisable(GL.DEPTH_TEST);
            orbits.renderLine(vp, LINEWIDTH_ORBIT);
            planets.renderPoints(pointFactor);
            objectNames.prepare(uploadedMarkers, reference, mv, vp, orbitFrameRotation, pointFactor);
            GL.glEnable(GL.DEPTH_TEST);
        }

        Transform.popView();
    }

    @Override
    public void renderScale(MapView mv, Viewport vp) {
        objectNames.clear();
        if (!enabled || !mv.isHpc() || !options.isLocation())
            return;
        ViewpointSky sky = options.getSky();
        sky.compute(mv, vp);
        sky.render(mv);
    }

    private static double temperedPointFactor(MapView mv, Viewport vp) {
        double markerScale = Math.max(1, vp.height / 1024f);
        return markerScale * Math.cbrt(ViewportMath.getPixelFactor(vp, mv.cameraWidth(vp)) / markerScale);
    }

    @Override
    public void renderFloat(MapView mv, Viewport vp) {
        if (enabled && options.isHeliospheric() && mv.isOrthographic())
            objectNames.render(vp, Display.getRenderPixelsPerPoint());
        else
            objectNames.clear();
    }

    @Override
    public void renderFullFloat(Viewport vp) {
        options.getSky().renderHover(vp);
    }

    @Override
    public void setEnabled(boolean _enabled) {
        setEnabled(_enabled, DisplayController.ViewpointApplyMode.KEEP_TRANSFORM);
    }

    private void setEnabled(boolean _enabled, DisplayController.ViewpointApplyMode mode) {
        boolean wasEnabled = enabled;
        super.setEnabled(_enabled && options.getCameraMode() != ViewpointLayerOptions.CameraMode.Observer);

        if (enabled) {
            options.activate();
            options.applyCurrentViewpoint(mode);
        } else {
            clearUploadedOrbits();
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
        objectNames.init();
        options.getSky().init();
        spiral.init();
    }

    @Override
    public void dispose() {
        clearUploadedOrbits();
        orbits.dispose();
        planets.dispose();
        objectNames.dispose();
        options.getSky().dispose();
        spiral.dispose();
    }

    @Override
    public void serialize(JSONObject jo) {
        options.serialize(jo);
    }

    public ViewpointLayerOptions getOptions() {
        return options;
    }

    private boolean prepareOrbits(List<PositionLoad> positionLoads, long time, long start, long end) {
        ViewpointOrbitGeometry.Parameters parameters = createOrbitParameters(positionLoads, time, start, end);
        if (parameters.entries().isEmpty()) {
            clearUploadedOrbits();
            return false;
        }

        if (!parameters.equals(uploadedParameters)) {
            ViewpointOrbitGeometry.Prepared geometry = orbitGeometry.build(parameters);
            orbits.upload(geometry.orbitVertices());
            planets.upload(geometry.planetVertices());
            uploadedParameters = parameters;
            uploadedMarkers = geometry.markers();
        }

        return true;
    }

    private static ViewpointOrbitGeometry.Parameters createOrbitParameters(List<PositionLoad> positionLoads, long time, long start, long end) {
        ArrayList<ViewpointOrbitGeometry.Entry> entries = new ArrayList<>(positionLoads.size());
        for (PositionLoad positionLoad : positionLoads) {
            PositionResponse response = positionLoad.getResponse();
            if (response != null)
                entries.add(new ViewpointOrbitGeometry.Entry(positionLoad, response, positionLoad.target().getTrajectoryColor(Display.whiteBackground)));
        }
        return new ViewpointOrbitGeometry.Parameters(entries, time, start, end);
    }

    private void clearUploadedOrbits() {
        uploadedParameters = null;
        uploadedMarkers = List.of();
        objectNames.clear();
    }

    private static double[][] spiralRotations() {
        double[][] rotations = new double[SPIRAL_ARMS][2];
        for (int i = 0; i < rotations.length; i++) {
            double angle = i * (2 * Math.PI / SPIRAL_ARMS);
            rotations[i][0] = Math.cos(angle);
            rotations[i][1] = Math.sin(angle);
        }
        return rotations;
    }

    private int sampleSpiralArm(double[] spiralLati, int speed) {
        double rad0 = spiralLati[0];
        double lon0 = spiralLati[1];
        double cosLat = Math.cos(spiralLati[2]);
        double sinLat = Math.sin(spiralLati[2]);
        double sr = speed * (Sun.RadiusKMeterInv / Sun.RotationRate);

        int pointCount = 0;
        // Index zero is the reference point, with radial samples on either side.
        for (int i = -SPIRAL_DIVISIONS; i <= SPIRAL_DIVISIONS; i++) {
            double rad = i < 0
                    ? Sun.Radius + (rad0 - Sun.Radius) * (i + SPIRAL_DIVISIONS) / SPIRAL_DIVISIONS
                    : rad0 + (SPIRAL_RADIUS - rad0) * i / SPIRAL_DIVISIONS;
            if (rad > SPIRAL_RADIUS)
                break;
            double lon = lon0 - (rad - rad0) / sr;
            double[] point = spiralArm[pointCount++];
            point[0] = rad * cosLat * Math.cos(lon);
            point[1] = rad * cosLat * Math.sin(lon);
            point[2] = rad * sinLat;
        }
        return pointCount;
    }

    private void renderSpiral(Viewport vp, double[] spiralLati, int speed) {
        int pointCount = sampleSpiralArm(spiralLati, speed);
        for (int arm = 0; arm < SPIRAL_ROTATIONS.length; arm++) {
            double[] rotation = SPIRAL_ROTATIONS[arm];
            double cos = rotation[0];
            double sin = rotation[1];
            byte[] color = arm == 0 ? SPIRAL_REFERENCE_COLOR : SPIRAL_SECONDARY_COLOR;
            for (int i = 0; i < pointCount; i++) {
                double[] point = spiralArm[i];
                float x = (float) (point[0] * cos - point[1] * sin);
                float y = (float) (point[0] * sin + point[1] * cos);
                float z = (float) point[2];
                if (i == 0)
                    spiralBuf.startLine(x, y, z, 1, color);
                else
                    spiralBuf.putVertex(x, y, z, 1, color);
            }
            spiralBuf.endLine();
        }

        spiral.uploadAndClear(spiralBuf);
        spiral.renderLine(vp, LINEWIDTH_SPIRAL);
    }
}
