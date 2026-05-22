package org.helioviewer.jhv.imagedata;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.metadata.MetaData;

public class ImageData {

    private final ImageBuffer imageBuffer;

    private final MetaData metaData;
    private final Region region;
    private final Position viewpoint;

    public ImageData(@Nonnull ImageBuffer _imageBuffer, @Nonnull MetaData _metaData, @Nonnull Region _region, @Nonnull Position _viewpoint) {
        imageBuffer = _imageBuffer;

        metaData = _metaData;
        region = _region;
        viewpoint = _viewpoint;
    }

    @Nonnull
    public ImageBuffer getImageBuffer() {
        return imageBuffer;
    }

    @Nonnull
    public MetaData getMetaData() {
        return metaData;
    }

    @Nonnull
    public Region getRegion() {
        return region;
    }

    @Nonnull
    public Position getViewpoint() {
        return viewpoint;
    }

}
