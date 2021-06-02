package org.helioviewer.jhv.view.uri;

import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageBuffer;

class URIDecoder implements Callable<ImageBuffer> {

    private final URI uri;
    private final URIImageReader reader;
    private final float[] minMax;
    private final boolean mgn;

    URIDecoder(URI _uri, URIImageReader _reader, float[] _minMax, boolean _mgn) {
        uri = _uri;
        reader = _reader;
        minMax = _minMax;
        mgn = _mgn;
    }

    @Nonnull
    @Override
    public ImageBuffer call() throws Exception {
        ImageBuffer imageBuffer = reader.readImageBuffer(uri, minMax);
        if (imageBuffer == null) // e.g. FITS
            throw new Exception("Could not read: " + uri);
        return ImageBuffer.mgnFilter(imageBuffer, mgn);
    }

}
