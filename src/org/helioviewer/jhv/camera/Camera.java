package org.helioviewer.jhv.camera;

import java.util.HashSet;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Mat4f;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;
import org.json.JSONArray;
import org.json.JSONObject;

public class Camera {

    public static final double ZOOM_MULTIPLIER_WHEEL = 2.;
    public static final double ZOOM_MULTIPLIER_BUTTON = 2.;

    public static final double INITFOV = Math.PI / 180.;
    private static final double MIN_FOV = INITFOV / 360;
    private static final double MAX_FOV = INITFOV * 120;
    private double fov = INITFOV;

    private Quat rotation = Quat.ZERO;
    private final Vec2 translation = new Vec2(0, 0);
    private Quat dragRotation = Quat.ZERO;
    private double cameraWidth = 1;

    private boolean tracking;

    private Position viewpoint = Sun.StartEarth;
    private UpdateViewpoint updateViewpoint;

    public interface Listener {
        void viewpointChanged(Position v);
    }

    private final HashSet<Listener> listeners = new HashSet<>();

    public void addListener(Listener listener) {
        listeners.add(listener);
        listener.viewpointChanged(viewpoint);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    ////
    private static final float clipNarrow = (float) (32 * Sun.Radius); // bit more than LASCO C3
    private static final float clipWide = (float) (50 * Sun.MeanEarthDistance); // bit further than Pluto

    private final float[] invProj = new float[16];

    public Camera(UpdateViewpoint _updateViewpoint) {
        updateViewpoint = _updateViewpoint;
    }

    public void projectionOrtho2D(double aspect) {
        Transform.setOrthoSymmetricProjection((float) (cameraWidth * aspect), (float) cameraWidth, -1, 1);
        Transform.setTranslateView((float) translation.x, (float) translation.y, 0);
        Transform.cacheMVP();
    }

    public void projectionOrtho(double aspect) {
        float clip = cameraWidth < 32 ? clipNarrow : clipWide;
        Transform.setOrthoSymmetricProjection((float) (cameraWidth * aspect), (float) cameraWidth, -clip, clip);
        Transform.setTranslateView((float) translation.x, (float) translation.y, 0);
        Transform.rotateView(rotation);
        Transform.cacheMVP();
    }

    public float[] getTransformationInverse(double aspect) {
        Mat4f.orthoSymmetricInverse(invProj, (float) (cameraWidth * aspect), (float) cameraWidth, -1, 1);
        Mat4f.translate(invProj, -(float) translation.x, -(float) translation.y, 0);
        return invProj;
    }

////

    private void updateCamera(JHVTime time) {
        Position v = updateViewpoint.update(time);
        viewpoint = Display.mode == Display.ProjectionMode.Orthographic ? v : new Position(v.time, Sun.MeanEarthDistance, v.lon, v.lat);
        updateRotation();
        updateWidth();
        listeners.forEach(l -> l.viewpointChanged(viewpoint));
    }

    private void updateRotation() {
        rotation = Quat.rotate(dragRotation, viewpoint.toQuat());
    }

    private void updateWidth() {
        cameraWidth = 2 * viewpoint.distance * Math.tan(0.5 * fov);
    }

    public void refresh() {
        updateCamera(Movie.getTime());
        MovieDisplay.render(1);
    }

    public void reset() {
        translation.x = 0;
        translation.y = 0;
        dragRotation = Quat.ZERO;

        updateCamera(Movie.getTime());
        CameraHelper.zoomToFit(this);
        MovieDisplay.render(1);
    }

    public Position getViewpoint() {
        return viewpoint;
    }

    public UpdateViewpoint getUpdateViewpoint() {
        return updateViewpoint;
    }

    public void setViewpointUpdate(UpdateViewpoint _updateViewpoint) {
        updateViewpoint = _updateViewpoint;
        reset();
    }

    public Vec2 getTranslation() {
        return translation;
    }

    public void setTranslation(double x, double y) {
        translation.x = x;
        translation.y = y;
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
        Vec3 axis = updateViewpoint == UpdateViewpoint.equatorial ? Vec3.ZAxis : Vec3.YAxis;
        dragRotation = dragRotation.twist(axis);
        updateRotation();
    }

    public void setFOV(double _fov) {
        fov = MathUtils.clip(_fov, MIN_FOV, MAX_FOV);
        updateWidth();
    }

    public void setTrackingMode(boolean _tracking) {
        if (tracking != _tracking) {
            tracking = _tracking;
            refresh();
        }
    }

    public boolean getTrackingMode() {
        return tracking;
    }

    public double getCameraWidth() {
        return cameraWidth;
    }

    public void zoom(double wr) {
        setFOV(fov * (1 + 0.015 * wr));
    }

    public void timeChanged(JHVTime date) {
        if (!tracking) {
            updateCamera(date);
        }
    }

    public JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("dragRotation", dragRotation.toJson());
        jo.put("translationX", translation.x);
        jo.put("translationY", translation.y);
        jo.put("fov", fov);
        return jo;
    }

    public void fromJson(JSONObject jo) {
        JSONArray ja = jo.optJSONArray("dragRotation");
        if (ja != null)
            dragRotation = Quat.fromJson(ja);
        translation.x = jo.optDouble("translationX", translation.x);
        translation.y = jo.optDouble("translationY", translation.y);
        fov = jo.optDouble("fov", fov);
    }

}
