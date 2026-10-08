package org.helioviewer.jhv.image;

import org.helioviewer.jhv.metadata.Region;

public final class DecodedImage implements AutoCloseable {

    private final ImageBuffer imageBuffer;
    private final Region region;

    // The decoder starts with one reference and transfers it to the cache.
    // After publication, delivery and layer references are retained and released on the EDT.
    private int references = 1;

    public DecodedImage(ImageBuffer _imageBuffer, Region _region) {
        imageBuffer = _imageBuffer;
        region = _region;
    }

    public ImageBuffer imageBuffer() {
        return imageBuffer;
    }

    public Region region() {
        return region;
    }

    public void retain() {
        if (references <= 0)
            throw new IllegalStateException("Decoded image has been released");
        references++;
    }

    @Override
    public void close() {
        if (references <= 0)
            throw new IllegalStateException("Decoded image has been released");
        if (--references == 0)
            imageBuffer.free();
    }

}
