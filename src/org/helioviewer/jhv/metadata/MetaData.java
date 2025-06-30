package org.helioviewer.jhv.metadata;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;

public interface MetaData {

    @Nonnull
    String getDisplayName();

    @Nonnull
    Region getPhysicalRegion();

    int getPixelWidth();

    int getPixelHeight();

    double getUnitPerArcsec();

    float getResponseFactor();

    @Nonnull
    Vec2 getCRVAL();

    @Nonnull
    Quat getCROTA();

    float getSector0();

    float getSector1();

    float getInnerRadius();

    float getOuterRadius();

    float getCutOffValue();

    float getCutOffX();

    float getCutOffY();

    @Nonnull
    float[] getPV2();

    @Nonnull
    Position getViewpoint();

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
