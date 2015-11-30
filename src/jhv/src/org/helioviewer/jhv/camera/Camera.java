package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;

public class Camera {

    static enum CameraMode {
        OBSERVER, EARTH, EXPERT
    }

    static final double INITFOV = (48. / 60.) * Math.PI / 180.;
    private static final double MIN_FOV = INITFOV * 0.1;
    private static final double MAX_FOV = INITFOV * 30;
    private double fov = INITFOV;

    private Quat rotation = new Quat();
    private final Vec2 currentTranslation = new Vec2();
    private final Quat currentDragRotation = new Quat();
    private double cameraWidth = 1;

    private boolean trackingMode;

    private final ViewpointObserver viewpointObserver = new ViewpointObserver();
    private final ViewpointEarth viewpointEarth = new ViewpointEarth();
    private final ViewpointExpert viewpointExpert = new ViewpointExpert(this);
    private Viewpoint viewpoint = viewpointObserver;

    private void updateCamera(JHVDate date) {
        viewpoint.update(date);
        updateTransformation();
        updateWidth();
    }

    private void updateTransformation() {
        rotation = currentDragRotation.copy();
        rotation.rotate(viewpoint.orientation);
    }

    private void updateWidth() {
        cameraWidth = viewpoint.distance * Math.tan(0.5 * fov);
    }

    void refresh() {
        updateCamera(Layers.getLastUpdatedTimestamp());
        Displayer.render();
    }

    void setMode(CameraMode mode) {
        switch (mode) {
            case EXPERT:
                viewpoint = viewpointExpert;
            break;
            case EARTH:
                viewpoint = viewpointEarth;
            break;
            default:
                viewpoint = viewpointObserver;
        }
        refresh();
    }

    public void reset() {
        currentTranslation.clear();
        currentDragRotation.clear();
        CameraHelper.zoomToFit(this);
        refresh();
    }

    private Viewpoint saveViewpoint;

    public void push(Viewpoint v) {
        if (!trackingMode) {
            saveViewpoint = viewpoint;
            viewpoint = v;
            updateTransformation();
            updateWidth();
        }
    }

    public void pop() {
        if (!trackingMode) {
            viewpoint = saveViewpoint;
            saveViewpoint = null;
            updateTransformation();
            updateWidth();
        }
    }

    public Viewpoint getViewpoint() {
        return viewpoint;
    }

    Quat getRotationQuat() {
        return rotation.copy();
    }

    public Mat4 getRotation() {
        return rotation.toMatrix();
    }

    public Vec2 getCurrentTranslation() {
        return currentTranslation;
    }

    void setCurrentTranslation(double x, double y) {
        currentTranslation.x = x;
        currentTranslation.y = y;
        updateTransformation();
    }

    Quat getCurrentDragRotation() {
        return currentDragRotation;
    }

    void rotateCurrentDragRotation(Quat _currentDragRotation) {
        currentDragRotation.rotate(_currentDragRotation);
        updateTransformation();
    }

    public void setCameraFOV(double _fov) {
        if (_fov < MIN_FOV) {
            fov = MIN_FOV;
        } else if (_fov > MAX_FOV) {
            fov = MAX_FOV;
        } else {
            fov = _fov;
        }
        updateWidth();
    }

    public void setTrackingMode(boolean _trackingMode) {
        trackingMode = _trackingMode;
        refresh();
    }

    public boolean getTrackingMode() {
        return trackingMode;
    }

    public double getWidth() {
        return cameraWidth;
    }

    public void zoom(int wr) {
        setCameraFOV(fov * (1 + 0.015 * wr));
    }

    CameraOptionPanel getOptionPanel() {
        return viewpoint.getOptionPanel();
    }

    public void timeChanged(JHVDate date) {
        if (!trackingMode) {
            updateCamera(date);
        }
    }

}
