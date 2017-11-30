package org.helioviewer.jhv.io;

import java.io.InputStream;
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
        try (InputStream is = new DownloadStream(uri.toURL()).getInput()) {
            APIRequest req = APIRequest.fromRequestJson(JSONUtils.getJSONStream(is));
            return requestAndOpenRemoteFile(req);
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
