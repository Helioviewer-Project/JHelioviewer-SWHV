package org.helioviewer.jhv.io;

import java.io.IOException;

import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.View;

public class LoadRemoteTask extends LoadURITask {

    private final APIRequest req;

    public LoadRemoteTask(ImageLayer _imageLayer, APIRequest _req) {
        super(_imageLayer, null);
        req = _req;
        setThreadName("MAIN--LoadRemote");
    }

    @Override
    protected View backgroundWork() {
        try {
            return requestAndOpenRemoteFile(req);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
