package org.helioviewer.jhv.display;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

public final class ViewportMath {

    private static final double PLANE_Z_EPS = 1e-8;

    private static double computeNormalizedX(Viewport vp, double screenX) {
        return (screenX - vp.x) / vp.width - 0.5;
    }

    private static double computeNormalizedY(Viewport vp, double screenY) {
        return 0.5 - (screenY - vp.yAWT) / vp.height;
    }

    private static double zoomedCameraWidth(Camera camera, Viewport vp) {
        // Mirror MapView.cameraWidth: the radial projections render at a normalized fit width, not the
        // orthographic R_sun FOV, so pan/zoom/LOD sensitivity must use the same width or it desyncs.
        double base = Display.mode.usesFitWidth() ? MapView.DISK_FIT_WIDTH : camera.baseCameraWidth();
        return base * vp.zoom;
    }

    private static double computeUpX(Camera camera, Viewport vp, double screenX) {
        return computeUpX(vp, zoomedCameraWidth(camera, vp), camera.getTranslationX(), screenX);
    }

    public static double computeUpX(Viewport vp, double width, double tx, double screenX) {
        return computeNormalizedX(vp, screenX) * width * vp.aspect - tx;
    }

    private static double computeUpY(Camera camera, Viewport vp, double screenY) {
        return computeUpY(vp, zoomedCameraWidth(camera, vp), camera.getTranslationY(), screenY);
    }

    public static double computeUpY(Viewport vp, double width, double ty, double screenY) {
        return computeNormalizedY(vp, screenY) * width - ty;
    }

    public static double getPixelFactor(Viewport vp, double width) {
        return vp.height / (width < 1 ? Math.cbrt(width) : width); // slow down zoomin of drawings
    }

    public static double getImagePixelFactor(Camera camera, Viewport vp) {
        return vp.height / zoomedCameraWidth(camera, vp);
    }

    public static double selectTrackballRadius2(Camera camera, Viewport vp, double screenX, double screenY) {
        double radius2 = screenPlaneRadius2(camera, vp, screenX, screenY);
        if (radius2 > 0.5 * Sun.Radius2) {
            double r = 0.5 * zoomedCameraWidth(camera, vp);
            return r * r;
        }
        return Sun.Radius2;
    }

    private static double screenPlaneRadius2(Camera camera, Viewport vp, double screenX, double screenY) {
        double upX = computeUpX(camera, vp, screenX);
        double upY = computeUpY(camera, vp, screenY);
        return upX * upX + upY * upY;
    }

    public static Quat calcTrackballDelta(Camera camera, Viewport vp, double startX, double startY, double endX, double endY, double refRadius2) {
        double width = zoomedCameraWidth(camera, vp);
        double tx = camera.getTranslationX();
        double ty = camera.getTranslationY();

        Quat dragRotation = camera.getDragRotation();
        Vec3 start = dragRotation.rotateInverseVector(trackballPoint(vp, width, tx, ty, startX, startY, refRadius2));
        Vec3 end = dragRotation.rotateInverseVector(trackballPoint(vp, width, tx, ty, endX, endY, refRadius2));
        return Quat.calcRotation(start, end);
    }

    private static Vec3 trackballPoint(Viewport vp, double width, double tx, double ty, double screenX, double screenY, double refRadius2) {
        double upX = computeUpX(vp, width, tx, screenX);
        double upY = computeUpY(vp, width, ty, screenY);
        double radius2 = upX * upX + upY * upY;
        return new Vec3(upX, upY, trackballZ(radius2, refRadius2));
    }

    private static double trackballZ(double radius2, double refRadius2) {
        double halfRadius2 = 0.5 * refRadius2;
        return radius2 <= halfRadius2 ? Math.sqrt(refRadius2 - radius2) : halfRadius2 / Math.sqrt(radius2);
    }

    @Nullable
    private static Vec3 intersectSphere(Viewport vp, double width, double tx, double ty, double screenX, double screenY) {
        double up1x = computeUpX(vp, width, tx, screenX);
        double up1y = computeUpY(vp, width, ty, screenY);
        double radius2 = up1x * up1x + up1y * up1y;
        return radius2 > Sun.Radius2 ? null : new Vec3(up1x, up1y, Math.sqrt(Sun.Radius2 - radius2));
    }

    @Nullable
    private static Vec3 intersectPlane(Viewport vp, double width, double tx, double ty, double screenX, double screenY, Vec3 planeNormal) {
        double denom = planeNormal.z;
        if (Math.abs(denom) < PLANE_Z_EPS)
            return null;

        double up1x = computeUpX(vp, width, tx, screenX);
        double up1y = computeUpY(vp, width, ty, screenY);
        double zvalue = -(planeNormal.x * up1x + planeNormal.y * up1y) / denom;
        return new Vec3(up1x, up1y, zvalue);
    }

    @Nullable
    static Vec3 unprojectToOutputSphere(Camera camera, Viewport vp, double width, double screenX, double screenY, Quat outputRotation) {
        Quat frameRotation = Quat.rotate(camera.getDragRotation(), outputRotation);
        Vec3 hitPoint = intersectSphere(vp, width, camera.getTranslationX(), camera.getTranslationY(), screenX, screenY);
        return hitPoint == null ? null : frameRotation.rotateInverseVector(hitPoint);
    }

    @Nullable
    static Vec3 unprojectToOutputPlane(Camera camera, Viewport vp, double width, double screenX, double screenY, Quat outputRotation) {
        Quat frameRotation = Quat.rotate(camera.getDragRotation(), outputRotation);
        Vec3 hitPoint = intersectPlane(vp, width, camera.getTranslationX(), camera.getTranslationY(), screenX, screenY, frameRotation.rotateVector(Vec3.ZAxis));
        return hitPoint == null ? null : frameRotation.rotateInverseVector(hitPoint);
    }

    @Nullable
    static Vec3 unprojectToCurrentViewSphereOrPlane(Camera camera, Viewport vp, double width, double x, double y) {
        Quat dragRotation = camera.getDragRotation();
        double tx = camera.getTranslationX();
        double ty = camera.getTranslationY();
        double upX = computeUpX(vp, width, tx, x);
        double upY = computeUpY(vp, width, ty, y);
        double radius2 = upX * upX + upY * upY;

        if (radius2 <= Sun.Radius2) {
            Vec3 hitPoint = new Vec3(upX, upY, Math.sqrt(Sun.Radius2 - radius2));
            Vec3 currentViewHitPoint = dragRotation.rotateInverseVector(hitPoint);
            if (currentViewHitPoint.z > 0.)
                return currentViewHitPoint;
        }

        Vec3 planeNormal = dragRotation.rotateVector(Vec3.ZAxis);
        double denom = planeNormal.z;
        if (Math.abs(denom) < PLANE_Z_EPS)
            return null;

        double zvalue = -(planeNormal.x * upX + planeNormal.y * upY) / denom;
        return dragRotation.rotateInverseVector(new Vec3(upX, upY, zvalue));
    }

    private ViewportMath() {}
}
