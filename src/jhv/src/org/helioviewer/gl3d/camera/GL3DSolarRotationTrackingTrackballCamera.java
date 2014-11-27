package org.helioviewer.gl3d.camera;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DGrid;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.HeliocentricCartesian2000CoordinateSystem;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.View;

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

    private GL3DSceneGraphView sceneGraphView;

    protected GL3DInteraction currentInteraction;

    private GL3DSceneGraphView lastScenegraph;

    private GL3DGrid followCameraGrid;

    public GL3DSolarRotationTrackingTrackballCamera(GL3DSceneGraphView sceneGraphView) {
        super();
        this.setSceneGraphView(sceneGraphView);
        this.rotationInteraction = new GL3DTrackballRotationInteraction(this, sceneGraphView);
        this.panInteraction = new GL3DPanInteraction(this, sceneGraphView);
        this.zoomBoxInteraction = new GL3DZoomBoxInteraction(this, sceneGraphView);
        this.currentInteraction = this.rotationInteraction;
        this.setGrid(this.getGrid(), this.getFollowGrid());
    }

    @Override
    public void applyCamera(GL3DState state) {
        super.applyCamera(state);
    }

    public void setSceneGraphView(GL3DSceneGraphView sceneGraphView) {
        this.sceneGraphView = sceneGraphView;
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

    @Override
    public void createNewGrid() {
        boolean hidden = getGrid().getDrawBits().get(Bit.Hidden);
        getSceneGraphView().getRoot().removeNode(getGrid());
        if (lastScenegraph == getSceneGraphView()) {
            getSceneGraphView().getRoot().removeNode(followCameraGrid);
        }
        lastScenegraph = getSceneGraphView();

        GL3DGrid newGrid = new GL3DGrid("grid", getGridResolutionX(), getGridResolutionY(), new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), false);
        newGrid.getDrawBits().set(Bit.Hidden, hidden);
        followCameraGrid = new GL3DGrid("grid", 90., 90., new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), true);
        newGrid.getDrawBits().set(Bit.Hidden, hidden);
        followCameraGrid.getDrawBits().set(Bit.Hidden, hidden);
        this.setGrid(newGrid, followCameraGrid);
    }

    @Override
    public void setGrid(GL3DGrid grid, GL3DGrid followCameraGrid) {
        super.setGrid(grid, followCameraGrid);
        grid.toString();
        getSceneGraphView().getRoot().addNode(grid);
        getSceneGraphView().getRoot().addNode(followCameraGrid);
    }

    public GL3DSceneGraphView getSceneGraphView() {
        return sceneGraphView;
    }

    public void viewChanged(View sender, ChangeEvent aEvent) {
    }
}