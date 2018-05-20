package org.helioviewer.jhv.io;

import java.net.URI;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONArray;
import org.json.JSONObject;

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
            int len;
            JSONObject jo = JSONUtils.get(uri);

            JSONArray ji = jo.getJSONArray("org.helioviewer.jhv.request.image");
            len = ji.length();
            for (int i = 0; i < len; i++) {
                APIRequest req = APIRequest.fromRequestJson(ji.getJSONObject(i));
                JHVGlobals.getExecutorService().execute(new LoadRemoteTask(ImageLayer.create(null), req));
            }

            JSONArray jt = jo.getJSONArray("org.helioviewer.jhv.request.timeline");
            len = jt.length();
            for (int i = 0; i < len; i++) {
                EVEPlugin.eveDataprovider.loadBand(jt.getJSONObject(i));
            }
        } catch (Exception e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

}
