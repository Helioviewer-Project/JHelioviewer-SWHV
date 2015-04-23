package org.helioviewer.gl3d.camera;

import org.helioviewer.base.physics.Constants;

/**
 * The trackball camera provides a trackball rotation behavior (
 * {@link GL3DTrackballRotationInteraction}) when in rotation mode. It is
 * currently the default camera.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DSolarRotationTrackingTrackballCamera extends GL3DCamera {

    protected static final double DEFAULT_CAMERA_DISTANCE = Constants.SunMeanDistanceToEarth / Constants.SunRadiusInMeter;

    private final GL3DTrackballRotationInteraction rotationInteraction;
    private final GL3DPanInteraction panInteraction;
    private final GL3DZoomBoxInteraction zoomBoxInteraction;

    protected GL3DInteraction currentInteraction;

    public GL3DSolarRotationTrackingTrackballCamera() {
        this.rotationInteraction = new GL3DTrackballRotationInteraction(this);
        this.panInteraction = new GL3DPanInteraction(this);
        this.zoomBoxInteraction = new GL3DZoomBoxInteraction(this);
        this.currentInteraction = this.rotationInteraction;
    }

    @Override
    public void reset() {
        super.reset();
        this.currentDragRotation.clear();
        this.currentInteraction.reset(this);
    }

    @Override
    public GL3DInteraction getPanInteraction() {
        return this.panInteraction;
    }

    @Override
    public GL3DInteraction getRotateInteraction() {
        return this.rotationInteraction;
    }

    @Override
    public GL3DInteraction getCurrentInteraction() {
        return this.currentInteraction;
    }

    @Override
    public void setCurrentInteraction(GL3DInteraction currentInteraction) {
        this.currentInteraction = currentInteraction;
    }

    @Override
    public GL3DInteraction getZoomInteraction() {
        return this.zoomBoxInteraction;
    }

    @Override
    public String getName() {
        return "Solar Rotation Tracking Camera";
    }

}
