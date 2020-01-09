package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.fits.FITSView;

class LoadFITSTask extends LoadViewTask {

    LoadFITSTask(ImageLayer _imageLayer, URI... _uriList) {
        super(_imageLayer, _uriList);
        setThreadName("MAIN--LoadFITS");
    }

    @Nullable
    @Override
    protected View backgroundWork() {
        try {
            if (uriList == null || uriList.length == 0)
                throw new IOException("Invalid URI list");

            return new FITSView(null, uriList[0]);
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
