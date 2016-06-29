package org.helioviewer.jhv.io;

import java.net.URI;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.layers.ImageLayer;

public class LoadStartup {

    /**
     * Loads the images which have to be displayed when the program starts.
     *
     * If there are any images defined on the command line then this tries
     * to load those images.
     * */
    public static void loadCommandLine() {
        // -jpx
        for (URI jpxUrl : CommandLineProcessor.getJPXOptionValues()) {
            if (jpxUrl != null) {
                LoadURITask uriTask = new LoadURITask(ImageLayer.createImageLayer(), jpxUrl, jpxUrl);
                JHVGlobals.getExecutorService().execute(uriTask);
            }
        }
        // -jpip
        for (URI jpipUri : CommandLineProcessor.getJPIPOptionValues()) {
            if (jpipUri != null) {
                LoadURITask uriTask = new LoadURITask(ImageLayer.createImageLayer(), jpipUri, jpipUri);
                JHVGlobals.getExecutorService().execute(uriTask);
            }
        }
    }

}
