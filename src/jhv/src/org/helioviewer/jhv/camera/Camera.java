package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableCamera;

public class Camera {

    static enum CameraMode {
        OBSERVER, EARTH, EXPERT
    }

    private static final double INITFOV = (48. / 60.) * Math.PI / 180.;
    private static final double MIN_FOV = INITFOV * 0.1;
    private static final double MAX_FOV = INITFOV * 30;
    private double fov = INITFOV;

    private Quat rotation = new Quat();

    private final Quat currentDragRotation = new Quat();
    private Vec2 currentTranslation = new Vec2();

    private boolean trackingMode;

    private double cameraWidth = 1;

    private double FOVangleToDraw;

    private final InteractionRotate rotationInteraction = new InteractionRotate(this);
    private final InteractionPan panInteraction = new InteractionPan(this);
    private final InteractionAnnotate annotateInteraction = new InteractionAnnotate(this);
    private Interaction currentInteraction = rotationInteraction;

    private final PositionLoad positionLoad = new PositionLoad(this);

    private final ViewpointObserver viewpointObserver = new ViewpointObserver();
    private final ViewpointEarth viewpointEarth = new ViewpointEarth();
    private final ViewpointExpert viewpointExpert = new ViewpointExpert(positionLoad);
    private Viewpoint viewpoint = viewpointObserver;

    private CameraMode mode = CameraMode.OBSERVER;

    private void refresh() {
        timeChanged(Layers.getLastUpdatedTimestamp());
        Displayer.render();
    }

    void setMode(CameraMode _mode) {
        mode = _mode;
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
        currentTranslation = new Vec2(0, 0);
        currentDragRotation.clear();
        zoomToFit();
        refresh();
    }

    public void push(JHVDate date) {
        if (!trackingMode) {
            viewpoint.push();
            viewpoint.update(date);
            updateTransformation();
            updateWidth();
        }
    }

    public void pop() {
        if (!trackingMode) {
            viewpoint.pop();
            updateTransformation();
            updateWidth();
        }
    }

    public double getFOVAngleToDraw() {
        return FOVangleToDraw;
    }

    public double getDistance() {
        return viewpoint.distance;
    }

    public Quat getOrientation() {
        return viewpoint.orientation;
    }

    public Quat getCameraDifferenceRotationQuat(Quat rot) {
        Quat cameraDifferenceRotation = rotation.copy();
        cameraDifferenceRotation.rotateWithConjugate(rot);

        return cameraDifferenceRotation;
    }

    public Vec2 getCurrentTranslation() {
        return currentTranslation;
    }

    void setCurrentTranslation(Vec2 pan) {
        currentTranslation = pan;
        updateTransformation();
    }

    Quat getCurrentDragRotation() {
        return currentDragRotation;
    }

    void rotateCurrentDragRotation(Quat _currentDragRotation) {
        currentDragRotation.rotate(_currentDragRotation);
        updateTransformation();
    }

    private void updateTransformation() {
        rotation = currentDragRotation.copy();
        rotation.rotate(viewpoint.orientation);
    }

    private void updateWidth() {
        cameraWidth = viewpoint.distance * Math.tan(0.5 * fov);
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

    public void setFOVangleDegrees(double fovAngle) {
        FOVangleToDraw = fovAngle * Math.PI / 180.;
    }

    public void setCurrentInteraction(Interaction _currentInteraction) {
        currentInteraction = _currentInteraction;
    }

    public Interaction getCurrentInteraction() {
        return currentInteraction;
    }

    public Interaction getPanInteraction() {
        return panInteraction;
    }

    public Interaction getRotateInteraction() {
        return rotationInteraction;
    }

    public InteractionAnnotate getAnnotateInteraction() {
        return annotateInteraction;
    }

    CameraOptionPanelExpert expertOptionPanel = new CameraOptionPanelExpert(positionLoad);

    void firePositionLoaded(final String state) {
        expertOptionPanel.fireLoaded(state);
        refresh();
    }

    CameraOptionPanel getOptionPanel() {
        if (mode == CameraMode.EXPERT) {
            return expertOptionPanel;
        }
        return null;
    }

    public void timeChanged(JHVDate date) {
        if (!trackingMode) {
            viewpoint.update(date);
            updateTransformation();
            updateWidth();
        }
    }

    public void fireTimeUpdated() {
        RenderableCamera renderableCamera = ImageViewerGui.getRenderableCamera();
        if (renderableCamera != null) {
            renderableCamera.setTimeString(viewpoint.time.toString());
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(renderableCamera);
        }
    }

    public void zoomToFit() {
        double size = Layers.getLargestPhysicalSize();
        if (size == 0)
            setCameraFOV(INITFOV);
        else
            setCameraFOV(2. * Math.atan2(0.5 * size, viewpoint.distance));
    }

    public Mat4 getRotation() {
        return rotation.toMatrix();
    }

}
