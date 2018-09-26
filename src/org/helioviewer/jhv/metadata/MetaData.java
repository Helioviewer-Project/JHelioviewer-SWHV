package org.helioviewer.jhv.metadata;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.position.Position;

public interface MetaData {

    int getFrameNumber();

    @Nonnull
    Region getPhysicalRegion();

    int getPixelWidth();

    int getPixelHeight();

    double getUnitPerArcsec();

    double getResponseFactor();

    double getCROTA();

    double getSCROTA();

    double getCCROTA();

    double getInnerCutOffRadius();

    double getOuterCutOffRadius();

    double getCutOffValue();

    @Nonnull
    Vec3 getCutOffDirection();

    @Nonnull
    Position getViewpoint();

    @Nonnull
    Quat getCenterRotation();

    @Nonnull
    Region roiToRegion(@Nonnull SubImage roi, double factorX, double factorY);

    double xPixelFactor(double xPoint);

    double yPixelFactor(double yPoint);

    @Nonnull
    String getUnit();

    @Nullable
    float[] getPhysicalLUT();

}
