package org.helioviewer.gl3d.camera;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DGrid;
import org.helioviewer.gl3d.wcs.CoordinateSystem;

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

    public static final double MIN_FOV = 0.05;
    public static final double MAX_FOV = 1000;

    private static final double INITFOV = 0.7;

    private double clipNear = Constants.SunRadius * 3;
    private double clipFar = Constants.SunRadius * 10000.;
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

    private final Stack<GL3DCameraAnimation> cameraAnimations = new Stack<GL3DCameraAnimation>();

    protected GL3DQuatd currentDragRotation;

    protected GL3DQuatd localRotation;

    private long timeDelay;

    private double translationz;

    private double ratio = 1.0;

    private double gridResolutionX = 20;
    private double gridResolutionY = 20;
    private GL3DGrid grid;

    private long time;

    private GL3DGrid followGrid;

    public GL3DCamera(double clipNear, double clipFar) {
        this();
        this.clipNear = clipNear;
        this.clipFar = clipFar;
    }

    public GL3DCamera() {
        this.cameraTransformation = GL3DMat4d.identity();
        this.rotation = GL3DQuatd.createRotation(0.0, new GL3DVec3d(0, 1, 0));
        this.currentDragRotation = GL3DQuatd.createRotation(0.0, new GL3DVec3d(0, 1, 0));
        this.localRotation = GL3DQuatd.createRotation(0.0, new GL3DVec3d(0, 1, 0));
        this.translation = new GL3DVec3d();
        this.grid = new GL3DGrid("grid", getGridResolutionX(), getGridResolutionY(), new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), false);
        this.followGrid = new GL3DGrid("grid", 90., 90., new GL3DVec4f(1.0f, 0.0f, 0.0f, 1.0f), new GL3DVec4d(0.0, 1.0, 0.0, 1.0), true);

        this.getGrid().getDrawBits().on(Bit.Hidden);
        this.getFollowGrid().getDrawBits().on(Bit.Hidden);

    }

    public GL3DGrid getGrid() {
        return this.grid;

    }

    public GL3DGrid setFollowGrid(GL3DGrid followgrid) {
        return this.followGrid = followgrid;

    }

    public abstract void createNewGrid();

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

    public void setGrid(GL3DGrid grid, GL3DGrid followGrid) {

        this.grid = grid;
        this.followGrid = followGrid;
    };

    public void reset() {
        this.resetFOV();
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
        return getTranslation().z;
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

    public void deactivate() {
        this.cameraAnimations.clear();
        //this.getGrid().getDrawBits().on(Bit.Hidden);
        //this.getFollowGrid().getDrawBits().on(Bit.Hidden);
    }

    public void activate() {
        //this.getGrid().getDrawBits().off(Bit.Hidden);
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
        this.rotation.clear();
        this.rotation.rotate(this.currentDragRotation);

        this.rotation.rotate(this.localRotation);

        cameraTransformation = GL3DMat4d.identity();
        cameraTransformation.translate(this.translation);
        cameraTransformation.multiply(this.rotation.toMatrix());

        if (fireEvent) {
            fireCameraMoved();
        }
    }

    public void applyCamera(GL3DState state) {
        for (Iterator<GL3DCameraAnimation> iter = this.cameraAnimations.iterator(); iter.hasNext();) {
            GL3DCameraAnimation animation = iter.next();
            if (!animation.isFinished()) {
                animation.animate(this);
            } else {
                iter.remove();
            }
        }
        state.multiplyMV(cameraTransformation);
    }

    public void addCameraAnimation(GL3DCameraAnimation animation) {
        for (Iterator<GL3DCameraAnimation> iter = this.cameraAnimations.iterator(); iter.hasNext();) {
            GL3DCameraAnimation ani = iter.next();
            if (!ani.isFinished() && ani.getClass().isInstance(animation)) {
                ani.updateWithAnimation(animation);
                return;
            }
        }

        this.cameraAnimations.add(animation);
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
        return this.fov = fov;
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

    public boolean isAnimating() {
        return !this.cameraAnimations.isEmpty();
    }

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

    public GL3DGrid getFollowGrid() {
        return this.followGrid;
    }
}
