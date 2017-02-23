package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.viewmodel.imagedata.SubImage;
import org.jetbrains.annotations.NotNull;

public interface MetaData {

    int getFrameNumber();

    @NotNull Region getPhysicalRegion();

    int getPixelWidth();

    int getPixelHeight();

    double getResponseFactor();

    @NotNull Position.Q getViewpoint();

    double getInnerCutOffRadius();

    double getOuterCutOffRadius();

    double getCutOffValue();

    @NotNull Vec3 getCutOffDirection();

    @NotNull Position.L getViewpointL();

    @NotNull Quat getCenterRotation();

    @NotNull Region roiToRegion(SubImage roi, double factorX, double factorY);

}
