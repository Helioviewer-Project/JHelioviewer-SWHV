package org.helioviewer.jhv.view;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.MetaData;

public class ViewROI {

    private static final double extraSize = 0.05;
    private static final int resolution = 5;
    private static final Vec2[] pointlist = new Vec2[(resolution + 1) * 2 * 2];
    private static final Vec2[] dePoints = new Vec2[pointlist.length];

    private static final Region unitRadius = new Region(-1, -1, 2, 2);

    static {
        int count = 0;
        for (int i = 0; i <= resolution; i++) {
            for (int j = 0; j <= 1; j++) {
                pointlist[count] = new Vec2(2. * (i / (double) resolution - 0.5), -2. * (j - 0.5));
                dePoints[count] = new Vec2();
                count++;
            }
        }
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= resolution; j++) {
                pointlist[count] = new Vec2(2. * (i - 0.5), -2. * (j / (double) resolution - 0.5));
                dePoints[count] = new Vec2();
                count++;
            }
        }
    }

    public static Region updateROI(Camera camera, Viewport vp, MetaData m) {
        switch (Displayer.mode) {
        case Orthographic:
            for (int i = 0; i < pointlist.length; i++) {
                dePoints[i].x = CameraHelper.deNormalizeX(vp, pointlist[i].x);
                dePoints[i].y = CameraHelper.deNormalizeY(vp, pointlist[i].y);
            }

            Quat cameraRotation = camera.getRotation();
            Quat imageRotation = m.getCenterRotation();
            Quat camDiff = Quat.rotateWithConjugate(cameraRotation, imageRotation);

            double minPhysicalX = Double.MAX_VALUE;
            double minPhysicalY = Double.MAX_VALUE;
            double maxPhysicalX = Double.MIN_VALUE;
            double maxPhysicalY = Double.MIN_VALUE;
            for (int i = 0; i < pointlist.length; i++) {
                Vec3 hitPoint = CameraHelper.getVectorFromSphereOrPlane(camera, vp, dePoints[i].x, dePoints[i].y, camDiff);
                if (hitPoint != null) {
                    minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                    minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                    maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                    maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
                }
            }

            Vec3 startPoint = cameraRotation.rotateVector(Vec3.ZAxis);
            Vec3 endPoint = imageRotation.rotateVector(Vec3.ZAxis);
            Vec3 rotationAxis = Vec3.cross(startPoint, endPoint);
            double rotationAngleZ = Math.abs(Math.atan2(rotationAxis.length(), Vec3.dot(startPoint, endPoint)));

            startPoint = cameraRotation.rotateVector(Vec3.YAxis);
            endPoint = imageRotation.rotateVector(Vec3.YAxis);
            rotationAxis = Vec3.cross(startPoint, endPoint);
            double rotationAngleY = Math.abs(Math.atan2(rotationAxis.length(), Vec3.dot(startPoint, endPoint)));

            if (Math.max(rotationAngleZ, rotationAngleY) > Math.PI / 2) {
                camDiff = Quat.rotateWithConjugate(camDiff, Quat.createRotation(Math.PI, imageRotation.getRotationAxis()));
                for (int i = 0; i < pointlist.length; i++) {
                    Vec3 hitPoint = CameraHelper.getVectorFromSphereOrPlane(camera, vp, dePoints[i].x, dePoints[i].y, camDiff);
                    if (hitPoint != null) {
                        minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                        minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                        maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                        maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
                    }
                }
            }

            if (minPhysicalX > maxPhysicalX || minPhysicalY > maxPhysicalY) {
                return m.getPhysicalRegion();
            } else {
                double widthxAdd = Math.abs(extraSize * (maxPhysicalX - minPhysicalX));
                double widthyAdd = Math.abs(extraSize * (maxPhysicalY - minPhysicalY));
                minPhysicalX -= widthxAdd;
                maxPhysicalX += widthxAdd;
                minPhysicalY -= widthyAdd;
                maxPhysicalY += widthyAdd;

                Region r = m.getPhysicalRegion();
                if (minPhysicalX < r.llx)
                    minPhysicalX = r.llx;
                if (minPhysicalY < r.lly)
                    minPhysicalY = r.lly;
                if (maxPhysicalX > r.urx)
                    maxPhysicalX = r.urx;
                if (maxPhysicalY > r.ury)
                    maxPhysicalY = r.ury;

                double regionWidth = maxPhysicalX - minPhysicalX;
                double regionHeight = maxPhysicalY - minPhysicalY;
                if (regionWidth > 0 && regionHeight > 0) {
                    return new Region(minPhysicalX, minPhysicalY, regionWidth, regionHeight);
                } else {
                    Log.info("ViewROI.updateROI: empty ROI");
                    return new Region(minPhysicalX, minPhysicalY, 0, 0);
                }
            }
        case Latitudinal:
            return unitRadius;
        default:
            return m.getPhysicalRegion();
        }
    }

}
