package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.renderable.gui.State;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONObject;

class LoadStateTask extends JHVWorker<JSONObject, Void> {

    private final URI uri;

    LoadStateTask(URI _uri) {
        uri = _uri;
        setThreadName("MAIN--LoadState");
    }

    @Override
    protected JSONObject backgroundWork() {
        try (NetClient nc = NetClient.of(uri)) {
            return JSONUtils.readJSON(nc.getReader());
        } catch (IOException e) {
            Log.error("An error occurred while opening the remote file: ", e);
            Message.err("An error occurred while opening the remote file: ", e.getMessage(), false);
        }
        return null;
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            try {
                JSONObject jo = get();
                if (jo != null) {
                    State.load(jo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
