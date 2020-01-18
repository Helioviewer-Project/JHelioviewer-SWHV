package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventDispatchQueue;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.timelines.band.BandDataProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

class LoadRequest implements Callable<Void> {

    static Future<Void> submit(URI uri) {
        return EventQueueCallbackExecutor.pool.submit(new LoadRequest(uri), new Callback());
    }

    private final URI uri;

    private LoadRequest(URI _uri) {
        uri = _uri;
    }

    @Override
    public Void call() throws Exception {
        JSONObject jo = JSONUtils.get(uri);

        JSONArray ji = jo.optJSONArray("org.helioviewer.jhv.request.image");
        if (ji != null) {
            int len = ji.length();
            for (int i = 0; i < len; i++) {
                APIRequest req = APIRequest.fromRequestJson(ji.getJSONObject(i));
                ImageLayer layer = EventDispatchQueue.invokeAndWait(() -> ImageLayer.create(null));
                LoadLayer.submit(layer, req);
            }
        }

        JSONArray jt = jo.optJSONArray("org.helioviewer.jhv.request.timeline");
        if (jt != null) {
            int len = jt.length();
            for (int i = 0; i < len; i++) {
                BandDataProvider.loadBand(jt.getJSONObject(i));
            }
        }
        return null;
    }

    private static class Callback implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file: ", t);
            Message.err("An error occurred while opening the remote file: ", t.getMessage(), false);
        }

    }

}
