package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.Stars;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.MapMode;
import org.helioviewer.jhv.display.MapScale;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.display.ViewportMath;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectElement;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.GL;
import org.helioviewer.jhv.opengl.GLRenderer;
import org.helioviewer.jhv.opengl.GLSLEclipseShader;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.Transform;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.Task;

final class ViewpointSky {

    private static final float PLANET_SIZE = 6;
    private static final double STAR_OFFSET = 3e-3; // [rad] apparent minus catalogue star direction: proper motion 1990-2050, aberration at 192 km/s, deflection, parallax
    static final double[] SOLAR_POLE = solarPole(Math.toRadians(286.13), Math.toRadians(63.87)); // J2000, BODY10_POLE_RA/DEC in pck00011.tpc

    private static final String[] APPARENT_PLANETS = {"Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Moon"};

    private final ViewpointLayerOptionsExpert location;
    private boolean active;
    private final List<PositionLoad> apparentPlanets = new ArrayList<>();
    private PositionLoad apparentSun;
    private PositionLoad observerState;
    private PositionLoad heliocentric;
    private Future<Stars.Catalog> starCatalog;

    private record Body(Vec3 direction, double distance, double sinRadiusSquared, float[] color, int pointIndex, int pointCount) {}

    private record HoverPoint(double x, double y, Object object, int viewportIndex) {}

    private final GLSLShape points = new GLSLShape(true);
    private final BufVertex pointBuffer = new BufVertex();
    private int starCount;
    private final List<Body> bodies = new ArrayList<>();
    private final List<HoverPoint> hoverPoints = new ArrayList<>();
    private final double[] vector = new double[3];
    private final double[] point = new double[3];
    private final double[] sun = new double[3];
    private final double[] north = new double[3];
    private final double[] west = new double[3];
    private final double[] ssb = new double[3];
    private final double[] velocity = new double[3];
    private final double[] helio = new double[3];
    private final PositionResponse.Interpolated interpolated = new PositionResponse.Interpolated();

    ViewpointSky(ViewpointLayerOptionsExpert _location) {
        location = _location;
    }

    void init() {
        points.init();
    }

    void dispose() {
        points.dispose();
    }

    void setActive(boolean _active) {
        if (active == _active)
            return;
        active = _active;
        reload();
    }

    // apparent directions seen from the location in J2000, and its barycentric state and heliocentric position for the stars
    void reload() {
        cancelSky();
        updateStarCatalog();
        long startTime = location.getStartTime(), endTime = location.getEndTime();
        List<SpaceObjectElement> selected = location.getContainer().getSelectedElements();
        if (!active || !(location.isPlanets() || location.isStars()) || selected.isEmpty())
            return;
        SpaceObject observer = selected.getFirst().getTarget();

        PositionLoad.StatusReceiver receiver = status -> DisplayController.display();
        apparentSun = PositionLoad.submitApparent(receiver, observer, SpaceObject.SUN, startTime, endTime);
        if (location.isPlanets()) {
            for (String name : APPARENT_PLANETS) {
                SpaceObject planet = SpaceObject.get(name);
                if (planet != null && planet != observer)
                    apparentPlanets.add(PositionLoad.submitApparent(receiver, observer, planet, startTime, endTime));
            }
        }
        if (location.isStars()) {
            observerState = PositionLoad.submitBarycentricState(receiver, observer, startTime, endTime);
            heliocentric = PositionLoad.submit(receiver, SpaceObject.SUN, observer, Frame.J2000, startTime, endTime);
        }
    }

    private void cancelSky() {
        if (apparentSun != null)
            apparentSun.cancel();
        apparentPlanets.forEach(PositionLoad::cancel);
        if (observerState != null)
            observerState.cancel();
        if (heliocentric != null)
            heliocentric.cancel();
        observerState = null;
        heliocentric = null;
        apparentSun = null;
        apparentPlanets.clear();
    }

    // independent of time and location, kept while the stars are shown
    private void updateStarCatalog() {
        if (active && location.isStars()) {
            if (starCatalog == null)
                starCatalog = Task.submitBackground("stars", Stars::load, catalog -> DisplayController.display(), (logContext, t) -> {
                    if (!AppThread.isInterrupted(t))
                        Log.error(t);
                });
        } else if (starCatalog != null) {
            starCatalog.cancel(true);
            starCatalog = null;
        }
    }

    // Orthographic points are shared; HPC points use each viewport's angular scale.
    void compute(MapView mv, Viewport vp, boolean enabled) {
        // Preserve screen hover positions while rendering captures.
        if (mv == GLRenderer.getMapView() && vp.idx == 0)
            hoverPoints.clear();
        starCount = 0;
        bodies.clear();
        if (enabled)
            putSky(mv, vp);
        points.upload(pointBuffer);
        pointBuffer.clear();
    }

