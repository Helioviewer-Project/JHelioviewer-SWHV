package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.camera.viewpoint.UpdateViewpoint;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Mat4f;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.time.JHVTime;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class Camera {

    public static final double ZOOM_MULTIPLIER_WHEEL = 2.;
    public static final double ZOOM_MULTIPLIER_BUTTON = 2.;

    public static final double INITFOV = 1. * Math.PI / 180.;
    private static final double MIN_FOV = INITFOV / 60;
    private static final double MAX_FOV = INITFOV * 30;
    private double fov = INITFOV;

    private Quat rotation = Quat.ZERO;
    private final Vec2 currentTranslation = new Vec2(0, 0);
    private Quat currentDragRotation = Quat.ZERO;
    private double cameraWidth = 1;

    private boolean tracking;

    private Position viewpoint = Sun.StartEarth;
    private UpdateViewpoint updateViewpoint;

    ////
    private static final float clipNarrow = (float) (32 * Sun.Radius); // bit more than LASCO C3
    private static final float clipWide = (float) (50 * Sun.MeanEarthDistance); // bit further than Pluto

    private final float[] invProj = new float[16];

    public Camera(UpdateViewpoint _updateViewpoint) {
        updateViewpoint = _updateViewpoint;
    }

    public static boolean useWideProjection(double distance) {
        return distance > 100 * Sun.MeanEarthDistance;
    }

    public void projectionOrtho2D(double aspect) {
        Transform.setOrthoSymmetricProjection((float) (cameraWidth * aspect), (float) cameraWidth, -1, 1);
        Transform.setTranslateView((float) currentTranslation.x, (float) currentTranslation.y, 0);
        Transform.cacheMVP();
    }

    public void projectionOrtho(double aspect, GL2 gl, GLSLShape blackCircle) {
        Transform.setOrthoSymmetricProjection((float) (cameraWidth * aspect), (float) cameraWidth, -clipNarrow, clipNarrow);
        Transform.setTranslateView((float) currentTranslation.x, (float) currentTranslation.y, 0);
        Transform.cacheMVP();

        blackCircle.renderShape(gl, GL2.GL_TRIANGLE_STRIP);

        Transform.rotateView(rotation);
        Transform.cacheMVP();
    }

    public float[] getTransformationInverse(double aspect) {
        Mat4f.orthoSymmetricInverse(invProj, (float) (cameraWidth * aspect), (float) cameraWidth, -1, 1);
        Mat4f.translate(invProj, -(float) currentTranslation.x, -(float) currentTranslation.y, 0);
        return invProj;
    }

    public void projectionOrthoWide(double aspect) {
        Transform.setOrthoSymmetricProjection((float) (cameraWidth * aspect), (float) cameraWidth, -clipWide, clipWide);
    }
////

    private void updateCamera(JHVTime time) {
        viewpoint = updateViewpoint.update(time);
        updateRotation();
        updateWidth();
    }

    private void updateRotation() {
        rotation = Quat.rotate(currentDragRotation, viewpoint.toQuat());
    }

    private void updateWidth() {
        cameraWidth = 2 * viewpoint.distance * Math.tan(0.5 * fov);
    }

    public void refresh() {
        updateCamera(Movie.getTime());
        MovieDisplay.render(1);
    }

    public void reset() {
        currentTranslation.x = 0;
        currentTranslation.y = 0;
        currentDragRotation = Quat.ZERO;

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

    public Vec2 getCurrentTranslation() {
        return currentTranslation;
    }

    public void setCurrentTranslation(double x, double y) {
        currentTranslation.x = x;
        currentTranslation.y = y;
    }

    public Quat getCurrentDragRotation() {
        return currentDragRotation;
    }

    void rotateCurrentDragRotation(Quat _currentDragRotation) {
        currentDragRotation = Quat.rotate(currentDragRotation, _currentDragRotation);
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
        jo.put("dragRotation", currentDragRotation.toJson());
        jo.put("translationX", currentTranslation.x);
        jo.put("translationY", currentTranslation.y);
        jo.put("fov", fov);
        return jo;
    }

    public void fromJson(JSONObject jo) {
        JSONArray ja = jo.optJSONArray("dragRotation");
        if (ja != null)
            currentDragRotation = Quat.fromJson(ja);
        currentTranslation.x = jo.optDouble("translationX", currentTranslation.x);
        currentTranslation.y = jo.optDouble("translationY", currentTranslation.y);
        fov = jo.optDouble("fov", fov);
    }

}
