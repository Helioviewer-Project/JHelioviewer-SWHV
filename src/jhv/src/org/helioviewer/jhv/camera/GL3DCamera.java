package org.helioviewer.jhv.camera;

import java.awt.Point;
import java.util.Date;

import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.GL3DMat4d;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.viewmodel.metadata.MetaData;

import com.jogamp.opengl.GL2;

public abstract class GL3DCamera {

    public static final double INITFOV = (48. / 60.) * Math.PI / 180.;
    public static final double MIN_FOV = INITFOV * 0.02;
    public static final double MAX_FOV = INITFOV * 30;
    private static final double clipNear = Sun.Radius * 3;
    private static final double clipFar = Sun.Radius * 10000;
    private double fov = INITFOV;

    private double previousAspect = -1.0;

    private GL3DMat4d cameraTransformation;

    private GL3DQuatd rotation;
    private GL3DVec3d translation;

    private final GL3DQuatd currentDragRotation;

    protected GL3DQuatd localRotation;

    private boolean trackingMode;

    private GL3DMat4d orthoMatrixInverse = GL3DMat4d.identity();

    private double cameraWidth = 1.;
    private double previousCameraWidth = -1;

    private double cameraWidthTimesAspect;

    private double FOVangleToDraw;

    private final GL3DTrackballRotationInteraction rotationInteraction;
    private final GL3DPanInteraction panInteraction;
    private final GL3DAnnotateInteraction annotateInteraction;

    private GL3DInteraction currentInteraction;

    public GL3DCamera() {
        this.cameraTransformation = GL3DMat4d.identity();
        this.rotation = new GL3DQuatd();
        this.currentDragRotation = new GL3DQuatd();
        this.localRotation = new GL3DQuatd();
        this.translation = new GL3DVec3d();
        this.fov = INITFOV;
        this.rotationInteraction = new GL3DTrackballRotationInteraction(this);
        this.panInteraction = new GL3DPanInteraction(this);
        this.annotateInteraction = new GL3DAnnotateInteraction(this);
        this.currentInteraction = this.rotationInteraction;
    }

    public void reset() {
        this.translation = new GL3DVec3d(0, 0, this.translation.z);
        this.currentDragRotation.clear();
        this.currentInteraction.reset();
        zoomToFit();
    }

    /**
     * This method is called when the camera changes and should copy the
     * required settings of the preceding camera objects.
     *
     * @param precedingCamera
     */
    public void activate(GL3DCamera precedingCamera) {
        if (precedingCamera != null) {
            this.rotation = precedingCamera.rotation.copy();
            this.translation = precedingCamera.translation.copy();
            this.FOVangleToDraw = precedingCamera.getFOVAngleToDraw();

            this.updateCameraTransformation();
            this.updateCameraWidthAspect(precedingCamera.previousAspect);

            GL3DInteraction precedingInteraction = precedingCamera.getCurrentInteraction();
            if (precedingInteraction.equals(precedingCamera.getRotateInteraction())) {
                this.setCurrentInteraction(this.getRotateInteraction());
            } else if (precedingInteraction.equals(precedingCamera.getPanInteraction())) {
                this.setCurrentInteraction(this.getPanInteraction());
            } else if (precedingInteraction.equals(precedingCamera.getAnnotateInteraction())) {
                this.setCurrentInteraction(this.getAnnotateInteraction());
            }
        } else {
            Log.debug("GL3DCamera: No Preceding Camera, resetting Camera");
            this.reset();
        }
    }

    private GL3DQuatd saveRotation;
    private GL3DQuatd saveLocalRotation;
    private GL3DVec3d saveTranslation;
    private GL3DMat4d saveTransformation;

    public void push(Date date, MetaData m) {
        if (!trackingMode) {
            saveRotation = rotation.copy();
            saveLocalRotation = localRotation.copy();
            saveTranslation = translation.copy();
            saveTransformation = cameraTransformation.copy();
            updateRotation(date, m);
        }
    }

    public void pop() {
        if (!trackingMode) {
            rotation = saveRotation;
            localRotation = saveLocalRotation;
            translation = saveTranslation;
            cameraTransformation = saveTransformation;
        }
    }

