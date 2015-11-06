package org.helioviewer.jhv.camera;

import java.awt.Point;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.Mat4d;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.math.Vec2d;
import org.helioviewer.jhv.base.math.Vec3d;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;

import com.jogamp.opengl.GL2;

public abstract class GL3DCamera {

    public static final double INITFOV = (48. / 60.) * Math.PI / 180.;
    public static final double MIN_FOV = INITFOV * 0.02;
    public static final double MAX_FOV = INITFOV * 30;
    private static final double clipNear = Sun.Radius * 3;
    private static final double clipFar = Sun.Radius * 10000;
    private double fov = INITFOV;

    private Mat4d cameraTransformation = Mat4d.identity();

    private Quatd rotation = new Quatd();
    private Quatd currentDragRotation = new Quatd();
    protected Quatd localRotation = new Quatd();

    private Vec2d translation = new Vec2d();
    protected double distance = Sun.MeanEarthDistance;

    private boolean trackingMode;

    private Mat4d orthoMatrixInverse = Mat4d.identity();

    private double cameraWidth = 1;
    private double cameraWidthTimesAspect;
    private double previousAspect = -1.0;

    private double FOVangleToDraw;

    private final GL3DTrackballRotationInteraction rotationInteraction = new GL3DTrackballRotationInteraction(this);
    private final GL3DPanInteraction panInteraction = new GL3DPanInteraction(this);
    private final GL3DAnnotateInteraction annotateInteraction = new GL3DAnnotateInteraction(this);

    private GL3DInteraction currentInteraction = rotationInteraction;

    public void reset() {
        translation = new Vec2d(0, 0);
        currentDragRotation.clear();
        currentInteraction.reset();
        zoomToFit();
        timeChanged(Layers.getLastUpdatedTimestamp());
    }

    /**
     * This method is called when the camera changes and should copy the
     * required settings of the preceding camera objects.
     *
     * @param precedingCamera
     */
    public void activate(GL3DCamera precedingCamera) {
        if (precedingCamera != null) {
            rotation = precedingCamera.rotation.copy();
            translation = precedingCamera.translation.copy();
            FOVangleToDraw = precedingCamera.getFOVAngleToDraw();

            updateCameraWidthAspect(precedingCamera.previousAspect);

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
            reset();
        }
        timeChanged(Layers.getLastUpdatedTimestamp());
    }

    public GL3DCamera duplicate(JHVDate date) {
        if (!trackingMode) {
            try {
                GL3DCamera camera = this.getClass().newInstance();
                camera.fov = this.fov;
                camera.translation = this.translation.copy();
                camera.currentDragRotation = this.currentDragRotation.copy();
                camera.updateRotation(date);
                camera.updateCameraWidthAspect(this.previousAspect);

                return camera;
            } catch (Exception e) {
                return this;
            }
        } else
            return this;
    }

    public double getFOVAngleToDraw() {
        return this.FOVangleToDraw;
    }

    public void setPanning(Vec2d pan) {
        translation = pan;
    }

    public Vec2d getPanning() {
        return translation;
    }

    public double getDistance() {
        return distance;
    }

    public Quatd getLocalRotation() {
        return localRotation;
    }

    public void rotateCurrentDragRotation(Quatd _currentDragRotation) {
        currentDragRotation.rotate(_currentDragRotation);
        rotation.clear();
        updateCameraTransformation();
    }

    public void updateCameraWidthAspect(double aspect) {
        cameraWidth = distance * Math.tan(0.5 * fov);
        previousAspect = aspect;
        cameraWidthTimesAspect = cameraWidth * aspect;
    }

    public Mat4d getOrthoMatrixInverse() {
        return Mat4d.orthoInverse(-cameraWidthTimesAspect, cameraWidthTimesAspect, -cameraWidth, cameraWidth, clipNear, clipFar);
    }

    public void applyPerspective(GL2 gl) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(-cameraWidthTimesAspect, cameraWidthTimesAspect, -cameraWidth, cameraWidth, clipNear, clipFar);

