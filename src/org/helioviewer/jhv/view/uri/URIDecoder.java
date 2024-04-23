package org.helioviewer.jhv.view.uri;

import java.io.File;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageBuffer;

record URIDecoder(File file, URIImageReader reader, boolean mgn) implements Callable<ImageBuffer> {

    @Nonnull
    @Override
    public ImageBuffer call() throws Exception {
        ImageBuffer imageBuffer = reader.readImageBuffer(file);
        if (imageBuffer == null) // e.g. FITS
            throw new Exception("Could not read: " + file);
        return ImageBuffer.mgnFilter(imageBuffer, mgn);
    }

}
