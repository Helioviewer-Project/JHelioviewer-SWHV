package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.jhv.base.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

class APIResponse {

    private final String err;
    private final String msg;
    private URI uri;
    private long[] frames;

    public APIResponse(JSONObject data) throws IOException {
        try {
            err = data.optString("error", null);
            msg = data.optString("message", null);

            if (!data.isNull("uri"))
                uri = new URI(data.getString("uri"));
            if (!data.isNull("frames")) {
                JSONArray arr = data.getJSONArray("frames");
                int len = arr.length();

                frames = new long[len];
                for (int i = 0; i < len; i++)
                    frames[i] = arr.getLong(i) * 1000L;
            }

            Log.debug("Response: " + data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public String getError() {
        return err;
    }

    public String getMessage() {
        return msg;
    }

    public URI getURI() {
        return uri;
    }

    public long[] getFrames() {
        return frames;
    }

}
