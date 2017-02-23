package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.Layers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.jogamp.opengl.GL2;

public class CameraHelper {

    private static final double clipNear = Sun.Radius * 3;
    private static final double clipFar = Sun.Radius * 10000;
    private static final double[] identity = Mat4.identity().m;

    public static Mat4 getOrthoMatrixInverse(@NotNull Camera camera, @NotNull Viewport vp) {
        double width = camera.getWidth();
        return Mat4.orthoInverse(-width * vp.aspect, width * vp.aspect, -width, width, clipNear, clipFar);
    }

    public static void applyPerspectiveLatitudinal(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        double width = camera.getWidth();
        gl.glOrtho(-width * vp.aspect, width * vp.aspect, -width, width, -1, 1);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadMatrixd(identity, 0);
    }

    public static void applyPerspective(@NotNull Camera camera, @NotNull Viewport vp, @NotNull GL2 gl) {
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        double width = camera.getWidth();
        gl.glOrtho(-width * vp.aspect, width * vp.aspect, -width, width, clipNear, clipFar);

        Vec2 translation = camera.getCurrentTranslation();
        Mat4 cameraTransformation = camera.getRotation().toMatrix().translate(translation.x, translation.y, -camera.getViewpoint().distance);
        // applyCamera
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadMatrixd(cameraTransformation.m, 0);
    }

    private static double computeNormalizedX(@NotNull Viewport vp, double screenX) {
        return 2. * ((screenX - vp.x) / vp.width - 0.5);
    }

    private static double computeNormalizedY(@NotNull Viewport vp, double screenY) {
        return -2. * ((screenY - vp.yAWT) / vp.height - 0.5);
    }

    public static double deNormalizeX(@NotNull Viewport vp, double normalizedX) {
        return 0.5 * vp.width * (1 + normalizedX) + vp.x;
    }

    public static double deNormalizeY(@NotNull Viewport vp, double normalizedY) {
        return 0.5 * vp.height * (1 - normalizedY) + vp.yAWT;
    }

    public static double computeUpX(@NotNull Camera camera, @NotNull Viewport vp, double screenX) {
        double width = camera.getWidth();
        Vec2 translation = camera.getCurrentTranslation();
        return computeNormalizedX(vp, screenX) * width * vp.aspect - translation.x;
    }

    public static double computeUpY(@NotNull Camera camera, @NotNull Viewport vp, double screenY) {
        double width = camera.getWidth();
        Vec2 translation = camera.getCurrentTranslation();
        return computeNormalizedY(vp, screenY) * width - translation.y;
    }

    public static Vec3 getVectorFromSphereTrackball(@NotNull Camera camera, @NotNull Viewport vp, double screenX, double screenY) {
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
    public static Vec3 getVectorFromSphere(@NotNull Camera camera, @NotNull Viewport vp, double screenX, double screenY, @NotNull Quat rotation, boolean correctDrag) {
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
    public static Vec3 getVectorFromPlane(@NotNull Camera camera, @NotNull Viewport vp, double screenX, double screenY, @NotNull Quat rotation, boolean correctDrag) {
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
    public static Vec3 getVectorFromSphereOrPlane(@NotNull Camera camera, @NotNull Viewport vp, double x, double y, @NotNull Quat cameraDifferenceRotation) {
        Vec3 rotatedHitPoint = getVectorFromSphere(camera, vp, x, y, cameraDifferenceRotation, false);
        if (rotatedHitPoint != null && rotatedHitPoint.z > 0.)
            return rotatedHitPoint;

        return getVectorFromPlane(camera, vp, x, y, cameraDifferenceRotation, false);
    }

    public static void zoomToFit(@NotNull Camera camera) {
        double size = 1;
        if (Displayer.mode == Displayer.DisplayMode.ORTHO) {
            size = Layers.getLargestPhysicalHeight();
        }

        double newFOV = Camera.INITFOV;
        if (size != 0)
            newFOV = 2. * Math.atan2(0.5 * size, camera.getViewpoint().distance);
        camera.setCameraFOV(newFOV);
    }

}
