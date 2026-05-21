package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.time.JHVTime;

import org.json.JSONArray;
import org.json.JSONObject;

public class Camera {

    public static final double ZOOM_MULTIPLIER_BUTTON = 1;
    private static final double ZOOM_STEP = 0.005;

    public static final double INITFOV = Math.PI / 180.;
    private static final double MIN_FOV = INITFOV / 360;
    private static final double MAX_FOV = INITFOV * 120;
    private double fov = INITFOV;

    private Vec2 translation = Vec2.ZERO;
    private Quat rotation = Quat.ZERO;
    private Quat dragRotation = Quat.ZERO;
    private double cameraWidth = 1;

    private final ViewpointModel viewpointModel;

    public Camera(ViewpointModel _viewpointModel) {
        viewpointModel = _viewpointModel;
    }

    public DisplayView displayView(Position p) {
        double width = 2 * p.distance * Math.tan(0.5 * fov);
        return displayView(p, width);
    }

    public DisplayView displayView(Position p, double width) {
        return new DisplayView(p, width, Quat.rotate(dragRotation, p.toQuat()));
    }

    public void updateViewpoint(JHVTime time) {
        Position viewpoint = viewpointModel.update(time);
        updateRotation();
        updateWidth(viewpoint);
    }

    private void updateRotation() {
        rotation = Quat.rotate(dragRotation, viewpointModel.getViewpoint().toQuat());
    }

    private void updateWidth(Position viewpoint) {
        cameraWidth = 2 * viewpoint.distance * Math.tan(0.5 * fov);
    }

    public void refresh() {
        updateViewpoint(Movie.getTime());
        MovieDisplay.render(1);
    }

    public void reset() {
        translation = Vec2.ZERO;
        dragRotation = Quat.ZERO;

        updateViewpoint(Movie.getTime());
        CameraHelper.zoomToFit(this);
        MovieDisplay.render(1);
    }

    public double getTranslationX() {
        return translation.x;
    }

    public double getTranslationY() {
        return translation.y;
    }

    public void setTranslation(double x, double y) {
        translation = new Vec2(x, y);
    }

    public Quat getDragRotation() {
        return dragRotation;
    }

    public void rotateDragRotation(Quat _dragRotation) {
        dragRotation = Quat.rotate(dragRotation, _dragRotation);
        updateRotation();
    }

    public void resetDragRotation() {
        dragRotation = Quat.ZERO;
        updateRotation();
    }

    public void resetDragRotationAxis() {
        dragRotation = dragRotation.twist(viewpointModel.getUpdateViewpoint().dragAxis());
        updateRotation();
    }

    public void setFOV(double _fov) {
        fov = Math.clamp(_fov, MIN_FOV, MAX_FOV);
        updateWidth(viewpointModel.getViewpoint());
    }

    public double getCameraWidth(Viewport vp) {
        return cameraWidth * vp.zoom;
    }

    public void zoom(double wr) {
        setFOV(fov * zoomFactor(wr));
    }

    public static double zoomFactor(double wr) {
        return Math.exp(ZOOM_STEP * wr); // smoother, direction-symmetric zooming via exponential scaling
    }

    public void timeChanged(JHVTime date) {
        if (!viewpointModel.getTrackingMode()) {
            updateViewpoint(date);
        }
    }

    public JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("translation", translation.toJson());
        jo.put("dragRotation", dragRotation.toJson());
        jo.put("fov", fov);
        return jo;
    }

    public void fromJson(JSONObject jo) {
        JSONArray ja;
        ja = jo.optJSONArray("translation");
        if (ja != null) translation = Vec2.fromJson(ja);
        ja = jo.optJSONArray("dragRotation");
        if (ja != null) dragRotation = Quat.fromJson(ja);
        setFOV(jo.optDouble("fov", fov));
        updateRotation();
    }

}
