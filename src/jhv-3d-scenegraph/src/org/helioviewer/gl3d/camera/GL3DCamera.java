package org.helioviewer.gl3d.camera;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DGrid;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * The GL3DCamera is responsible for the view space transformation. It sets up
 * the perspective and is generates the view space transformation. This
 * transformation is in turn influenced by the user interaction. Different
 * styles of user interaction are supported. These interactions are encapsulated
 * in {@link GL3DInteraction} objects that can be selected in the main toolbar.
 * The interactions then change the rotation and translation fields out of which
 * the resulting cameraTransformation is generated.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DCamera {
    protected GLU glu = new GLU();

    public static final double MAX_DISTANCE = -Constants.SunMeanDistanceToEarth * 1.8;
    public static final double MIN_DISTANCE = -Constants.SunRadius * 1.2;

    public static final double INITFOV = 0.6 * 4096. / 3600.;

    public static final double MIN_FOV = INITFOV * 0.05;
    public static final double MAX_FOV = INITFOV * 100;
    private final double clipNear = Constants.SunRadius * 3;
    private final double clipFar = Constants.SunRadius * 10000.;
    private double fov = INITFOV;
    private double aspect = 0.0;
    private double width = 0.0;
    private double height = 0.0;
    public int currentMouseX = 0;
    public int currentMouseY = 0;

    private final List<GL3DCameraListener> listeners = new ArrayList<GL3DCameraListener>();

    private GL3DMat4d cameraTransformation;

    protected GL3DQuatd rotation;
    protected GL3DVec3d translation;

    protected GL3DQuatd currentDragRotation;

    protected GL3DQuatd localRotation;

    private long timeDelay;

    private double translationz;

    private double ratio = 1.0;

    private double gridResolutionX = 20;
    private double gridResolutionY = 20;
    protected GL3DGrid grid;
    private GL3DGrid followGrid;

    private long time;

    private boolean trackingMode;

    protected GL3DSceneGraphView sceneGraphView;

    public GL3DCamera(GL3DSceneGraphView sceneGraphView) {
        this.sceneGraphView = sceneGraphView;
        this.cameraTransformation = GL3DMat4d.identity();
        this.rotation = GL3DQuatd.createRotation(0, GL3DVec3d.YAxis);
        this.currentDragRotation = GL3DQuatd.createRotation(0, GL3DVec3d.YAxis);
        this.localRotation = GL3DQuatd.createRotation(0, GL3DVec3d.YAxis);
        this.translation = new GL3DVec3d();
        GL3DGrid grid = new GL3DGrid("grid", getGridResolutionX(), getGridResolutionY(), new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), false);
        GL3DGrid followGrid = new GL3DGrid("grid", 90., 90., new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), true);
        this.sceneGraphView.getRoot().addNode(grid);
        this.sceneGraphView.getRoot().addNode(followGrid);
        this.grid = grid;
        this.followGrid = followGrid;
        this.grid.getDrawBits().on(Bit.Hidden);
        this.followGrid.getDrawBits().on(Bit.Hidden);
    }

    public GL3DGrid getGrid() {
        return this.grid;
    }

    public GL3DGrid setFollowGrid(GL3DGrid followgrid) {
        return this.followGrid = followgrid;

    }

    public void setGridResolution(int resolution) {
        this.setGridResolutionX(resolution);
        this.setGridResolutionY(resolution);
        createNewGrid();
    }

    public void setGridResolutionX(double resolution) {
        this.gridResolutionX = resolution;
        createNewGrid();
    }

    public void setGridResolutionY(double resolution) {
        this.gridResolutionY = resolution;
        createNewGrid();
    }

    protected GL3DSceneGraphView getSceneGraphView() {
        return this.sceneGraphView;
    }

    public void reset() {
        this.resetFOV();
        this.translation = new GL3DVec3d(0, 0, this.translation.z);
    }

    private void resetFOV() {
        this.fov = INITFOV;
    }

    /**
     * This method is called when the camera changes and should copy the
     * required settings of the preceding camera objects.
     *
     * @param precedingCamera
     */
    public void activate(GL3DCamera precedingCamera) {
        if (precedingCamera != null) {
            this.rotation = precedingCamera.getRotation().copy();
            this.translation = precedingCamera.translation.copy();
            this.width = precedingCamera.width;
            this.height = precedingCamera.height;
            this.updateCameraTransformation();

            // Also set the correct interaction
            if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getRotateInteraction())) {
                this.setCurrentInteraction(this.getRotateInteraction());
            } else if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getPanInteraction())) {
                this.setCurrentInteraction(this.getPanInteraction());
            } else if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getZoomInteraction())) {
                this.setCurrentInteraction(this.getZoomInteraction());
            }
        } else {
            Log.debug("GL3DCamera: No Preceding Camera, resetting Camera");
            this.reset();
        }
    }

    protected void setZTranslation(double z) {
        this.translationz = Math.min(MIN_DISTANCE, Math.max(MAX_DISTANCE, z));
        this.translation.z = this.ratio * this.translationz;
    }

    protected void addPanning(double x, double y) {
        setPanning(this.translation.x + x, this.translation.y + y);
    }

    public void setPanning(double x, double y) {
        this.translation.x = x;
        this.translation.y = y;
    }

    public GL3DVec3d getTranslation() {
        return this.translation;
    }

    public GL3DMat4d getCameraTransformation() {
        return this.cameraTransformation;
    }

    public double getZTranslation() {
        return this.translation.z;
    }

    public GL3DQuatd getLocalRotation() {
        return this.localRotation;
    }

    public GL3DQuatd getRotation() {
        this.updateCameraTransformation();
        return this.rotation;
    }

    public void resetCurrentDragRotation() {
        this.currentDragRotation.clear();
    }

    public void setLocalRotation(GL3DQuatd localRotation) {
        this.localRotation = localRotation;
        this.rotation.clear();
        this.updateCameraTransformation();

    }

    public void setCurrentDragRotation(GL3DQuatd currentDragRotation) {
        this.currentDragRotation = currentDragRotation;
        this.rotation.clear();
        this.updateCameraTransformation();
    }

    public void rotateCurrentDragRotation(GL3DQuatd currentDragRotation) {
        this.currentDragRotation.rotate(currentDragRotation);
        this.rotation.clear();
        this.updateCameraTransformation();
    }

    public void setSceneGraphView(GL3DSceneGraphView sgv) {
        this.sceneGraphView = sgv;
    }

    public void activate() {
        //this.getGrid().getDrawBits().off(Bit.Hidden);
    }

    public void createNewGrid() {
        this.createNewGrid(this.sceneGraphView);
    }

    public void createNewGrid(GL3DSceneGraphView gv) {
        boolean hidden = getGrid().getDrawBits().get(Bit.Hidden);
        this.sceneGraphView.getRoot().removeNode(this.grid);
        this.sceneGraphView.getRoot().removeNode(this.followGrid);

        GL3DGrid newGrid = new GL3DGrid("grid", getGridResolutionX(), getGridResolutionY(), new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), false);
        newGrid.getDrawBits().set(Bit.Hidden, hidden);
        GL3DGrid followCameraGrid = new GL3DGrid("grid", 90., 90., new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), true);
        followCameraGrid.getDrawBits().set(Bit.Hidden, hidden);
        gv.getRoot().addNode(this.grid);
        gv.getRoot().addNode(this.followGrid);
    }

    public void applyPerspective(GL3DState state) {
        GL2 gl = state.gl;
        int viewport[] = new int[4];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
        this.width = viewport[2];
        this.height = viewport[3];
        this.aspect = width / height;

        gl.glMatrixMode(GL2.GL_PROJECTION);

        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluPerspective(this.fov, this.aspect, this.clipNear, this.clipFar);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public void resumePerspective(GL3DState state) {
        GL2 gl = state.gl;
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public void updateCameraTransformation() {
        this.updateCameraTransformation(true);
    }

    public void updateCameraTransformation(GL3DMat4d transformation) {
        this.cameraTransformation = transformation;
        // fireCameraMoved();
    }

    /**
     * Updates the camera transformation by applying the rotation and
     * translation information.
     */
    public void updateCameraTransformation(boolean fireEvent) {
        this.rotation = this.currentDragRotation.copy();
        this.rotation.rotate(this.localRotation);

        cameraTransformation = GL3DMat4d.translation(this.translation);
        cameraTransformation.multiply(this.rotation.toMatrix());

        if (fireEvent) {
            fireCameraMoved();
        }
    }

    public void applyCamera(GL3DState state) {
        state.multiplyMV(cameraTransformation);
    }

    public abstract GL3DMat4d getVM();

    public abstract double getDistanceToSunSurface();

    public abstract GL3DInteraction getPanInteraction();

    public abstract GL3DInteraction getRotateInteraction();

    public abstract GL3DInteraction getZoomInteraction();

    public abstract String getName();

    public void drawCamera(GL3DState state) {
        getCurrentInteraction().drawInteractionFeedback(state, this);
    }

    public abstract GL3DInteraction getCurrentInteraction();

    public abstract void setCurrentInteraction(GL3DInteraction currentInteraction);

    public double getCameraFOV() {
        return this.fov;
    }

    public double setCameraFOV(double fov) {
        if (fov < MIN_FOV) {
            this.fov = MIN_FOV;
        } else if (fov > MAX_FOV) {
            this.fov = MAX_FOV;
        } else {
            this.fov = fov;
        }
        return this.fov;
    }

    public double getClipNear() {
        return clipNear;
    }

    public double getClipFar() {
        return clipFar;
    }

    public double getAspect() {
        return aspect;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void addCameraListener(GL3DCameraListener listener) {
        this.listeners.add(listener);
    }

    public void removeCameraListener(GL3DCameraListener listener) {
        this.listeners.remove(listener);
    }

    protected void fireCameraMoved() {
        for (GL3DCameraListener l : this.listeners) {
            l.cameraMoved(this);
        }
    }

    protected void fireCameraMoving() {
        for (GL3DCameraListener l : this.listeners) {
            l.cameraMoving(this);
        }
    }

    public abstract CoordinateSystem getViewSpaceCoordinateSystem();

    public void updateRotation(long dateMillis) {

    }

    public void setTimeDelay(long timeDelay) {
        this.timeDelay = timeDelay;
    }

    public long getTimeDelay() {
        return this.timeDelay;
    }

    public GL3DQuatd getCurrentDragRotation() {
        return this.currentDragRotation;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
        this.translation.z = this.translationz * this.ratio;
    }

    public double getGridResolutionX() {
        return gridResolutionX;
    }

    public double getGridResolutionY() {
        return gridResolutionY;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setTrackingMode(boolean trackingMode) {
        this.trackingMode = trackingMode;
    }

    public boolean getTrackingMode() {
        return this.trackingMode;
    }

    public void deactivate() {
    }

    public void setDefaultFOV() {
        this.fov = INITFOV;
    }

    public GL3DNode getFollowGrid() {
        return this.followGrid;
    }

}
