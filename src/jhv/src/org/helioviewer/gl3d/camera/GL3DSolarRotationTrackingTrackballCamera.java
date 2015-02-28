package org.helioviewer.gl3d.camera;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.HeliocentricCartesian2000CoordinateSystem;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * The trackball camera provides a trackball rotation behavior (
 * {@link GL3DTrackballRotationInteraction}) when in rotation mode. It is
 * currently the default camera.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DSolarRotationTrackingTrackballCamera extends GL3DCamera {
    public static final double DEFAULT_CAMERA_DISTANCE = Constants.SunMeanDistanceToEarth / Constants.SunRadiusInMeter;

    private GL3DRay lastMouseRay;

    protected CoordinateSystem viewSpaceCoordinateSystem = new HeliocentricCartesian2000CoordinateSystem();

    private final GL3DTrackballRotationInteraction rotationInteraction;
    private final GL3DPanInteraction panInteraction;
    private final GL3DZoomBoxInteraction zoomBoxInteraction;

    protected GL3DInteraction currentInteraction;

    protected GL3DSceneGraphView lastScenegraph;

    public GL3DSolarRotationTrackingTrackballCamera(GL3DSceneGraphView sceneGraphView) {
        super(sceneGraphView);
        this.rotationInteraction = new GL3DTrackballRotationInteraction(this, sceneGraphView);
        this.panInteraction = new GL3DPanInteraction(this, sceneGraphView);
        this.zoomBoxInteraction = new GL3DZoomBoxInteraction(this, sceneGraphView);
        this.currentInteraction = this.rotationInteraction;
    }

    @Override
    public void applyCamera(GL3DState state) {
        super.applyCamera(state);
    }

    @Override
    public void reset() {
        super.reset();
        this.currentDragRotation.clear();
        this.currentInteraction.reset(this);
    }

    @Override
    public double getDistanceToSunSurface() {
        return -this.getCameraTransformation().translation().z;
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

    public GL3DRay getLastMouseRay() {
        return lastMouseRay;
    }

    @Override
    public CoordinateSystem getViewSpaceCoordinateSystem() {
        return this.viewSpaceCoordinateSystem;
    }

    @Override
    public GL3DMat4d getVM() {
        GL3DMat4d c = this.getCameraTransformation().copy();
        return c;
    }

    @Override
    public String getName() {
        return "Solar Rotation Tracking Camera";
    }

}
