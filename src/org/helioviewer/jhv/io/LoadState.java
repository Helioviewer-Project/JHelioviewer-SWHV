package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.selector.State;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

class LoadState {

    static void submit(@Nonnull URI uri) {
        EDTCallbackExecutor.pool.submit(new LoadStateURI(uri), new Callback());
    }

    static void submit(@Nonnull String json) {
        EDTCallbackExecutor.pool.submit(new LoadStateString(json), new Callback());
    }

    private record LoadStateURI(URI uri) implements Callable<JSONObject> {
        @Override
        public JSONObject call() throws Exception {
            return JSONUtils.get(uri).getJSONObject("org.helioviewer.jhv.state");
        }
    }

    private record LoadStateString(String json) implements Callable<JSONObject> {
        @Override
        public JSONObject call() {
            return new JSONObject(json).getJSONObject("org.helioviewer.jhv.state");
        }
    }

    private static class Callback implements FutureCallback<JSONObject> {

        @Override
        public void onSuccess(JSONObject result) {
            State.load(result);
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            Log.error(t);
            Message.err("An error occurred opening the remote file", t.getMessage());
        }

    }

}
