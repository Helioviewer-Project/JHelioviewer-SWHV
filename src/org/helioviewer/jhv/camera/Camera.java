package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.math.Mat4f;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.time.JHVDate;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class Camera {

    public static final double INITFOV = 1. * Math.PI / 180.;
    private static final double MIN_FOV = INITFOV * 0.1;
    private static final double MAX_FOV = INITFOV * 30;
    private double fov = INITFOV;

    private Quat rotation = Quat.ZERO;
    private final Vec2 currentTranslation = new Vec2(0, 0);
    private Quat currentDragRotation = Quat.ZERO;
    private double cameraWidth = 1;

    private boolean tracking;

    private Position viewpoint = Sun.StartEarth;

////
    private static final float halfDepth = (float) (3 * Sun.MeanEarthDistance);

    public void applyPerspectiveLatitudinal(double aspect) {
        Transform.setOrthoProjection(-(float) (cameraWidth * aspect), (float) (cameraWidth * aspect), -(float) cameraWidth, (float) cameraWidth, -1, 1);
        Transform.setTranslateView((float) currentTranslation.x, (float) currentTranslation.y, 0);
    }

    public void applyPerspective(double aspect, GL2 gl, GLSLShape blackCircle) {
        Transform.setOrthoProjection(-(float) (cameraWidth * aspect), (float) (cameraWidth * aspect), -(float) cameraWidth, (float) cameraWidth, -halfDepth, halfDepth);
        Transform.setTranslateView((float) currentTranslation.x, (float) currentTranslation.y, 0);

        blackCircle.renderShape(gl, GL2.GL_TRIANGLE_FAN);

        Transform.rotateView(rotation);
    }

    public float[] getTransformationInverse(double aspect) {
        return Mat4f.orthoInverse(-(float) (cameraWidth * aspect), (float) (cameraWidth * aspect), -(float) cameraWidth, (float) cameraWidth, -1, 1)
                    .translate(-(float) currentTranslation.x, -(float) currentTranslation.y, 0).m;
    }
////

    private void updateCamera(JHVDate time) {
        viewpoint = Display.getUpdateViewpoint().update(time);
        updateRotation();
        updateWidth();
    }

    private void updateRotation() {
        rotation = Quat.rotate(currentDragRotation, viewpoint.toQuat());
    }

    private void updateWidth() {
        cameraWidth = viewpoint.distance * Math.tan(0.5 * fov);
    }

    public void refresh() {
        updateCamera(Movie.getTime());
        Display.render(1);
    }

    public void reset() {
        currentTranslation.clear();
        currentDragRotation = Quat.ZERO;

        updateCamera(Movie.getTime());
        CameraHelper.zoomToFit(this);
        Display.render(1);
    }

    public Position getViewpoint() {
        return viewpoint;
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
        if (_fov < MIN_FOV) {
            fov = MIN_FOV;
        } else if (_fov > MAX_FOV) {
            fov = MAX_FOV;
        } else {
            fov = _fov;
        }
        updateWidth();
    }

    public double getFOV() {
        return fov;
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

    public double getWidth() {
        return cameraWidth;
    }

    public void zoom(double wr) {
        setFOV(fov * (1 + 0.015 * wr));
    }

    public void timeChanged(JHVDate date) {
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
