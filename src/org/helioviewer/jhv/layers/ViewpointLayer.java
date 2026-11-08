package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.Stars;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportMath;
import org.helioviewer.jhv.input.InputController;
import org.helioviewer.jhv.input.InputMouseListener;
import org.helioviewer.jhv.input.PointerEvent;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.opengl.GLSLEclipseShader;
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
    private static final float SIZE_LOCATION_PLANET = 6;
    private static final double STAR_OFFSET = 3e-3; // [rad] apparent minus catalogue star direction: proper motion 1990-2050, aberration at 192 km/s, deflection, parallax
    static final double[] SOLAR_POLE = solarPole(Math.toRadians(286.13), Math.toRadians(63.87)); // J2000, BODY10_POLE_RA/DEC in pck00011.tpc

    private final GLSLLine orbits = new GLSLLine(true);
    private final GLSLShape planets = new GLSLShape(true);
    private final ViewpointOrbitWorker orbitWorker = new ViewpointOrbitWorker(this::orbitsReady);
    private ViewpointOrbitWorker.Parameters uploadedParameters;
    private ViewpointOrbitWorker.Prepared readyOrbits;

    private record LocationPlanet(Vec3 direction, double distance, double sinRadiusSquared, float[] color, int pointIndex, int pointCount) {}

    private record LocationHover(double x, double y, Object object, int viewportIndex) {}

    private final GLSLShape locationPoints = new GLSLShape(true);
    private final BufVertex locationPointsBuf = new BufVertex();
    private int locationStarCount;
    private final List<LocationPlanet> locationPlanets = new ArrayList<>();
    private final List<LocationHover> locationHover = new ArrayList<>();
    private final double[] locationVector = new double[3];
    private final double[] locationPoint = new double[3];
    private final double[] locationSun = new double[3];
    private final double[] locationNorth = new double[3];
    private final double[] locationWest = new double[3];
    private final double[] locationSsb = new double[3];
    private final double[] locationVelocity = new double[3];
    private final double[] locationHelio = new double[3];
    private final PositionResponse.Interpolated locationInterpolated = new PositionResponse.Interpolated();

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

    private double getRelativeLongitude(long time, long start, long end) {
        double relativeLon = 0;

        PositionLoad control = options.getHighlightedLoad();
        if (control != null && options.isRelative()) {
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
            computeLocation(mv, vp);

        if (!enabled)
            return;
        if (options.isLocation()) {
            renderLocation(mv);
            return;
        }
        if (!options.isHeliospheric())
            return;

        long time = Player.getTime().milli;
        long start = Player.getStartTime();
        long end = Player.getEndTime();

        PositionLoad control = options.getHighlightedLoad();
        double relativeLon = 0;
        int spiralSpeed = 0;

        if (control != null) {
            PositionResponse response = control.getResponse();
            if (response != null) {
                response.interpolateLatitudinal(time, start, end, lati, latiInterpolated);
                spiralSpeed = options.getSpiralSpeed(); // only if we have control point
                relativeLon = options.isRelative() ? lati[1] : 0;
            }
        }

        double pointFactor = temperedPointFactor(vp, mv.cameraWidth(vp));
        Position viewpoint = mv.viewpoint();

        Transform.pushView();
        Transform.rotateViewInverse(Quat.createXY(viewpoint.lat, viewpoint.lon + relativeLon));

        if (spiralSpeed > 0)
            renderSpiral(vp, lati, spiralSpeed);

        List<PositionLoad> positionLoads = options.getVisibleLoads();
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
        computeLocation(mv, vp);
        if (enabled)
            renderLocation(mv);
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

        List<PositionLoad> positionLoads = options.getVisibleLoads();
        if (positionLoads.isEmpty()) {
            clearHoverTextIfNeeded();
            return;
        }

        MapView mv = GLRenderer.getMapView();
        long time = Player.getTime().milli, start = Player.getStartTime(), end = Player.getEndTime();
        double relativeLon = getRelativeLongitude(time, start, end);

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
        MapView mv = GLRenderer.getMapView();
        mouseX = e.x();
        mouseY = e.y();

        Viewport vp = Display.getActiveViewport();
        if (!enabled || !(mv.isOrthographic() || mv.isHpc())) {
            clearHoverTextIfNeeded();
            return;
        }
        double width = mv.cameraWidth(vp);
        double mousePlaneX = ViewportMath.computeUpX(vp, width, mv.cameraTranslationX(), mouseX);
        double mousePlaneY = ViewportMath.computeUpY(vp, width, mv.cameraTranslationY(), mouseY);

        double halfWidth = width / 2;
        double hoverThreshold2 = (0.01 * halfWidth) * (0.01 * halfWidth);
        double minDist2 = Double.MAX_VALUE;

        Object nearest = null;
        for (LocationHover point : locationHover) {
            if (mv.isHpc() && point.viewportIndex != vp.idx)
                continue;
            double deltaX = point.x - mousePlaneX;
            double deltaY = point.y - mousePlaneY;
            double dist2 = deltaX * deltaX + deltaY * deltaY;
            if (dist2 < minDist2) {
                minDist2 = dist2;
                nearest = point.object;
            }
        }
        clearHoverTextIfNeeded();
        if (nearest != null && minDist2 < hoverThreshold2) {
            hoverText.add(switch (nearest) {
                case PositionLoad load -> load.target().toString();
                case Stars.Star star -> star.hip() > 0 ? String.format("HIP%d,G%.2f", star.hip(), star.mag()) : String.format("G%.2f", star.mag());
                default -> nearest.toString();
            });
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
        locationPoints.init();
        spiral.init();
    }

    @Override
    public void dispose() {
        clearOrbitWorker();
        orbits.dispose();
        planets.dispose();
        locationPoints.dispose();
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

    // Orthographic points are shared; HPC points use each viewport's angular scale.
    private void computeLocation(MapView mv, Viewport vp) {
        // Preserve screen hover positions while rendering captures.
        if (mv == GLRenderer.getMapView() && vp.idx == 0)
            locationHover.clear();
        locationStarCount = 0;
        locationPlanets.clear();
        putLocationSky(mv, vp);
        locationPoints.upload(locationPointsBuf);
        locationPointsBuf.clear();
    }

    private void putLocationSky(MapView mv, Viewport vp) {
        if (!enabled)
            return;

        ViewpointLayerOptionsExpert location = options.getLocationOptions();
        PositionResponse sunResponse = location.getApparentSun();
        if (sunResponse == null)
            return;

        Position viewpoint = mv.viewpoint();
        long time = Player.getTime().milli, start = Player.getStartTime(), end = Player.getEndTime();
        double sunDistance = sunResponse.interpolateRectangular(time, start, end, locationSun, locationInterpolated);
        helioprojectiveAxes(locationSun, sunDistance, SOLAR_POLE, locationNorth, locationWest);

        PositionResponse state = location.getLocationState();
        PositionResponse heliocentric = location.getLocationHeliocentric();
        Stars.Catalog catalog = location.getStarCatalog();
        if (catalog != null && state != null && heliocentric != null) {
            state.interpolateState(time, start, end, locationSsb, locationVelocity, locationInterpolated);
            double years = Stars.julianYear(locationInterpolated.time) - catalog.epoch();
            for (int i = 0; i < 3; i++)
                locationSsb[i] *= Sun.MeanEarthDistanceInv; // [au]
            double helioDistance = heliocentric.interpolateRectangular(time, start, end, locationHelio, locationInterpolated);
            for (int i = 0; i < 3; i++)
                locationHelio[i] /= helioDistance;
            helioDistance *= Sun.MeanEarthDistanceInv; // [au]

            double cosCone;
            if (mv.isHpc()) {
                MapScale scale = mv.scale(vp);
                double halfWidth = mv.cameraWidth(vp) / 2;
                double cx = .5 - mv.cameraTranslationX() / vp.aspect;
                double cy = .5 - mv.cameraTranslationY();
                double lon = Math.max(Math.abs(scale.toMapX(Math.clamp(cx - halfWidth, 0, 1))),
                        Math.abs(scale.toMapX(Math.clamp(cx + halfWidth, 0, 1))));
                double lat = Math.max(Math.abs(scale.toMapY(Math.clamp(cy - halfWidth, 0, 1))),
                        Math.abs(scale.toMapY(Math.clamp(cy + halfWidth, 0, 1))));
                cosCone = Math.cos(Math.min(Math.PI, Math.toRadians(Math.hypot(lon, lat)) + STAR_OFFSET));
            } else {
                // The tilted plane is foreshortened by cos(tilt).
                double extent = 0;
                for (Viewport viewport : Display.getViewports()) {
                    double width = mv.cameraWidth(viewport);
                    extent = Math.max(extent, Math.hypot(width * viewport.aspect / 2, width / 2));
                }
                extent += Math.hypot(mv.cameraTranslationX(), mv.cameraTranslationY());
                double cosTilt = Math.abs(mv.dragRotation().rotateVector(Vec3.ZAxis).z);
                cosCone = Math.cos(Math.min(Math.PI / 2, Math.atan2(extent, cosTilt * viewpoint.distance) + STAR_OFFSET));
            }

            byte[] color = Display.whiteBackground ? Colors.Black.bytes() : Colors.White.bytes();
            for (Stars.Star star : catalog.stars()) {
                if (star.x() * locationSun[0] + star.y() * locationSun[1] + star.z() * locationSun[2] < cosCone)
                    continue;
                double[] apparent = Stars.apparent(star, years, locationSsb, locationHelio, helioDistance, locationVelocity);
                float size = (float) Math.clamp(5 - 0.28 * star.mag(), 2.5, 5.5);
                putLocation(star, projectLocation(apparent, true, locationSun, locationNorth, locationWest, sunDistance, viewpoint.distance, mv.mode()), mv, vp, size, color);
            }
        }
        locationStarCount = locationPointsBuf.getCount();

        for (PositionLoad load : location.getApparentPlanets()) {
            PositionResponse response = load.getResponse();
            if (response == null)
                continue;
            double objectDistance = response.interpolateRectangular(time, start, end, locationVector, locationInterpolated);
            Vec2 center = projectLocation(locationVector, false, locationSun, locationNorth, locationWest, sunDistance, viewpoint.distance, mv.mode());
            if (center == null && objectDistance >= sunDistance)
                continue;
            SpaceObject target = load.target();
            int pointIndex = locationPointsBuf.getCount();
            putLocation(load, center, mv, vp, SIZE_LOCATION_PLANET, target.getTrajectoryColor(Display.whiteBackground));
            double sinRadius = objectDistance < sunDistance ? target.getRadius() / objectDistance : 0;
            double x = 0, y = 0, z = 0;
            for (int i = 0; i < 3; i++) {
                x += locationVector[i] * locationWest[i];
                y += locationVector[i] * locationNorth[i];
                z += locationVector[i] * locationSun[i];
            }
            Vec3 direction = new Vec3(x / objectDistance, y / objectDistance, z / objectDistance);
            locationPlanets.add(new LocationPlanet(direction, objectDistance, sinRadius * sinRadius,
                    target.getEclipseColor(Display.whiteBackground), pointIndex, locationPointsBuf.getCount() - pointIndex));
        }
        locationPlanets.sort((a, b) -> Double.compare(b.distance(), a.distance()));
    }

    private void renderLocation(MapView mv) {
        Transform.pushView();
        if (mv.isOrthographic())
            Transform.rotateViewInverse(mv.viewRotation()); // points are in screen coordinates
        GL.glDisable(GL.DEPTH_TEST);
        locationPoints.renderPoints(Display.pixelScale[1], 0, locationStarCount);
        for (LocationPlanet planet : locationPlanets) {
            locationPoints.renderPoints(Display.pixelScale[1], planet.pointIndex(), planet.pointCount());
            if (planet.sinRadiusSquared() > 0)
                GLSLEclipseShader.render(mv, planet.direction(), planet.sinRadiusSquared(), planet.color());
        }
        GL.glEnable(GL.DEPTH_TEST);
        Transform.popView();
    }

    private void putLocation(Object object, @Nullable Vec2 xy, MapView mv, Viewport vp, float size, byte[] color) {
        if (xy == null)
            return;
        double x, y;
        if (mv.isHpc()) {
            MapScale scale = mv.scale(vp);
            double unitX = scale.toUnitX(xy.x), unitY = scale.toUnitY(xy.y);
            if (unitX < 0 || unitX > 1 || unitY < 0 || unitY > 1)
                return;
            x = (unitX - .5) * vp.aspect;
            y = unitY - .5;
        } else {
            // Transiting markers follow the solar surface; other markers stay on the image plane.
            double r2 = xy.x * xy.x + xy.y * xy.y;
            boolean transit = r2 < 1;
            locationPoint[0] = xy.x;
            locationPoint[1] = xy.y;
            locationPoint[2] = transit ? Math.sqrt(1 - r2) : 0;
            mv.dragRotation().qxv(locationPoint, locationPoint);
            if (transit ? locationPoint[2] <= 0 : locationPoint[0] * locationPoint[0] + locationPoint[1] * locationPoint[1] < 1)
                return;
            x = locationPoint[0];
            y = locationPoint[1];
        }
        locationPointsBuf.putVertex((float) x, (float) y, 0, size, color);
        if (mv == GLRenderer.getMapView())
            locationHover.add(new LocationHover(x, y, object, vp.idx));
    }

    private static double[] solarPole(double ra, double dec) {
        return new double[]{Math.cos(dec) * Math.cos(ra), Math.cos(dec) * Math.sin(ra), Math.sin(dec)};
    }

    // normalizes sun, solar north and west perpendicular to it
    static void helioprojectiveAxes(double[] sun, double sunDistance, double[] pole, double[] north, double[] west) {
        for (int i = 0; i < 3; i++)
            sun[i] /= sunDistance;
        double pz = pole[0] * sun[0] + pole[1] * sun[1] + pole[2] * sun[2];
        for (int i = 0; i < 3; i++)
            north[i] = pole[i] - pz * sun[i];
        double norm = Math.sqrt(north[0] * north[0] + north[1] * north[1] + north[2] * north[2]);
        for (int i = 0; i < 3; i++)
            north[i] /= norm;
        west[0] = sun[1] * north[2] - sun[2] * north[1]; // north x -sun
        west[1] = sun[2] * north[0] - sun[0] * north[2];
        west[2] = sun[0] * north[1] - sun[1] * north[0];
    }

    // Apparent angular position in HPC, or position on the Sun plane in Orthographic mode.
    @Nullable
    static Vec2 projectLocation(double[] v, boolean beyondSun, double[] sun, double[] north, double[] west, double sunDistance, double distance, MapMode mode) {
        double z = v[0] * sun[0] + v[1] * sun[1] + v[2] * sun[2];
        if (mode == MapMode.Orthographic && z <= 0)
            return null;
        double r = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if ((beyondSun || r > sunDistance) && z * sunDistance > r * Math.sqrt(sunDistance * sunDistance - Sun.Radius * Sun.Radius)) // cos(sep) > cos(solar angular radius)
            return null;
        double x = v[0] * west[0] + v[1] * west[1] + v[2] * west[2];
        double y = v[0] * north[0] + v[1] * north[1] + v[2] * north[2];
        if (mode == MapMode.HPC)
            return new Vec2(Math.toDegrees(Math.atan2(x, z)), Math.toDegrees(Math.atan2(y, Math.hypot(x, z))));
        return new Vec2(distance * x / z, distance * y / z);
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
