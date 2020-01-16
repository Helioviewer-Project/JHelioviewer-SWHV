package org.helioviewer.jhv.view;

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.imagedata.ImageBuffer;

public class URIDecoder implements Callable<ImageBuffer> {

    private final URIView view;

    public URIDecoder(URIView _view) {
        view = _view;
    }

    @Nonnull
    @Override
    public ImageBuffer call() throws Exception {
        ImageBuffer imageBuffer = view.getReader().readImageBuffer(view.getURI());
        if (imageBuffer == null) // e.g. FITS
            throw new Exception("Could not read: " + view.getURI());
        return imageBuffer;
    }

}
