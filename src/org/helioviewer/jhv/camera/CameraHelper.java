package org.helioviewer.jhv.camera;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.ImageLayerBounds;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

public class CameraHelper {

    private static final double PLANE_Z_EPS = 1e-8;

    private static double computeNormalizedX(Viewport vp, double screenX) {
        return (screenX - vp.x) / vp.width - 0.5;
    }

    private static double computeNormalizedY(Viewport vp, double screenY) {
        return 0.5 - (screenY - vp.yAWT) / vp.height;
    }

    public static double computeUpX(Camera camera, Viewport vp, double screenX) {
        double width = camera.getCameraWidth();
        return computeNormalizedX(vp, screenX) * width * vp.aspect - camera.getTranslationX();
    }

    public static double computeUpY(Camera camera, Viewport vp, double screenY) {
        double width = camera.getCameraWidth();
        return computeNormalizedY(vp, screenY) * width - camera.getTranslationY();
    }

    public static double getPixelFactor(Camera camera, Viewport vp) {
        double width = camera.getCameraWidth();
        return vp.height / (width < 1 ? Math.cbrt(width) : width); // slow down zoomin of drawings
    }

    public static double getImagePixelFactor(Camera camera, Viewport vp) {
        return vp.height / camera.getCameraWidth();
    }

    static double selectTrackballRadius2(Camera camera, Viewport vp, double screenX, double screenY) {
        double up1x = computeUpX(camera, vp, screenX);
        double up1y = computeUpY(camera, vp, screenY);
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 > 0.5 * Sun.Radius2) {
            double r = 0.5 * camera.getCameraWidth();
            return r * r;
        }
        return Sun.Radius2;
    }

    static Quat calcTrackballDelta(Camera camera, Viewport vp, double startX, double startY, double endX, double endY, double refRadius2) {
        double width = camera.getCameraWidth();
        double tx = camera.getTranslationX();
        double ty = camera.getTranslationY();
        double widthAspect = width * vp.aspect;
        double halfRadius2 = 0.5 * refRadius2;

        double startUpX = ((startX - vp.x) / vp.width - 0.5) * widthAspect - tx;
        double startUpY = (0.5 - (startY - vp.yAWT) / vp.height) * width - ty;
        double startRadius2 = startUpX * startUpX + startUpY * startUpY;
        double startZ = startRadius2 <= halfRadius2 ? Math.sqrt(refRadius2 - startRadius2) : halfRadius2 / Math.sqrt(startRadius2);

        double endUpX = ((endX - vp.x) / vp.width - 0.5) * widthAspect - tx;
        double endUpY = (0.5 - (endY - vp.yAWT) / vp.height) * width - ty;
        double endRadius2 = endUpX * endUpX + endUpY * endUpY;
        double endZ = endRadius2 <= halfRadius2 ? Math.sqrt(refRadius2 - endRadius2) : halfRadius2 / Math.sqrt(endRadius2);

        Quat dragRotation = camera.getDragRotation();
        Vec3 start = dragRotation.rotateInverseVector(new Vec3(startUpX, startUpY, startZ));
        Vec3 end = dragRotation.rotateInverseVector(new Vec3(endUpX, endUpY, endZ));
        return Quat.calcRotation(start, end);
    }

    @Nullable
    private static Vec3 intersectSphere(Camera camera, Viewport vp, double screenX, double screenY) {
        double up1x = computeUpX(camera, vp, screenX);
        double up1y = computeUpY(camera, vp, screenY);
        double radius2 = up1x * up1x + up1y * up1y;
        return radius2 > Sun.Radius2 ? null : new Vec3(up1x, up1y, Math.sqrt(Sun.Radius2 - radius2));
    }

    @Nullable
    private static Vec3 intersectPlane(Camera camera, Viewport vp, double screenX, double screenY, Vec3 planeNormal) {
        double denom = planeNormal.z;
        if (Math.abs(denom) < PLANE_Z_EPS)
            return null;

        double up1x = computeUpX(camera, vp, screenX);
        double up1y = computeUpY(camera, vp, screenY);
        double zvalue = -(planeNormal.x * up1x + planeNormal.y * up1y) / denom;
        return new Vec3(up1x, up1y, zvalue);
    }

    @Nullable
    public static Vec3 unprojectToOutputSphere(Camera camera, Viewport vp, double screenX, double screenY, Quat outputRotation) {
        Quat dragRotation = camera.getDragRotation();
        Quat frameRotation = Quat.rotate(dragRotation, outputRotation);
        Vec3 hitPoint = intersectSphere(camera, vp, screenX, screenY);
        return hitPoint == null ? null : frameRotation.rotateInverseVector(hitPoint);
    }

    @Nullable
    public static Vec3 unprojectToOutputPlane(Camera camera, Viewport vp, double screenX, double screenY, Quat outputRotation) {
        Quat dragRotation = camera.getDragRotation();
        Quat frameRotation = Quat.rotate(dragRotation, outputRotation);
        Vec3 hitPoint = intersectPlane(camera, vp, screenX, screenY, frameRotation.rotateVector(Vec3.ZAxis));
        return hitPoint == null ? null : frameRotation.rotateInverseVector(hitPoint);
    }

    @Nullable
    public static Vec3 unprojectToCurrentViewSphereOrPlane(Camera camera, Viewport vp, double x, double y) {
        Quat dragRotation = camera.getDragRotation();
        Vec3 hitPoint = intersectSphere(camera, vp, x, y);
        if (hitPoint != null) {
            Vec3 currentViewHitPoint = dragRotation.rotateInverseVector(hitPoint);
            if (currentViewHitPoint.z > 0.)
                return currentViewHitPoint;
        }

        hitPoint = intersectPlane(camera, vp, x, y, dragRotation.rotateVector(Vec3.ZAxis));
        return hitPoint == null ? null : dragRotation.rotateInverseVector(hitPoint);
    }

    public static void zoomToFit(Camera camera) {
        double size = ImageLayerBounds.getLargestPhysicalHeight();
        double newFOV = Camera.INITFOV;
        if (size != 0)
            newFOV = 2. * Math.atan2(0.5 * size, camera.getViewpoint().distance);
        camera.setFOV(newFOV);
    }

}