    public double getFOVAngleToDraw() {
        return this.FOVangleToDraw;
    }

    public void setPanning(double x, double y) {
        translation.x = x;
        translation.y = y;
    }

    protected void setZTranslation(double z) {
        translation.z = z;
        updateCameraWidthAspect(previousAspect);
    }

    public double getZTranslation() {
        return this.translation.z;
    }

    public GL3DVec3d getTranslation() {
        return this.translation;
    }

    public GL3DQuatd getLocalRotation() {
        return this.localRotation;
    }

    public void resetCurrentDragRotation() {
        this.currentDragRotation.clear();
    }

    public void setLocalRotation(GL3DQuatd localRotation) {
        this.localRotation = localRotation;
        this.rotation.clear();
        this.updateCameraTransformation();
    }

    public void rotateCurrentDragRotation(GL3DQuatd currentDragRotation) {
        this.currentDragRotation.rotate(currentDragRotation);
        this.rotation.clear();
        this.updateCameraTransformation();
    }

    // quantization bits per half width camera
    private static final int quantFactor = 1 << 12;

    public void updateCameraWidthAspect(double aspect) {
        cameraWidth = -translation.z * Math.tan(fov / 2.);
        if (cameraWidth == 0.)
            cameraWidth = 1.;

        cameraWidth = (long) (cameraWidth * quantFactor) / (double) quantFactor;
        if (cameraWidth == previousCameraWidth && aspect == previousAspect) {
            return;
        }

        previousCameraWidth = cameraWidth;
        previousAspect = aspect;
        cameraWidthTimesAspect = cameraWidth * aspect;

        //orthoMatrix = GL3DMat4d.ortho(-cameraWidthTimesAspect, cameraWidthTimesAspect, -cameraWidth, cameraWidth, clipNear, clipFar);
        orthoMatrixInverse = GL3DMat4d.orthoInverse(-cameraWidthTimesAspect, cameraWidthTimesAspect, -cameraWidth, cameraWidth, clipNear, clipFar);

        if (this == Displayer.getViewport().getCamera()) {
            // Displayer.render();
            ImageViewerGui.getZoomStatusPanel().updateZoomLevel(cameraWidth);
        }
    }

    public GL3DMat4d getOrthoMatrixInverse() {
        return orthoMatrixInverse.copy();
    }

    public void applyPerspective(GL2 gl) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(-cameraWidthTimesAspect, cameraWidthTimesAspect, -cameraWidth, cameraWidth, clipNear, clipFar);

