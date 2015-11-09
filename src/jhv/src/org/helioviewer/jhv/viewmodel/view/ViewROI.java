package org.helioviewer.jhv.viewmodel.view;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3d;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class ViewROI {

    private static final double extraSize = 0.02;
    private static final int resolution = 5;
    private static final Vec2[] pointlist = new Vec2[(resolution + 1) * 2 * 2];

    private static ViewROI instance;

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

    public static ViewROI getInstance() {
        if (instance == null) {
            instance = new ViewROI();
        }
        return instance;
    }

    public static Region updateROI(Camera camera, JHVDate masterTime, MetaData m) {
        double minPhysicalX = Double.MAX_VALUE;
        double minPhysicalY = Double.MAX_VALUE;
        double maxPhysicalX = Double.MIN_VALUE;
        double maxPhysicalY = Double.MIN_VALUE;

        Camera localCamera = camera.duplicate(masterTime);

        Quat camDiff = localCamera.getCameraDifferenceRotationQuat(m.getRotationObs());
        for (int i = 0; i < pointlist.length; i++) {
            Vec3d hitPoint = localCamera.getVectorFromSphereOrPlane(pointlist[i], camDiff);
            minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
            minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
            maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
            maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
        }

        double widthxAdd = Math.abs(extraSize * (maxPhysicalX - minPhysicalX));
        double widthyAdd = Math.abs(extraSize * (maxPhysicalY - minPhysicalY));
        minPhysicalX = minPhysicalX - widthxAdd;
        maxPhysicalX = maxPhysicalX + widthxAdd;
        minPhysicalY = minPhysicalY - widthyAdd;
        maxPhysicalY = maxPhysicalY + widthyAdd;

        Vec2 metPhysicalSize = m.getPhysicalSize();
        double metLLX = m.getPhysicalLowerLeft().x;
        double metLLY = m.getPhysicalLowerLeft().y;
        double metURX = metLLX + metPhysicalSize.x;
        double metURY = metLLY + metPhysicalSize.y;

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
            newRegion = new Region(metLLX, metLLY, metURX - metLLX, metURY - metLLY);
        }

        return newRegion;
    }

}
