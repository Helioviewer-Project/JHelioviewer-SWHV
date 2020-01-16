package org.helioviewer.jhv.io;

import java.net.URI;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.URIView;
import org.helioviewer.jhv.view.View;

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
                throw new Exception("Invalid URI list");

            if (uriList.length == 1)
                return new URIView(imageLayer.getExecutor(), null, uriList[0], URIView.URIType.FITS);
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
