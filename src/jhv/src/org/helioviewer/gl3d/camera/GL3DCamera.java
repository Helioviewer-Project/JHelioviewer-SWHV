package org.helioviewer.gl3d.camera;

import java.awt.Point;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.math.GL3DMat4d;
import org.helioviewer.gl3d.math.GL3DQuatd;
import org.helioviewer.gl3d.math.GL3DVec2d;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;

/**
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public abstract class GL3DCamera {

    protected GLU glu = new GLU();

    public static final double MAX_DISTANCE = -Constants.SunMeanDistanceToEarth * 1.8;
    public static final double MIN_DISTANCE = -Constants.SunRadius * 1.2;
    public static final double INITFOV = (48. / 60.) * Math.PI / 180.;
    public static final double MIN_FOV = INITFOV * 0.05;
    public static final double MAX_FOV = INITFOV * 100;
    private final double clipNear = Constants.SunRadius * 3;
    private final double clipFar = Constants.SunRadius * 10000.;
    private double fov = INITFOV;
    private double aspect = 0.0;
    private double previousAspect = -1.0;

    private final RayTracer rayTracer;
    private GL3DMat4d cameraTransformation;

    protected GL3DQuatd rotation;
    protected GL3DVec3d translation;

    protected GL3DQuatd currentDragRotation;

    protected GL3DQuatd localRotation;

    private long timeDelay;

    private double translationz;

    private double ratio = 1.0;

    private long time;

    private boolean trackingMode;

    public GL3DMat4d orthoMatrixInverse = GL3DMat4d.identity();

    private double cameraWidth = 1.;
    private double previousCameraWidth = -1;

    private double cameraWidthTimesAspect;

    public GL3DCamera() {
        this.cameraTransformation = GL3DMat4d.identity();
        this.rotation = new GL3DQuatd();
        this.currentDragRotation = new GL3DQuatd();
        this.localRotation = new GL3DQuatd();
        this.translation = new GL3DVec3d();
        rayTracer = new RayTracer(this);
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
            this.updateCameraTransformation();

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

    public GL3DQuatd getRotationInverse() {
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

    public void activate() {
    }

    public void applyPerspective(GL3DState state) {
        GL2 gl = state.gl;
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        this.aspect = state.getViewportWidth() / (double) state.getViewportHeight();
        cameraWidth = -translation.z * Math.tan(fov / 2.);
        if (cameraWidth == 0.)
            cameraWidth = 1.;

        cameraWidthTimesAspect = cameraWidth * aspect;
        gl.glLoadIdentity();
        gl.glOrtho(-cameraWidthTimesAspect, cameraWidthTimesAspect, -cameraWidth, cameraWidth, clipNear, clipFar);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        if (cameraWidth == previousCameraWidth && aspect == previousAspect) {
            return;
        }
        previousCameraWidth = cameraWidth;
        previousAspect = aspect;
        //orthoMatrix = GL3DMat4d.ortho(-cameraWidthTimesAspect, cameraWidthTimesAspect, -cameraWidth, cameraWidth, clipNear, clipFar);
        orthoMatrixInverse = GL3DMat4d.orthoInverse(-cameraWidthTimesAspect, cameraWidthTimesAspect, -cameraWidth, cameraWidth, clipNear, clipFar);
    }

    public void resumePerspective(GL3DState state) {
        GL2 gl = state.gl;
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public GL3DVec3d getVectorFromSphereOrPlane(GL3DVec2d normalizedScreenpos, GL3DMat4d cameraDifferenceRotation) {
        //GL3DVec4d up1 = this.orthoMatrixInverse.multiply(normalizedScreenpos);
        double up1x = normalizedScreenpos.x * cameraWidthTimesAspect;
        double up1y = normalizedScreenpos.y * cameraWidth;

        GL3DVec3d hitPoint;
        GL3DVec3d rotatedHitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 1) {
            hitPoint = new GL3DVec3d(up1x, up1y, Math.sqrt(1. - radius2));
            rotatedHitPoint = cameraDifferenceRotation.multiplyTranspose(hitPoint);
            if (rotatedHitPoint.z > 0.) {
                return rotatedHitPoint;
            }
        }
        GL3DVec3d altnormal = cameraDifferenceRotation.multiply(GL3DVec3d.ZAxis);
        double zvalue = -(altnormal.x * up1x + altnormal.y * up1y) / altnormal.z;
        hitPoint = new GL3DVec3d(up1x, up1y, zvalue);
        rotatedHitPoint = cameraDifferenceRotation.multiplyTranspose(hitPoint);
        return rotatedHitPoint;
    }

    public GL3DVec3d getVectorFromSphere(Point viewportCoordinates) {
        GL3DVec3d hp = rayTracer.cast(viewportCoordinates.getX(), viewportCoordinates.getY()).getHitpoint();
        return hp;
    }

    public GL3DVec3d getVectorFromSphereAlt(Point viewportCoordinates) {
        GL3DVec3d hp = rayTracer.cast(viewportCoordinates.getX(), viewportCoordinates.getY()).getHitpoint();
        return this.getLocalRotation().toMatrix().multiply(hp);
    }

    /**
     * Updates the camera transformation by applying the rotation and
     * translation information.
     */
    public void updateCameraTransformation() {
        this.rotation = this.currentDragRotation.copy();
        this.rotation.rotate(this.localRotation);
        cameraTransformation = this.rotation.toMatrix().translate(this.translation);
    }

    public GL3DMat4d getRotationMatrix() {
        return this.getRotation().toMatrix();
    }

    public void applyCamera(GL3DState state) {
        state.gl.glMultMatrixd(cameraTransformation.m, 0);
    }

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

    public final double getClipNear() {
        return clipNear;
    }

    public final double getClipFar() {
        return clipFar;
    }

    public double getAspect() {
        return aspect;
    }

    @Override
    public String toString() {
        return getName();
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

    public double getCameraWidth() {
        return cameraWidth;
    }

    public void zoomIn(int wr) {
        double currentWidth = this.getCameraWidth();
        double newWidth = currentWidth + wr * 0.1;
        double fov = 2. * Math.atan(-newWidth / this.getTranslation().z);
        this.setCameraFOV(fov);
        Displayer.display();
    }

}
