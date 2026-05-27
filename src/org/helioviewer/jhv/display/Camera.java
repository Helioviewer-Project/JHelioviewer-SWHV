package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

import org.json.JSONArray;
import org.json.JSONObject;

public class Camera {

    static final double ZOOM_MULTIPLIER_BUTTON = 1;
    private static final double ZOOM_STEP = 0.005;

    static final double INITFOV = Math.PI / 180.;
    private static final double MIN_FOV = INITFOV / 360;
    private static final double MAX_FOV = INITFOV * 120;
    private double fov = INITFOV;

    private Vec2 translation = Vec2.ZERO;
    private Quat dragRotation = Quat.ZERO;
    private double cameraWidth = 1;

    Camera() {}

    void updateViewpoint(Position viewpoint) {
        updateWidth(viewpoint);
    }

    private void updateWidth(Position viewpoint) {
        cameraWidth = 2 * viewpoint.distance * Math.tan(0.5 * fov);
    }

    void reset(Position viewpoint) {
        translation = Vec2.ZERO;
        dragRotation = Quat.ZERO;

        updateViewpoint(viewpoint);
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
    }

    public void resetDragRotation() {
        dragRotation = Quat.ZERO;
    }

    void resetDragRotationAxis(Vec3 dragAxis) {
        dragRotation = dragRotation.twist(dragAxis);
    }

    public void setFOV(double _fov, Position viewpoint) {
        fov = Math.clamp(_fov, MIN_FOV, MAX_FOV);
        updateWidth(viewpoint);
    }

    public double getCameraWidth(double zoom) {
        return cameraWidth * zoom;
    }

    public static double zoomFactor(double wr) {
        return Math.exp(ZOOM_STEP * wr); // smoother, direction-symmetric zooming via exponential scaling
    }

    JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("translation", translation.toJson());
        jo.put("dragRotation", dragRotation.toJson());
        jo.put("fov", fov);
        return jo;
    }

    void fromJson(JSONObject jo, Position viewpoint) {
        JSONArray ja;
        ja = jo.optJSONArray("translation");
        if (ja != null) translation = Vec2.fromJson(ja);
        ja = jo.optJSONArray("dragRotation");
        if (ja != null) dragRotation = Quat.fromJson(ja);
        setFOV(jo.optDouble("fov", fov), viewpoint);
    }

}
