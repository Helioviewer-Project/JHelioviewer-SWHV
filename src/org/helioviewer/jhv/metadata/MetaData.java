package org.helioviewer.jhv.metadata;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.position.Position;

public interface MetaData {

    int getFrameNumber();

    @Nonnull
    String getDisplayName();

    @Nonnull
    Region getPhysicalRegion();

    int getPixelWidth();

    int getPixelHeight();

    double getUnitPerArcsec();

    double getResponseFactor();

    float getCROTA();

    float getSCROTA();

    float getCCROTA();

    float getSector0();

    float getSector1();

    double getInnerRadius();

    double getOuterRadius();

    float getCutOffValue();

    @Nonnull
    Vec2 getCutOffDirection();

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
