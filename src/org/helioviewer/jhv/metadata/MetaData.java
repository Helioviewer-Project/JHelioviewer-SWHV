package org.helioviewer.jhv.metadata;

import java.net.URI;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.wcs.ImageGeometry;

public interface MetaData {

    URI UNKNOWN_SOURCE_URI = URI.create("unknown:unknown");

    @Nonnull
    String getDisplayName();

    @Nonnull
    URI getSourceUri();

    @Nonnull
    ImageGeometry getGeometry();

    float getResponseFactor();

    float getSector0();

    float getSector1();

    float getInnerRadius();

    float getOuterRadius();

    float getCutOffValue();

    float getCutOffX();

    float getCutOffY();

    @Nonnull
    Position getViewpoint();

    default double getRenderDistance(double viewDistance) {
        double imageDistance = getViewpoint().distance;
        return getGeometry().wcs.projection.isSurfaceMap() ? viewDistance : imageDistance;
    }

    boolean getCalculateDepth();

    @Nonnull
    DetectorMask getDetectorMask();

}