        // applyCamera
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadMatrixd(cameraTransformation.m, 0);
    }

    public Vec3d getVectorFromSphereOrPlane(Vec2d normalizedScreenpos, Quatd cameraDifferenceRotation) {
        double up1x = normalizedScreenpos.x * cameraWidthTimesAspect - translation.x;
        double up1y = normalizedScreenpos.y * cameraWidth - translation.y;

        Vec3d hitPoint;
        Vec3d rotatedHitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 1) {
            hitPoint = new Vec3d(up1x, up1y, Math.sqrt(1. - radius2));
            rotatedHitPoint = cameraDifferenceRotation.rotateInverseVector(hitPoint);
            if (rotatedHitPoint.z > 0.) {
                return rotatedHitPoint;
            }
        }
        Vec3d altnormal = cameraDifferenceRotation.rotateVector(Vec3d.ZAxis);
        double zvalue = -(altnormal.x * up1x + altnormal.y * up1y) / altnormal.z;
        hitPoint = new Vec3d(up1x, up1y, zvalue);

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

    public Vec3d getVectorFromSphere(Point viewportCoordinates) {
        Vec3d hitPoint = getVectorFromSphereAlt(viewportCoordinates);
        if (hitPoint != null) {
            return localRotation.rotateInverseVector(hitPoint);
        }
        return null;
    }

    public Vec3d getVectorFromPlane(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);
        Vec3d altnormal = currentDragRotation.rotateVector(Vec3d.ZAxis);
        if (altnormal.z == 0) {
            return null;
        }
        double zvalue = -(altnormal.x * up1x + altnormal.y * up1y) / altnormal.z;

        Vec3d hitPoint = new Vec3d(up1x, up1y, zvalue);
        return currentDragRotation.rotateInverseVector(hitPoint);
    }

    public Vec3d getVectorFromSphereAlt(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);

        Vec3d hitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 1.) {
            hitPoint = new Vec3d(up1x, up1y, Math.sqrt(1. - radius2));
            return currentDragRotation.rotateInverseVector(hitPoint);
        }
        return null;
    }

    public double getRadiusFromSphereAlt(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);

        return Math.sqrt(up1x * up1x + up1y * up1y);
    }

    public Vec3d getVectorFromSphereTrackball(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);
        Vec3d hitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 0.5 * Sun.Radius2) {
            hitPoint = new Vec3d(up1x, up1y, Math.sqrt(Sun.Radius2 - radius2));
        } else {
            hitPoint = new Vec3d(up1x, up1y, 0.5 * Sun.Radius2 / Math.sqrt(radius2));
        }
        return currentDragRotation.rotateInverseVector(hitPoint);
    }

    public Quatd getCameraDifferenceRotationQuatd(Quatd rot) {
        Quatd cameraDifferenceRotation = rotation.copy();
        cameraDifferenceRotation.rotateWithConjugate(rot);

        return cameraDifferenceRotation;
    }

    /**
     * Updates the camera transformation by applying the rotation and
     * translation information.
     */
    protected void updateCameraTransformation() {
        rotation = currentDragRotation.copy();
        rotation.rotate(localRotation);
        cameraTransformation = rotation.toMatrix().translate(translation.x, translation.y, -distance);
    }

    public void setCameraFOV(double _fov) {
        if (_fov < MIN_FOV) {
            fov = MIN_FOV;
        } else if (_fov > MAX_FOV) {
            fov = MAX_FOV;
        } else {
            fov = _fov;
        }
    }

    public void setTrackingMode(boolean trackingMode) {
        this.trackingMode = trackingMode;
    }

    public boolean getTrackingMode() {
        return trackingMode;
    }

    public double getCameraWidth() {
        return cameraWidth;
    }

    public void zoom(int wr) {
        setCameraFOV(2. * Math.atan2(cameraWidth * (1 + 0.015 * wr), distance));
    }

    public void setFOVangleDegrees(double fovAngle) {
        FOVangleToDraw = fovAngle * Math.PI / 180.;
    }

    public void setCurrentInteraction(GL3DInteraction _currentInteraction) {
        currentInteraction = _currentInteraction;
    }

    public GL3DInteraction getCurrentInteraction() {
        return currentInteraction;
    }

    public GL3DInteraction getPanInteraction() {
        return panInteraction;
    }

    public GL3DInteraction getRotateInteraction() {
        return rotationInteraction;
    }

    public GL3DAnnotateInteraction getAnnotateInteraction() {
        return annotateInteraction;
    }

    public abstract GL3DCameraOptionPanel getOptionPanel();

    public abstract void timeChanged(JHVDate date);

    public abstract void updateRotation(JHVDate date);

    public void zoomToFit() {
        double size = Layers.getLargestPhysicalSize();
        if (size == 0)
            setCameraFOV(INITFOV);
        else
            setCameraFOV(2. * Math.atan2(0.5 * size, distance));
    }

    public Mat4d getRotation() {
        return rotation.toMatrix();
    }

}
