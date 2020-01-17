package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.selector.State;
import org.helioviewer.jhv.log.Log;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

class LoadState implements Callable<JSONObject> {

    private final URI uri;

    LoadState(URI _uri) {
        uri = _uri;
    }

    @Override
    public JSONObject call() throws Exception {
        return JSONUtils.get(uri).getJSONObject("org.helioviewer.jhv.state");
    }

    static class Callback implements FutureCallback<JSONObject> {

        @Override
        public void onSuccess(JSONObject result) {
            State.load(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error("An error occurred while opening the remote file: ", t);
            Message.err("An error occurred while opening the remote file: ", t.getMessage(), false);
            // t.printStackTrace();
        }

    }

}
