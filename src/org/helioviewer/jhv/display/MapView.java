package org.helioviewer.jhv.display;

import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.BufVertex;

public abstract class MapView {

    protected final Camera camera;
    protected final Position viewpoint;
    protected final MapMode mode;
    protected final GridType gridType;
    private final MapScale[] scales;
    private final Quat dragRotation;
    private final Quat viewRotation;
    private final double latiLongitudeOrigin;
    private final double latiLatitudeOrigin;

    MapView(Camera _camera, Position _viewpoint, MapMode _mode, GridType _gridType, MapScale[] _scales) {
        camera = _camera;
        viewpoint = _viewpoint;
        mode = _mode;
        gridType = _gridType;
        scales = _scales;
        dragRotation = camera.getDragRotation();
        viewRotation = Quat.rotate(dragRotation, viewpoint.toQuat());
        latiLongitudeOrigin = mode == MapMode.Latitudinal ? gridType.toLongitude(viewpoint) : 0;
        latiLatitudeOrigin = mode == MapMode.Latitudinal ? gridType.toLatitude(viewpoint) : 0;
    }

    public static MapView create(Camera camera, Position viewpoint, MapMode mode, GridType gridType, MapScale[] scales) {
        return mode == MapMode.Orthographic
                ? new OrthographicView(camera, viewpoint, gridType, scales)
                : new ProjectedView(camera, viewpoint, gridType, mode, scales);
    }

    public double cameraWidth(Viewport vp) {
        return mode.baseCameraWidth(camera) * vp.zoom;
    }

    public double cameraTranslationX() {
        return camera.getTranslationX();
    }

    public double cameraTranslationY() {
        return camera.getTranslationY();
    }

    public MapMode mode() {
        return mode;
    }

    public GridType gridType() {
        return gridType;
    }

    public Position viewpoint() {
        return viewpoint;
    }

    public Quat viewRotation() {
        return viewRotation;
    }

    public double latiLongitudeOrigin() {
        return latiLongitudeOrigin;
    }

    public double latiLatitudeOrigin() {
        return latiLatitudeOrigin;
    }

    public MapScale scale(Viewport vp) {
        return scales[vp.idx];
    }

    public Quat dragRotation() { // only for ViewpointLayer
        return dragRotation;
    }

    public boolean isOrthographic() {
        return mode == MapMode.Orthographic;
    }

    public boolean isHpc() {
        return mode == MapMode.HPC;
    }

    public boolean isLatitudinal() {
        return mode == MapMode.Latitudinal;
    }

    public boolean isRadialWarp() {
        return mode == MapMode.RadialWarp;
    }

    public boolean isRectWarp() {
        return mode == MapMode.RectWarp;
    }

    public Vec3 mouseToSky(Viewport vp, int x, int y) {
        return ViewportMath.unprojectToCurrentViewSphereOrPlane(camera, vp, cameraWidth(vp), x, y);
    }

    public Vec3 mouseToPlane(Viewport vp, int x, int y) { // only for SWEKPopupController
        return ViewportMath.unprojectToOutputPlane(camera, vp, cameraWidth(vp), x, y, Quat.ZERO);
    }

    public abstract Vec2 projectToScreen(Viewport vp, Vec3 v);

    public abstract Vec2 mouseToMap(Viewport vp, int x, int y);

    public abstract Vec3 mouseToSurface(Viewport vp, int x, int y);

    public abstract Vec2 mouseToScreen(Viewport vp, int x, int y); // only for SWEKPopupController

    public abstract void emitMapLine(Viewport vp, List<Vec3> vertices, double radius, byte[] color, BufVertex vexBuf);

    public abstract void emitMapPoints(Viewport vp, List<Vec3> vertices, double size, double radius, byte[] color, BufVertex vexBuf);
}
