package org.helioviewer.jhv.image;

import org.helioviewer.jhv.wcs.ImageGeometry;

public final class DecodedImage {

    private final ImageBuffer imageBuffer;
    private final ImageGeometry geometry;

    // The decoder starts with one reference and transfers it to the cache.
    // After publication, delivery and layer references are retained and released on the EDT.
    private int references = 1;

    public DecodedImage(ImageBuffer _imageBuffer, ImageGeometry _geometry) {
        imageBuffer = _imageBuffer;
        geometry = _geometry;
    }

    public ImageBuffer imageBuffer() {
        return imageBuffer;
    }

    public ImageGeometry geometry() {
        return geometry;
    }

    public void retain() {
        if (references <= 0)
            throw new IllegalStateException("Decoded image has been released");
        references++;
    }

    public void release() {
        if (references <= 0)
            throw new IllegalStateException("Decoded image has been released");
        if (--references == 0)
            imageBuffer.free();
    }

}
