package org.helioviewer.jhv.io;

import java.io.IOException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.View;

public class LoadRemoteTask extends LoadViewTask {

    private final APIRequest req;

    public LoadRemoteTask(ImageLayer _imageLayer, APIRequest _req) {
        super(_imageLayer);
        req = _req;
        setThreadName("MAIN--LoadRemote");
    }

    @Nullable
    @Override
    protected View backgroundWork() {
        try {
            APIResponse res = APIRequestManager.requestRemoteFile(req);
            return res == null ? null : loadView(res.getURI(), req, res, null, null);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
