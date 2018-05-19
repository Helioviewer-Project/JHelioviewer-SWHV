package org.helioviewer.jhv.io;

import java.net.URI;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONArray;

class LoadRequestTask extends JHVWorker<Void, Void> {

    private final URI uri;

    LoadRequestTask(URI _uri) {
        uri = _uri;
        setThreadName("MAIN--LoadRequest");
    }

    @Nullable
    @Override
    protected Void backgroundWork() {
        try {
            JSONArray ja = JSONUtils.get(uri).getJSONArray("org.helioviewer.jhv.request.image");
            int len = ja.length();
            for (int i = 0; i < len; i++) {
                APIRequest req = APIRequest.fromRequestJson(ja.getJSONObject(i));
                JHVGlobals.getExecutorService().execute(new LoadRemoteTask(ImageLayer.create(null), req));
            }
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
