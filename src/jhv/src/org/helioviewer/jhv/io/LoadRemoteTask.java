package org.helioviewer.jhv.io;

import java.io.IOException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.io.APIRequestManager.APIRequest;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.viewmodel.view.View;

public class LoadRemoteTask extends LoadURITask {

    private final APIRequest req;

    public LoadRemoteTask(ImageLayer _imageLayer, APIRequest _req) {
        super(_imageLayer, null, null);
        req = _req;
        setThreadName("MAIN--LoadRemote");
    }

    @Override
    protected View backgroundWork() {
        View view = null;
        try {
            view = APIRequestManager.requestAndOpenRemoteFile(req, true);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return view;
    }

}
