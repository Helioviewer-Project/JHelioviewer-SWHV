package org.helioviewer.jhv.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.position.Position;

public interface MetaData {

    int getFrameNumber();

    Region getPhysicalRegion();

    int getPixelWidth();

    int getPixelHeight();

    double getUnitPerArcsec();

    double getResponseFactor();

    double getCROTA();

    double getInnerCutOffRadius();

    double getOuterCutOffRadius();

    double getCutOffValue();

    Vec3 getCutOffDirection();

    Position getViewpoint();

    Quat getCenterRotation();

    Region roiToRegion(SubImage roi, double factorX, double factorY);

}
