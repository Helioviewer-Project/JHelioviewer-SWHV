package org.helioviewer.jhv.view.fits;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.position.Position;

class FITSDecoder implements Runnable {

    private final FITSView view;
    private final Position viewpoint;

    FITSDecoder(FITSView _view, Position _viewpoint) {
        view = _view;
        viewpoint = _viewpoint;
    }

    @Override
    public void run() {
        try {
            ImageBuffer imageBuffer = FITSImage.getHDU(view.getURI());
            if (imageBuffer == null)
                throw new Exception("Could not read FITS: " + view.getURI());
            view.setDataFromDecoder(imageBuffer, viewpoint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
