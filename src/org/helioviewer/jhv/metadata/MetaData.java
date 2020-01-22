package org.helioviewer.jhv.metadata;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.Quat;

public interface MetaData {

    int getFrameNumber();

    @Nonnull
    String getDisplayName();

    @Nonnull
    Region getPhysicalRegion();

    int getPixelWidth();

    int getPixelHeight();

    double getUnitPerArcsec();

    float getResponseFactor();

    float getCROTA();

    float getSCROTA();

    float getCCROTA();

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
    Quat getCenterRotation();

    @Nonnull
    Region roiToRegion(int roiX, int roiY, int roiWidth, int roiHeight, double factorX, double factorY);

    double xPixelFactor(double xPoint);

    double yPixelFactor(double yPoint);

    @Nonnull
    String getUnit();

    @Nullable
    float[] getPhysicalLUT();

    boolean getCalculateDepth();

}
