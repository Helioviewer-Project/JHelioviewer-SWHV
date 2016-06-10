package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;

public interface MetaData {

    int getFrameNumber();

    Region getPhysicalRegion();

    int getPixelWidth();

    int getPixelHeight();

    Position.Q getViewpoint();

    double getInnerCutOffRadius();

    double getOuterCutOffRadius();

    double getCutOffValue();

    Vec3 getCutOffDirection();

    Position.L getViewpointL();

    Region roiToRegion(SubImage roi, double factorX, double factorY);

}
