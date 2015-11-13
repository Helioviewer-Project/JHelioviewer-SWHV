package org.helioviewer.jhv.camera;

import java.awt.Point;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;

import com.jogamp.opengl.GL2;

public class CameraHelper {

    private static final double clipNear = Sun.Radius * 3;
    private static final double clipFar = Sun.Radius * 10000;

    public static Mat4 getOrthoMatrixInverse(Camera camera, Viewport vp) {
        double width = camera.getWidth();
        double aspect = vp.getWidth() / (double) vp.getHeight();
        return Mat4.orthoInverse(-width * aspect, width * aspect, -width, width, clipNear, clipFar);
    }

    public static void applyPerspective(Camera camera, Viewport vp, GL2 gl) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        double width = camera.getWidth();
        double aspect = vp.getWidth() / (double) vp.getHeight();
        gl.glOrtho(-width * aspect, width * aspect, -width, width, clipNear, clipFar);

        Vec2 translation = camera.getCurrentTranslation();
        Mat4 cameraTransformation = camera.getRotation().translate(translation.x, translation.y, -camera.getDistance());
        // applyCamera
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadMatrixd(cameraTransformation.m, 0);
    }

    private static double computeNormalizedX(Viewport vp, Point viewportCoordinates) {
        return +2. * ((viewportCoordinates.getX() - vp.getOffsetX()) / vp.getWidth() - 0.5);
    }

    private static double computeNormalizedY(Viewport vp, Point viewportCoordinates) {
        return -2. * ((viewportCoordinates.getY() - vp.getOffsetY()) / vp.getHeight() - 0.5);
    }

    private static double computeUpX(Camera camera, Viewport vp, Point viewportCoordinates) {
        double width = camera.getWidth();
        double aspect = vp.getWidth() / (double) vp.getHeight();
        Vec2 translation = camera.getCurrentTranslation();
        return computeNormalizedX(vp, viewportCoordinates) * width * aspect - translation.x;
    }

    private static double computeUpY(Camera camera, Viewport vp, Point viewportCoordinates) {
        double width = camera.getWidth();
        Vec2 translation = camera.getCurrentTranslation();
        return computeNormalizedY(vp, viewportCoordinates) * width - translation.y;
    }

    public static Vec3 getVectorFromSphere(Camera camera, Viewport vp, Point viewportCoordinates) {
        Vec3 hitPoint = getVectorFromSphereAlt(camera, vp, viewportCoordinates);
        if (hitPoint != null) {
            return camera.getOrientation().rotateInverseVector(hitPoint);
        }
        return null;
    }

    public static Vec3 getVectorFromPlane(Camera camera, Viewport vp, Point viewportCoordinates) {
        double up1x = computeUpX(camera, vp, viewportCoordinates);
        double up1y = computeUpY(camera, vp, viewportCoordinates);

        Quat currentDragRotation = camera.getCurrentDragRotation();
        Vec3 altnormal = currentDragRotation.rotateVector(Vec3.ZAxis);
        if (altnormal.z == 0) {
            return null;
        }
        double zvalue = -(altnormal.x * up1x + altnormal.y * up1y) / altnormal.z;

        Vec3 hitPoint = new Vec3(up1x, up1y, zvalue);
        return currentDragRotation.rotateInverseVector(hitPoint);
    }

    public static Vec3 getVectorFromSphereAlt(Camera camera, Viewport vp, Point viewportCoordinates) {
        double up1x = computeUpX(camera, vp, viewportCoordinates);
        double up1y = computeUpY(camera, vp, viewportCoordinates);

        Vec3 hitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 1.) {
            hitPoint = new Vec3(up1x, up1y, Math.sqrt(1. - radius2));
            return camera.getCurrentDragRotation().rotateInverseVector(hitPoint);
        }
        return null;
    }

    public static double getRadiusFromSphereAlt(Camera camera, Viewport vp, Point viewportCoordinates) {
        double up1x = computeUpX(camera, vp, viewportCoordinates);
        double up1y = computeUpY(camera, vp, viewportCoordinates);

        return Math.sqrt(up1x * up1x + up1y * up1y);
    }

    public static Vec3 getVectorFromSphereTrackball(Camera camera, Viewport vp, Point viewportCoordinates) {
        double up1x = computeUpX(camera, vp, viewportCoordinates);
        double up1y = computeUpY(camera, vp, viewportCoordinates);
        Vec3 hitPoint;
        double radius2 = up1x * up1x + up1y * up1y;
        if (radius2 <= 0.5 * Sun.Radius2) {
            hitPoint = new Vec3(up1x, up1y, Math.sqrt(Sun.Radius2 - radius2));
        } else {
            hitPoint = new Vec3(up1x, up1y, 0.5 * Sun.Radius2 / Math.sqrt(radius2));
        }
        return camera.getCurrentDragRotation().rotateInverseVector(hitPoint);
    }

    public static Vec3 getVectorFromSphereOrPlane(Camera camera, Viewport vp, Vec2 normalizedScreenpos, Quat cameraDifferenceRotation) {
        double width = camera.getWidth();
        double aspect = vp.getWidth() / (double) vp.getHeight();
        Vec2 translation = camera.getCurrentTranslation();

        double up1x = normalizedScreenpos.x * width * aspect - translation.x;
        double up1y = normalizedScreenpos.y * width - translation.y;

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

}
