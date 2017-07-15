package org.helioviewer.jhv.io;

import java.net.URI;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.fitsview.FITSView;

public class LoadFITSTask extends LoadURITask {

    private final URI uri;

    public LoadFITSTask(ImageLayer _imageLayer, URI _uri) {
        super(_imageLayer, null);
        uri = _uri;
        setThreadName("MAIN--LoadFITS");
    }

    @Override
    protected View backgroundWork() {
        try {
            return new FITSView(uri);
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
