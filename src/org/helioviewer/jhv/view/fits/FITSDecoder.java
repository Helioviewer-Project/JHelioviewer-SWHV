package org.helioviewer.jhv.view.fits;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.position.Position;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

class FITSDecoder implements Runnable {

    private static final ThreadLocal<Cache<FITSView, ImageBuffer>> decodeCache = ThreadLocal.withInitial(() -> CacheBuilder.newBuilder().softValues().build());

    private final FITSView view;
    private final Position viewpoint;

    FITSDecoder(FITSView _view, Position _viewpoint) {
        view = _view;
        viewpoint = _viewpoint;
    }

    @Override
    public void run() {
        if (view == null) {
            return;
        }

        ImageBuffer imageBuffer = decodeCache.get().getIfPresent(view);
        if (imageBuffer == null) {
            try {
                imageBuffer = FITSImage.getHDU(view.getURI());
                if (imageBuffer == null)
                    throw new Exception("Could not read FITS: " + view.getURI());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        decodeCache.get().put(view, imageBuffer);
        view.setDataFromDecoder(imageBuffer, viewpoint);
    }

    void abolish() {
        decodeCache.get().invalidateAll();
    }

}
