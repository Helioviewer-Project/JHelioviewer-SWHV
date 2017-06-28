package org.helioviewer.jhv.io;

import java.net.URI;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;

public interface Layer {

    void add(URI uri);

    Image image = new Image();
    Timeline timeline = new Timeline();

    class Image implements Layer {
        @Override
        public void add(URI uri) {
            JHVGlobals.getExecutorService().execute(new LoadURITask(ImageLayer.create(null), uri));
        }
    }

    class Timeline implements Layer {
        @Override
        public void add(URI uri) {
            EVEPlugin.eveDataprovider.loadBand(uri);
        }
    }

}
