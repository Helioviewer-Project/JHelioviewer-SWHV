package org.helioviewer.jhv.io;

import java.net.URI;

import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.View;

class LoadRequestTask extends LoadViewTask {

    LoadRequestTask(ImageLayer _imageLayer, URI _uri) {
        super(_imageLayer, _uri);
        setThreadName("MAIN--LoadRequest");
    }

    @Override
    protected View backgroundWork() {
        try {
            APIRequest req = APIRequest.fromRequestJson(JSONUtils.get(uri));
            return requestAndOpenRemoteFile(req);
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
