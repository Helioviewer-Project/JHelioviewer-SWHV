package org.helioviewer.jhv.layers;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageBuffer;
import org.helioviewer.jhv.metadata.MetaData;

// The decoded pixels of a frame, at the viewpoint they were requested for.
public record ImageData(
        @Nonnull DecodedImage image,
        @Nonnull Frames.Frame frame,
        @Nonnull Position viewpoint) {

    public MetaData metaData() {
        return frame.metaData();
    }

    public ImageBuffer imageBuffer() {
        return image.imageBuffer();
    }

}
