package org.helioviewer.jhv.io;

import java.net.URI;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.view.View;

public class LoadJSONTask extends LoadURITask {

    private final URI uri;

    public LoadJSONTask(ImageLayer _imageLayer, URI _uri) {
        super(_imageLayer, null);
        uri = _uri;
        setThreadName("MAIN--LoadJSON");
    }

    @Override
    protected View backgroundWork() {
        try {
            APIRequest req = APIRequest.fromRequestJson(JSONUtils.getJSONStream(new DownloadStream(uri.toURL()).getInput()));
            return APIRequestManager.requestAndOpenRemoteFile(req);
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
