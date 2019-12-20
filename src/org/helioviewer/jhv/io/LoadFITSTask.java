package org.helioviewer.jhv.io;

import java.net.URI;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.fits.FITSView;

class LoadFITSTask extends LoadViewTask {

    LoadFITSTask(ImageLayer _imageLayer, URI _uri) {
        super(_imageLayer, _uri);
        setThreadName("MAIN--LoadFITS");
    }

    @Nullable
    @Override
    protected View backgroundWork() {
        try {
            return new FITSView(null, uri);
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
