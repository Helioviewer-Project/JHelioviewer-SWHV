package org.helioviewer.jhv.io;

import java.net.URI;

import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.fitsview.FITSView;

class LoadFITSTask extends LoadURITask {

    private final URI uri;

    LoadFITSTask(ImageLayer _imageLayer, URI _uri) {
        super(_imageLayer, null);
        uri = _uri;
        setThreadName("MAIN--LoadFITS");
    }

    @Override
    protected View backgroundWork() {
        try {
            return new FITSView(uri, null);
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
