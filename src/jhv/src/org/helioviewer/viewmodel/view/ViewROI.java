package org.helioviewer.viewmodel.view;

import org.helioviewer.base.Region;
import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.metadata.MetaData;

public class ViewROI {

    private final int resolution = 3;
    private final GL3DVec2d[] pointlist = new GL3DVec2d[(resolution + 1) * 2 * 2];

    private static final ViewROI instance = new ViewROI();

    public static ViewROI getSingletonInstance() {
        return instance;
    }

    private ViewROI() {
        int count = 0;
        for (int i = 0; i <= resolution; i++) {
            for (int j = 0; j <= 1; j++) {
                pointlist[count] = new GL3DVec2d(2. * (1. * i / resolution - 0.5), -2. * (j - 0.5));
                count++;
            }
        }
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= resolution; j++) {
                pointlist[count] = new GL3DVec2d(2. * (i / 1. - 0.5), -2. * (1. * j / resolution - 0.5));
                count++;
            }
        }
    }

    public Region updateROI(MetaData metaData) {
        double minPhysicalX = Double.MAX_VALUE;
        double minPhysicalY = Double.MAX_VALUE;
        double maxPhysicalX = Double.MIN_VALUE;
        double maxPhysicalY = Double.MIN_VALUE;

        GL3DCamera activeCamera = Displayer.getActiveCamera();
        GL3DQuatd camdiff = activeCamera.getCameraDifferenceRotationQuatd(metaData.getRotationObs());

        for (int i = 0; i < pointlist.length; i++) {
            GL3DVec3d hitPoint;
            hitPoint = activeCamera.getVectorFromSphereOrPlane(pointlist[i], camdiff);
            if (hitPoint != null) {
                minPhysicalX = Math.min(minPhysicalX, hitPoint.x);
                minPhysicalY = Math.min(minPhysicalY, hitPoint.y);
                maxPhysicalX = Math.max(maxPhysicalX, hitPoint.x);
                maxPhysicalY = Math.max(maxPhysicalY, hitPoint.y);
            }
        }

        double widthxAdd = Math.abs((maxPhysicalX - minPhysicalX) * 0.025);
        double widthyAdd = Math.abs((maxPhysicalY - minPhysicalY) * 0.025);
        minPhysicalX = minPhysicalX - widthxAdd;
        maxPhysicalX = maxPhysicalX + widthxAdd;
        minPhysicalY = minPhysicalY - widthyAdd;
        maxPhysicalY = maxPhysicalY + widthyAdd;

        GL3DVec2d metPhysicalSize = metaData.getPhysicalSize();
        double metLLX = metaData.getPhysicalLowerLeft().x;
        double metLLY = metaData.getPhysicalLowerLeft().y;
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
