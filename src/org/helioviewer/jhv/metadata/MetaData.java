package org.helioviewer.jhv.metadata;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.wcs.WcsHeader;

public interface MetaData {

    @Nonnull
    String getDisplayName();

    @Nonnull
    Region getPhysicalRegion();

    double getUnitPerPixelY();

    double getUnitPerArcsec();

    float getResponseFactor();

    @Nonnull
    WcsHeader getWcsHeader();

    float getSector0();

    float getSector1();

    float getInnerRadius();

    float getOuterRadius();

    float getCutOffValue();

    float getCutOffX();

    float getCutOffY();

    @Nonnull
    Position getViewpoint();

    @Nonnull
    Region roiToRegion(int roiX, int roiY, int roiWidth, int roiHeight, double factorX, double factorY);

    @Nonnull
    Region roiToSunRegion(int roiX, int roiY, int roiWidth, int roiHeight, double factorX, double factorY);

    boolean getCalculateDepth();

    @Nonnull
    DetectorMask getDetectorMask();

}
