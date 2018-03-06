package org.helioviewer.jhv.camera;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.math.Mat4;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLShape;

import com.jogamp.opengl.GL2;

public class CameraHelper {

    private static final double halfDepth = 3 * Sun.MeanEarthDistance;

    public static void applyPerspectiveLatitudinal(Camera camera, Viewport vp, GL2 gl) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        double width = camera.getWidth();
        gl.glOrtho(-width * vp.aspect, width * vp.aspect, -width, width, -1, 1);

        gl.glMatrixMode(GL2.GL_MODELVIEW);

        Vec2 translation = camera.getCurrentTranslation();
        Mat4 transformation = Mat4.translation(translation.x, translation.y, 0);
        gl.glLoadMatrixd(transformation.m, 0);
    }

    public static void applyPerspective(Camera camera, Viewport vp, GL2 gl, GLShape blackCircle) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        double width = camera.getWidth();
        gl.glOrtho(-width * vp.aspect, width * vp.aspect, -width, width, -halfDepth, halfDepth);

        gl.glMatrixMode(GL2.GL_MODELVIEW);

        Vec2 translation = camera.getCurrentTranslation();
        Mat4 transformation = Mat4.translation(translation.x, translation.y, 0);
        gl.glLoadMatrixd(transformation.m, 0);

        blackCircle.renderShape(gl, GL2.GL_TRIANGLE_FAN);

        transformation = camera.getRotation().toMatrix().translate(translation.x, translation.y, 0);
        gl.glLoadMatrixd(transformation.m, 0);
    }

    private static double computeNormalizedX(Viewport vp, double screenX) {
        return 2. * ((screenX - vp.x) / vp.width - 0.5);
    }

    private static double computeNormalizedY(Viewport vp, double screenY) {
        return -2. * ((screenY - vp.yAWT) / vp.height - 0.5);
    }

    public static double computeUpX(Camera camera, Viewport vp, double screenX) {
        double width = camera.getWidth();
        Vec2 translation = camera.getCurrentTranslation();
        return computeNormalizedX(vp, screenX) * width * vp.aspect - translation.x;
    }

    public static double computeUpY(Camera camera, Viewport vp, double screenY) {
        double width = camera.getWidth();
        Vec2 translation = camera.getCurrentTranslation();
        return computeNormalizedY(vp, screenY) * width - translation.y;
    }

    public static Vec3 getVectorFromSphereTrackball(Camera camera, Viewport vp, double screenX, double screenY) {
        double up1x = computeUpX(camera, vp, screenX);
        double up1y = computeUpY(camera, vp, screenY);
        double radius2 = up1x * up1x + up1y * up1y;

        Vec3 hitPoint;
        if (radius2 <= 0.5 * Sun.Radius2)
            hitPoint = new Vec3(up1x, up1y, Math.sqrt(Sun.Radius2 - radius2));
        else
            hitPoint = new Vec3(up1x, up1y, 0.5 * Sun.Radius2 / Math.sqrt(radius2));
        return camera.getCurrentDragRotation().rotateInverseVector(hitPoint);
    }

    @Nullable
    public static Vec3 getVectorFromSphere(Camera camera, Viewport vp, double screenX, double screenY, Quat rotation, boolean correctDrag) {
        double up1x = computeUpX(camera, vp, screenX);
        double up1y = computeUpY(camera, vp, screenY);
        double radius2 = up1x * up1x + up1y * up1y;

        if (radius2 > Sun.Radius2)
            return null;

        Vec3 hitPoint = new Vec3(up1x, up1y, Math.sqrt(Sun.Radius2 - radius2));
        if (correctDrag)
            hitPoint = camera.getCurrentDragRotation().rotateInverseVector(hitPoint);
        return rotation.rotateInverseVector(hitPoint);
    }

    @Nullable
    public static Vec3 getVectorFromPlane(Camera camera, Viewport vp, double screenX, double screenY, Quat rotation, boolean correctDrag) {
        Quat currentDragRotation = camera.getCurrentDragRotation();
        Vec3 altnormal = rotation.rotateVector(Vec3.ZAxis);
        if (correctDrag)
            altnormal = currentDragRotation.rotateVector(Vec3.ZAxis);

        if (altnormal.z == 0)
            return null;

        double up1x = computeUpX(camera, vp, screenX);
        double up1y = computeUpY(camera, vp, screenY);
        double zvalue = -(altnormal.x * up1x + altnormal.y * up1y) / altnormal.z;

        Vec3 hitPoint = new Vec3(up1x, up1y, zvalue);
        if (correctDrag)
            hitPoint = currentDragRotation.rotateInverseVector(hitPoint);
        return rotation.rotateInverseVector(hitPoint);
    }

    @Nullable
    public static Vec3 getVectorFromSphereOrPlane(Camera camera, Viewport vp, double x, double y, Quat cameraDifferenceRotation) {
        Vec3 rotatedHitPoint = getVectorFromSphere(camera, vp, x, y, cameraDifferenceRotation, false);
        if (rotatedHitPoint != null && rotatedHitPoint.z > 0.)
            return rotatedHitPoint;

        return getVectorFromPlane(camera, vp, x, y, cameraDifferenceRotation, false);
    }

    public static void zoomToFit(Camera camera) {
        double size = 1;
        if (Displayer.mode == Displayer.DisplayMode.Orthographic) {
            size = ImageLayers.getLargestPhysicalHeight();
        }

        double newFOV = Camera.INITFOV;
        if (size != 0)
            newFOV = 2. * Math.atan2(0.5 * size, camera.getViewpoint().distance);
        camera.setFOV(newFOV);
    }

}
