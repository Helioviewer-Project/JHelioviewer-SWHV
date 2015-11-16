package org.helioviewer.jhv.viewmodel.view;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.Viewport;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class ViewROI {

    private static final double extraSize = 0.05;
    private static final int resolution = 5;
    private static final Vec2[] pointlist = new Vec2[(resolution + 1) * 2 * 2];

    private static final ViewROI instance = new ViewROI();

    private ViewROI() {
        int count = 0;
        for (int i = 0; i <= resolution; i++) {
            for (int j = 0; j <= 1; j++) {
                pointlist[count] = new Vec2(2. * (i / (double) resolution - 0.5), -2. * (j - 0.5));
                count++;
            }
        }
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= resolution; j++) {
                pointlist[count] = new Vec2(2. * (i - 0.5), -2. * (j / (double) resolution - 0.5));
                count++;
            }
        }
    }

    public static Region updateROI(Camera camera, Viewport vp, JHVDate masterTime, MetaData m) {
        double minPhysicalX = Double.MAX_VALUE;
        double minPhysicalY = Double.MAX_VALUE;
        double maxPhysicalX = Double.MIN_VALUE;
        double maxPhysicalY = Double.MIN_VALUE;

        camera.push(masterTime);

        Quat camDiff = camera.getCameraDifferenceRotationQuat(m.getRotationObs());
        for (int i = 0; i < pointlist.length; i++) {
            Vec3 hitPoint = CameraHelper.getVectorFromSphereOrPlane(camera, vp, pointlist[i], camDiff);
            minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
            minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
            maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
            maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
        }

        camera.pop();

        double widthxAdd = Math.abs(extraSize * (maxPhysicalX - minPhysicalX));
        double widthyAdd = Math.abs(extraSize * (maxPhysicalY - minPhysicalY));
        minPhysicalX = minPhysicalX - widthxAdd;
        maxPhysicalX = maxPhysicalX + widthxAdd;
        minPhysicalY = minPhysicalY - widthyAdd;
        maxPhysicalY = maxPhysicalY + widthyAdd;

        Region r = m.getPhysicalRegion();
        double metLLX = r.getLLX();
        double metLLY = r.getLLY();
        double metURX = r.getURX();
        double metURY = r.getURY();

        if (minPhysicalX < metLLX)
            minPhysicalX = metLLX;
        if (minPhysicalY < metLLY)
            minPhysicalY = metLLY;
        if (maxPhysicalX > metURX)
            maxPhysicalX = metURX;
        if (maxPhysicalY > metURY)
            maxPhysicalY = metURY;

        double regionWidth = maxPhysicalX - minPhysicalX;
        double regionHeight = maxPhysicalY - minPhysicalY;
        Region newRegion;
        if (regionWidth > 0 && regionHeight > 0) {
            newRegion = new Region(minPhysicalX, minPhysicalY, regionWidth, regionHeight);
        } else {
            newRegion = r;
            System.out.println(">> something is messed");
        }

        return newRegion;
    }

}
