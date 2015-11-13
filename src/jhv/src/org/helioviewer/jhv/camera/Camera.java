package org.helioviewer.jhv.camera;

import java.awt.Point;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableCamera;

import com.jogamp.opengl.GL2;

public class Camera {

    static enum CameraMode {
        OBSERVER, EARTH, EXPERT
    }

    private static final double INITFOV = (48. / 60.) * Math.PI / 180.;
    private static final double MIN_FOV = INITFOV * 0.1;
    private static final double MAX_FOV = INITFOV * 30;
    private static final double clipNear = Sun.Radius * 3;
    private static final double clipFar = Sun.Radius * 10000;
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

    private double cameraAspect = 1; // tbd

    public void setAspect(double aspect) {
        cameraAspect = aspect;
    }

    private void updateWidth() {
        cameraWidth = viewpoint.distance * Math.tan(0.5 * fov);
    }

    public Mat4 getOrthoMatrixInverse(double aspect) {
        return Mat4.orthoInverse(-cameraWidth * aspect, cameraWidth * aspect, -cameraWidth, cameraWidth, clipNear, clipFar);
    }

    public void applyPerspective(GL2 gl, double aspect) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(-cameraWidth * aspect, cameraWidth * aspect, -cameraWidth, cameraWidth, clipNear, clipFar);

        Mat4 cameraTransformation = rotation.toMatrix().translate(currentTranslation.x, currentTranslation.y, -viewpoint.distance);
        // applyCamera
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadMatrixd(cameraTransformation.m, 0);
    }

    public Vec3 getVectorFromSphereOrPlane(Vec2 normalizedScreenpos, Quat cameraDifferenceRotation) {
        double up1x = normalizedScreenpos.x * cameraWidth * cameraAspect - currentTranslation.x;
        double up1y = normalizedScreenpos.y * cameraWidth - currentTranslation.y;

        Vec3 hitPoint;
        Vec3 rotatedHitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 1) {
            hitPoint = new Vec3(up1x, up1y, Math.sqrt(1. - radius2));
            rotatedHitPoint = cameraDifferenceRotation.rotateInverseVector(hitPoint);
            if (rotatedHitPoint.z > 0.) {
                return rotatedHitPoint;
            }
        }
        Vec3 altnormal = cameraDifferenceRotation.rotateVector(Vec3.ZAxis);
        double zvalue = -(altnormal.x * up1x + altnormal.y * up1y) / altnormal.z;
        hitPoint = new Vec3(up1x, up1y, zvalue);

        return cameraDifferenceRotation.rotateInverseVector(hitPoint);
    }

    private static double computeNormalizedX(Point viewportCoordinates) {
        return +2. * ((viewportCoordinates.getX() - Displayer.getViewport().getOffsetX()) / Displayer.getViewport().getWidth() - 0.5);
    }

    private static double computeNormalizedY(Point viewportCoordinates) {
        return -2. * ((viewportCoordinates.getY() - Displayer.getViewport().getOffsetY()) / Displayer.getViewport().getHeight() - 0.5);
    }

    private double computeUpX(Point viewportCoordinates) {
        return computeNormalizedX(viewportCoordinates) * cameraWidth * cameraAspect - currentTranslation.x;
    }

    private double computeUpY(Point viewportCoordinates) {
        return computeNormalizedY(viewportCoordinates) * cameraWidth - currentTranslation.y;
    }

    public Vec3 getVectorFromSphere(Point viewportCoordinates) {
        Vec3 hitPoint = getVectorFromSphereAlt(viewportCoordinates);
        if (hitPoint != null) {
            return viewpoint.orientation.rotateInverseVector(hitPoint);
        }
        return null;
    }

    public Vec3 getVectorFromPlane(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);
        Vec3 altnormal = currentDragRotation.rotateVector(Vec3.ZAxis);
        if (altnormal.z == 0) {
            return null;
        }
        double zvalue = -(altnormal.x * up1x + altnormal.y * up1y) / altnormal.z;

        Vec3 hitPoint = new Vec3(up1x, up1y, zvalue);
        return currentDragRotation.rotateInverseVector(hitPoint);
    }

    public Vec3 getVectorFromSphereAlt(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);

        Vec3 hitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 1.) {
            hitPoint = new Vec3(up1x, up1y, Math.sqrt(1. - radius2));
            return currentDragRotation.rotateInverseVector(hitPoint);
        }
        return null;
    }

    public double getRadiusFromSphereAlt(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);

        return Math.sqrt(up1x * up1x + up1y * up1y);
    }

    public Vec3 getVectorFromSphereTrackball(Point viewportCoordinates) {
        double up1x = computeUpX(viewportCoordinates);
        double up1y = computeUpY(viewportCoordinates);
        Vec3 hitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 0.5 * Sun.Radius2) {
            hitPoint = new Vec3(up1x, up1y, Math.sqrt(Sun.Radius2 - radius2));
        } else {
            hitPoint = new Vec3(up1x, up1y, 0.5 * Sun.Radius2 / Math.sqrt(radius2));
        }
        return currentDragRotation.rotateInverseVector(hitPoint);
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

    void rotateCurrentDragRotation(Quat _currentDragRotation) {
        currentDragRotation.rotate(_currentDragRotation);
        updateTransformation();
    }

    private void updateTransformation() {
        rotation = currentDragRotation.copy();
        rotation.rotate(viewpoint.orientation);
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

    public double getCameraWidth() {
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
