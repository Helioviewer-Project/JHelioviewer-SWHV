package org.helioviewer.jhv.view;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.position.Position;

public class URIDecoder implements Runnable {

    private final URIView view;
    private final Position viewpoint;

    public URIDecoder(URIView _view, Position _viewpoint) {
        view = _view;
        viewpoint = _viewpoint;
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName("URIDecoder");
            ImageBuffer imageBuffer = view.getReader().readImageBuffer(view.getURI());
            if (imageBuffer == null)
                throw new Exception("Could not read: " + view.getURI());
            view.setDataFromDecoder(imageBuffer, viewpoint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
