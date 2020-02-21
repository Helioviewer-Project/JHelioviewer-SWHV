package org.helioviewer.jhv.camera;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;

public class CameraHelper {

    private static double computeNormalizedX(Viewport vp, double screenX) {
        return (screenX - vp.x) / vp.width - 0.5;
    }

    private static double computeNormalizedY(Viewport vp, double screenY) {
        return 0.5 - (screenY - vp.yAWT) / vp.height;
    }

    public static double computeUpX(Camera camera, Viewport vp, double screenX) {
        double width = camera.getCameraWidth();
        Vec2 translation = camera.getTranslation();
        return computeNormalizedX(vp, screenX) * width * vp.aspect - translation.x;
    }

    public static double computeUpY(Camera camera, Viewport vp, double screenY) {
        double width = camera.getCameraWidth();
        Vec2 translation = camera.getTranslation();
        return computeNormalizedY(vp, screenY) * width - translation.y;
    }

    public static double getPixelFactor(Camera camera, Viewport vp) {
        return vp.height / camera.getCameraWidth();
    }

    static Vec3 getVectorFromSphereTrackball(Camera camera, Viewport vp, double screenX, double screenY, double refRadius2) {
        double up1x = computeUpX(camera, vp, screenX);
        double up1y = computeUpY(camera, vp, screenY);
        double radius2 = up1x * up1x + up1y * up1y;

        double z = radius2 <= 0.5 * refRadius2 ? Math.sqrt(refRadius2 - radius2) : 0.5 * refRadius2 / Math.sqrt(radius2);
        Vec3 hitPoint = new Vec3(up1x, up1y, z);
        return camera.getDragRotation().rotateInverseVector(hitPoint);
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
            hitPoint = camera.getDragRotation().rotateInverseVector(hitPoint);
        return rotation.rotateInverseVector(hitPoint);
    }

    @Nullable
    public static Vec3 getVectorFromPlane(Camera camera, Viewport vp, double screenX, double screenY, Quat rotation, boolean correctDrag) {
        Quat dragRotation = camera.getDragRotation();
        Vec3 altnormal = rotation.rotateVector(Vec3.ZAxis);
        if (correctDrag)
            altnormal = dragRotation.rotateVector(Vec3.ZAxis);

        if (altnormal.z == 0)
            return null;

        double up1x = computeUpX(camera, vp, screenX);
        double up1y = computeUpY(camera, vp, screenY);
        double zvalue = -(altnormal.x * up1x + altnormal.y * up1y) / altnormal.z;

        Vec3 hitPoint = new Vec3(up1x, up1y, zvalue);
        if (correctDrag)
            hitPoint = dragRotation.rotateInverseVector(hitPoint);
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
        if (Display.mode == Display.DisplayMode.Orthographic) {
            size = ImageLayers.getLargestPhysicalHeight();
        }

        double newFOV = Camera.INITFOV;
        if (size != 0)
            newFOV = 2. * Math.atan2(0.5 * size, camera.getViewpoint().distance);
        camera.setFOV(newFOV);
    }

    public static void rotate2Earth(Position viewpoint) {
        Position e = Sun.getEarthHCI(viewpoint.time);
        Transform.rotateViewInverse(new Quat(e.lat, 2 * viewpoint.lon - e.lon));
    }

    public static void rotate2EarthLon(Position viewpoint) {
        Position e = Sun.getEarthHCI(viewpoint.time);
        Transform.rotateViewInverse(new Quat(0, 2 * viewpoint.lon - e.lon));
    }

}
