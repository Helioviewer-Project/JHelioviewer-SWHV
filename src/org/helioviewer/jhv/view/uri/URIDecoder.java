package org.helioviewer.jhv.view.uri;

import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageBuffer;

class URIDecoder implements Callable<ImageBuffer> {

    private final URI uri;
    private final URIImageReader reader;

    URIDecoder(URI _uri, URIImageReader _reader) {
        uri = _uri;
        reader = _reader;
    }

    @Nonnull
    @Override
    public ImageBuffer call() throws Exception {
        ImageBuffer imageBuffer = reader.readImageBuffer(uri);
        if (imageBuffer == null) // e.g. FITS
            throw new Exception("Could not read: " + uri);
        return imageBuffer;
    }

}