        // applyCamera
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadMatrixd(cameraTransformation.m, 0);
    }

    public GL3DVec3d getVectorFromSphereOrPlane(GL3DVec2d normalizedScreenpos, GL3DQuatd cameraDifferenceRotation) {
        double up1x = normalizedScreenpos.x * cameraWidthTimesAspect - translation.x;
        double up1y = normalizedScreenpos.y * cameraWidth - translation.y;

        GL3DVec3d hitPoint;
        GL3DVec3d rotatedHitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 1) {
            hitPoint = new GL3DVec3d(up1x, up1y, Math.sqrt(1. - radius2));
            rotatedHitPoint = cameraDifferenceRotation.rotateInverseVector(hitPoint);
            if (rotatedHitPoint.z > 0.) {
                return rotatedHitPoint;
            }
        }
        GL3DVec3d altnormal = cameraDifferenceRotation.rotateVector(GL3DVec3d.ZAxis);
        double zvalue = -(altnormal.x * up1x + altnormal.y * up1y) / altnormal.z;
        hitPoint = new GL3DVec3d(up1x, up1y, zvalue);

        return cameraDifferenceRotation.rotateInverseVector(hitPoint);
    }

    private static double computeNormalizedX(Point viewportCoordinates) {
        return +2. * ((viewportCoordinates.getX() - Displayer.getViewport().getOffsetX()) / Displayer.getViewport().getWidth() - 0.5);
    }

    private static double computeNormalizedY(Point viewportCoordinates) {
        return -2. * ((viewportCoordinates.getY() - Displayer.getViewport().getOffsetY()) / Displayer.getViewport().getHeight() - 0.5);
    }

    private double computeUpX(Point viewportCoordinates) {
        return computeNormalizedX(viewportCoordinates) * cameraWidthTimesAspect - translation.x;
    }

    private double computeUpY(Point viewportCoordinates) {
        return computeNormalizedY(viewportCoordinates) * cameraWidth - translation.y;
    }

    public GL3DVec3d getVectorFromSphere(Point viewportCoordinates) {
        GL3DVec3d hitPoint = getVectorFromSphereAlt(viewportCoordinates);
        if (hitPoint != null) {
            return localRotation.rotateInverseVector(hitPoint);
        }
        return null;
    }

    public GL3DVec3d getVectorFromPlane(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);
        GL3DVec3d altnormal = currentDragRotation.rotateVector(GL3DVec3d.ZAxis);
        if (altnormal.z == 0) {
            return null;
        }
        double zvalue = -(altnormal.x * up1x + altnormal.y * up1y) / altnormal.z;

        GL3DVec3d hitPoint = new GL3DVec3d(up1x, up1y, zvalue);
        return currentDragRotation.rotateInverseVector(hitPoint);
    }

    public GL3DVec3d getVectorFromSphereAlt(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);

        GL3DVec3d hitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 1.) {
            hitPoint = new GL3DVec3d(up1x, up1y, Math.sqrt(1. - radius2));
            return currentDragRotation.rotateInverseVector(hitPoint);
        }
        return null;
    }

    public double getRadiusFromSphereAlt(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);

        return Math.sqrt(up1x * up1x + up1y * up1y);
    }

    public GL3DVec3d getVectorFromSphereTrackball(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);
        GL3DVec3d hitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= Sun.Radius2 / 2.) {
            hitPoint = new GL3DVec3d(up1x, up1y, Math.sqrt(Sun.Radius2 - radius2));
        } else {
            hitPoint = new GL3DVec3d(up1x, up1y, Sun.Radius2 / (2. * Math.sqrt(radius2)));
        }
        return currentDragRotation.rotateInverseVector(hitPoint);
    }

    public GL3DQuatd getCameraDifferenceRotationQuatd(GL3DQuatd rot) {
        GL3DQuatd cameraDifferenceRotation = rotation.copy();
        cameraDifferenceRotation.rotateWithConjugate(rot);

        return cameraDifferenceRotation;
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

    public double getCameraFOV() {
        return fov;
    }

    public void setCameraFOV(double fov) {
        if (fov < MIN_FOV) {
            this.fov = MIN_FOV;
        } else if (fov > MAX_FOV) {
            this.fov = MAX_FOV;
        } else {
            this.fov = fov;
        }
        updateCameraWidthAspect(previousAspect);
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setTrackingMode(boolean trackingMode) {
        this.trackingMode = trackingMode;
    }

    public boolean getTrackingMode() {
        return trackingMode;
    }

    public void deactivate() {
    }

    public double getCameraWidth() {
        return cameraWidth;
    }

    public void zoom(int wr) {
        setCameraFOV(fov + 0.0005 * wr);
    }

    public void setFOVangleDegrees(double fovAngle) {
        this.FOVangleToDraw = fovAngle * Math.PI / 180.0;
    }

    public void setCurrentInteraction(GL3DInteraction currentInteraction) {
        this.currentInteraction = currentInteraction;
    }

    public GL3DInteraction getCurrentInteraction() {
        return this.currentInteraction;
    }

    public GL3DInteraction getPanInteraction() {
        return this.panInteraction;
    }

    public GL3DInteraction getRotateInteraction() {
        return this.rotationInteraction;
    }

    public GL3DInteraction getAnnotateInteraction() {
        return this.annotateInteraction;
    }

    public abstract String getName();

    public abstract GL3DCameraOptionPanel getOptionPanel();

    public abstract void timeChanged(Date date);

    public abstract void updateRotation(Date date, MetaData m);

    public void zoomToFit() {
        double size = Layers.getLargestPhysicalSize();

        if (size == 0)
            setCameraFOV(INITFOV);
        else
            setCameraFOV(2. * Math.atan(-size / 2. / this.getZTranslation()));
    }

    public GL3DMat4d getRotation() {
        return this.rotation.toMatrix();
    }

}
