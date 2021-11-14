package org.helioviewer.jhv.view.uri;

import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageBuffer;

record URIDecoder(URI uri, URIImageReader reader, float[] minMax, boolean mgn) implements Callable<ImageBuffer> {

    @Nonnull
    @Override
    public ImageBuffer call() throws Exception {
        ImageBuffer imageBuffer = reader.readImageBuffer(uri, minMax);
        if (imageBuffer == null) // e.g. FITS
            throw new Exception("Could not read: " + uri);
        return ImageBuffer.mgnFilter(imageBuffer, mgn);
    }

}
