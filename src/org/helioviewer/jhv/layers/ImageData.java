package org.helioviewer.jhv.layers;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.Region;

public record ImageData(
        @Nonnull DecodedImage image,
        @Nonnull MetaData metaData,
        @Nonnull Position viewpoint) {

    public ImageBuffer imageBuffer() {
        return image.imageBuffer();
    }

    public Region region() {
        return image.region();
    }

}
