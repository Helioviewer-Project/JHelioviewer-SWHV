package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.jhv.log.Log;
import org.json.JSONArray;
import org.json.JSONObject;

class APIResponse {

    private final String err;
    private final String msg;
    private URI uri;

    APIResponse(JSONObject data) throws IOException {
        try {
            err = data.optString("error", null);
            msg = data.optString("message", null);

            if (!data.isNull("uri"))
                uri = new URI(data.getString("uri"));
            if (!data.isNull("frames")) {
                JSONArray arr = data.getJSONArray("frames");
                data.put("frames", arr.length()); // don't log timestamps, modifies input
            }

            Log.debug("Response: " + data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    String getError() {
        return err;
    }

    String getMessage() {
        return msg;
    }

    URI getURI() {
        return uri;
    }

}
