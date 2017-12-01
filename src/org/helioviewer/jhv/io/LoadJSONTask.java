package org.helioviewer.jhv.io;

import java.net.URI;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.View;

class LoadJSONTask extends LoadURITask {

    private final URI uri;

    LoadJSONTask(ImageLayer _imageLayer, URI _uri) {
        super(_imageLayer, null);
        uri = _uri;
        setThreadName("MAIN--LoadJSON");
    }

    @Override
    protected View backgroundWork() {
        try (NetClient is = new NetClient(uri.toURL())) {
            APIRequest req = APIRequest.fromRequestJson(JSONUtils.decodeJSON(is.getReader()));
            return requestAndOpenRemoteFile(req);
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
