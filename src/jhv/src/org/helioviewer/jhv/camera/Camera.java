package org.helioviewer.jhv.camera;

import java.awt.Point;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.Mat4d;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2d;
import org.helioviewer.jhv.base.math.Vec3d;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableCamera;

import com.jogamp.opengl.GL2;

public class Camera {

    protected static enum CameraMode {
        OBSERVER, EARTH, EXPERT
    }

    private static final double INITFOV = (48. / 60.) * Math.PI / 180.;
    private static final double MIN_FOV = INITFOV * 0.02;
    private static final double MAX_FOV = INITFOV * 30;
    private static final double clipNear = Sun.Radius * 3;
    private static final double clipFar = Sun.Radius * 10000;
    private double fov = INITFOV;

    private Mat4d cameraTransformation = Mat4d.identity();

    private Quat rotation = new Quat();

    private Quat currentDragRotation = new Quat();
    private Vec2d currentTranslation = new Vec2d();

    private boolean trackingMode;

    private double cameraWidth = 1;
    private double cameraWidthTimesAspect;
    private double previousAspect = -1.0;

    private double FOVangleToDraw;

    private final InteractionRotate rotationInteraction = new InteractionRotate(this);
    private final InteractionPan panInteraction = new InteractionPan(this);
    private final InteractionAnnotate annotateInteraction = new InteractionAnnotate(this);
    private Interaction currentInteraction = rotationInteraction;

    private final VantagePointObserver vantagePointObserver = new VantagePointObserver();
    private final VantagePointEarth vantagePointEarth = new VantagePointEarth();
    private final VantagePointExpert vantagePointExpert = new VantagePointExpert();
    private VantagePoint vantagePoint = vantagePointObserver;

    private CameraMode mode = CameraMode.OBSERVER;

    protected void setMode(CameraMode _mode) {
        mode = _mode;
        switch (mode) {
            case EXPERT:
                vantagePoint = vantagePointExpert;
            break;
            case EARTH:
                vantagePoint = vantagePointEarth;
            break;
            default:
                vantagePoint = vantagePointObserver;
        }
    }

    public void reset() {
        currentTranslation = new Vec2d(0, 0);
        currentDragRotation.clear();
        currentInteraction.reset();
        zoomToFit();
        timeChanged(Layers.getLastUpdatedTimestamp());
    }

    public Camera duplicate(JHVDate date) {
        if (!trackingMode) {
            try {
                Camera camera = new Camera();
                camera.setMode(mode);
                camera.fov = fov;
                camera.currentTranslation = currentTranslation.copy();
                camera.currentDragRotation = currentDragRotation.copy();
                camera.updateCameraWidthAspect(previousAspect);
                camera.vantagePoint.update(date);
                camera.updateCameraTransformation();

                return camera;
            } catch (Exception e) {
                return this;
            }
        } else
            return this;
    }

    public double getFOVAngleToDraw() {
        return FOVangleToDraw;
    }

    public void setPanning(Vec2d pan) {
        currentTranslation = pan;
    }

    public Vec2d getPanning() {
        return currentTranslation;
    }

    public double getDistance() {
        return vantagePoint.distance;
    }

    public Quat getOrientation() {
        return vantagePoint.orientation;
    }

    public void rotateCurrentDragRotation(Quat _currentDragRotation) {
        currentDragRotation.rotate(_currentDragRotation);
        rotation.clear();
        updateCameraTransformation();
    }

    public void updateCameraWidthAspect(double aspect) {
        cameraWidth = vantagePoint.distance * Math.tan(0.5 * fov);
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

    public Vec3d getVectorFromSphereOrPlane(Vec2d normalizedScreenpos, Quat cameraDifferenceRotation) {
        double up1x = normalizedScreenpos.x * cameraWidthTimesAspect - currentTranslation.x;
        double up1y = normalizedScreenpos.y * cameraWidth - currentTranslation.y;

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
        return computeNormalizedX(viewportCoordinates) * cameraWidthTimesAspect - currentTranslation.x;
    }

    private double computeUpY(Point viewportCoordinates) {
        return computeNormalizedY(viewportCoordinates) * cameraWidth - currentTranslation.y;
    }

    public Vec3d getVectorFromSphere(Point viewportCoordinates) {
        Vec3d hitPoint = getVectorFromSphereAlt(viewportCoordinates);
        if (hitPoint != null) {
            return vantagePoint.orientation.rotateInverseVector(hitPoint);
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

    public Quat getCameraDifferenceRotationQuat(Quat rot) {
        Quat cameraDifferenceRotation = rotation.copy();
        cameraDifferenceRotation.rotateWithConjugate(rot);

        return cameraDifferenceRotation;
    }

    /**
     * Updates the camera transformation by applying the rotation and
     * translation information.
     */
    protected void updateCameraTransformation() {
        rotation = currentDragRotation.copy();
        rotation.rotate(vantagePoint.orientation);
        cameraTransformation = rotation.toMatrix().translate(currentTranslation.x, currentTranslation.y, -vantagePoint.distance);
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

    public void setTrackingMode(boolean _trackingMode) {
        trackingMode = _trackingMode;
    }

    public boolean getTrackingMode() {
        return trackingMode;
    }

    public double getCameraWidth() {
        return cameraWidth;
    }

    public void zoom(int wr) {
        setCameraFOV(2. * Math.atan2(cameraWidth * (1 + 0.015 * wr), vantagePoint.distance));
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

    private CameraOptionPanel optionPanel = new CameraOptionPanel();
    protected CameraOptionPanelExpert expertOptionPanel;

    public CameraOptionPanel getOptionPanel() {
        if (mode == CameraMode.EXPERT) {
            return expertOptionPanel;
        } else {
            return optionPanel;
        }
    }

    public void timeChanged(JHVDate date) {
        if (!trackingMode) {
            vantagePoint.update(date);
            updateCameraTransformation();
        } else {
            Displayer.render();
        }

        RenderableCamera renderableCamera = ImageViewerGui.getRenderableCamera();
        if (renderableCamera != null) {
            renderableCamera.setTimeString(vantagePoint.time.toString());
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(renderableCamera);
        }
    }

    public void zoomToFit() {
        double size = Layers.getLargestPhysicalSize();
        if (size == 0)
            setCameraFOV(INITFOV);
        else
            setCameraFOV(2. * Math.atan2(0.5 * size, vantagePoint.distance));
    }

    public Mat4d getRotation() {
        return rotation.toMatrix();
    }

}