    private void putSky(MapView mv, Viewport vp) {
        PositionLoad observer = location.getHighlightedLoad();
        // Wait for the Location viewpoint before projecting with its camera distance.
        if (observer == null || observer.getResponse() == null || apparentSun == null)
            return;
        PositionResponse sunResponse = apparentSun.getResponse();
        if (sunResponse == null)
            return;

        Position viewpoint = mv.viewpoint();
        long time = Player.getTime().milli, start = Player.getStartTime(), end = Player.getEndTime();
        double sunDistance = sunResponse.interpolateRectangular(time, start, end, sun, interpolated);
        helioprojectiveAxes(sun, sunDistance, SOLAR_POLE, north, west);

        PositionResponse state = observerState == null ? null : observerState.getResponse();
        PositionResponse helioResponse = heliocentric == null ? null : heliocentric.getResponse();
        Stars.Catalog catalog = starCatalog != null && starCatalog.state() == Future.State.SUCCESS ? starCatalog.resultNow() : null;
        if (catalog != null && state != null && helioResponse != null) {
            state.interpolateState(time, start, end, ssb, velocity, interpolated);
            double years = Stars.julianYear(interpolated.time) - catalog.epoch();
            for (int i = 0; i < 3; i++)
                ssb[i] *= Sun.MeanEarthDistanceInv; // [au]
            double helioDistance = helioResponse.interpolateRectangular(time, start, end, helio, interpolated);
            for (int i = 0; i < 3; i++)
                helio[i] /= helioDistance;
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
                if (star.x() * sun[0] + star.y() * sun[1] + star.z() * sun[2] < cosCone)
                    continue;
                double[] apparent = Stars.apparent(star, years, ssb, helio, helioDistance, velocity);
                float size = (float) Math.clamp(5 - 0.28 * star.mag(), 2.5, 5.5);
                putPoint(star, projectLocation(apparent, true, sun, north, west, sunDistance, viewpoint.distance, mv.mode()), mv, vp, size, color);
            }
        }
        starCount = pointBuffer.getCount();

        for (PositionLoad load : apparentPlanets) {
            PositionResponse response = load.getResponse();
            if (response == null)
                continue;
            double objectDistance = response.interpolateRectangular(time, start, end, vector, interpolated);
            Vec2 center = projectLocation(vector, false, sun, north, west, sunDistance, viewpoint.distance, mv.mode());
            if (center == null && objectDistance >= sunDistance)
                continue;
            SpaceObject target = load.target();
            int pointIndex = pointBuffer.getCount();
            putPoint(load, center, mv, vp, PLANET_SIZE, target.getTrajectoryColor(Display.whiteBackground));
            double sinRadius = objectDistance < sunDistance ? target.getRadius() / objectDistance : 0;
            double x = 0, y = 0, z = 0;
            for (int i = 0; i < 3; i++) {
                x += vector[i] * west[i];
                y += vector[i] * north[i];
                z += vector[i] * sun[i];
            }
            Vec3 direction = new Vec3(x / objectDistance, y / objectDistance, z / objectDistance);
            bodies.add(new Body(direction, objectDistance, sinRadius * sinRadius,
                    target.getEclipseColor(Display.whiteBackground), pointIndex, pointBuffer.getCount() - pointIndex));
        }
        bodies.sort((a, b) -> Double.compare(b.distance(), a.distance()));
    }

    void render(MapView mv) {
        Transform.pushView();
        if (mv.isOrthographic())
            Transform.rotateViewInverse(mv.viewRotation()); // points are in screen coordinates
        GL.glDisable(GL.DEPTH_TEST);
        points.renderPoints(Display.pixelScale[1], 0, starCount);
        for (Body planet : bodies) {
            points.renderPoints(Display.pixelScale[1], planet.pointIndex(), planet.pointCount());
            if (planet.sinRadiusSquared() > 0)
                GLSLEclipseShader.render(mv, planet.direction(), planet.sinRadiusSquared(), planet.color());
        }
        GL.glEnable(GL.DEPTH_TEST);
        Transform.popView();
    }

    private void putPoint(Object object, @Nullable Vec2 xy, MapView mv, Viewport vp, float size, byte[] color) {
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
            point[0] = xy.x;
            point[1] = xy.y;
            point[2] = transit ? Math.sqrt(1 - r2) : 0;
            mv.dragRotation().qxv(point, point);
            if (transit ? point[2] <= 0 : point[0] * point[0] + point[1] * point[1] < 1)
                return;
            x = point[0];
            y = point[1];
        }
        pointBuffer.putVertex((float) x, (float) y, 0, size, color);
        if (mv == GLRenderer.getMapView())
            hoverPoints.add(new HoverPoint(x, y, object, vp.idx));
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

    @Nullable
    String hover(MapView mv, Viewport vp, int mouseX, int mouseY) {
        if (!(mv.isOrthographic() || mv.isHpc()))
            return null;
        double width = mv.cameraWidth(vp);
        double mousePlaneX = ViewportMath.computeUpX(vp, width, mv.cameraTranslationX(), mouseX);
        double mousePlaneY = ViewportMath.computeUpY(vp, width, mv.cameraTranslationY(), mouseY);

        double halfWidth = width / 2;
        double hoverThreshold2 = (0.01 * halfWidth) * (0.01 * halfWidth);
        double minDist2 = Double.MAX_VALUE;

        Object nearest = null;
        for (HoverPoint point : hoverPoints) {
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
        if (nearest == null || minDist2 >= hoverThreshold2)
            return null;
        return switch (nearest) {
            case PositionLoad load -> load.target().toString();
            case Stars.Star star -> star.hip() > 0 ? String.format("HIP%d,G%.2f", star.hip(), star.mag()) : String.format("G%.2f", star.mag());
            default -> nearest.toString();
        };
    }

}
