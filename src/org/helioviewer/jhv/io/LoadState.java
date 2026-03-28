package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.layers.selector.State;
import org.helioviewer.jhv.threads.Tasks;
import org.json.JSONObject;

class LoadState {

    static void submit(@Nonnull URI uri) {
        Tasks.submit(uri.toString(), new LoadStateURI(uri), State::load, "An error occurred opening the remote file");
    }

    static void submit(@Nonnull String json) {
        Tasks.submit("state", new LoadStateString(json), State::load, "An error occurred opening the remote file");
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

}
