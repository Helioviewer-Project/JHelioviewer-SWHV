package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.log.Log;
import org.json.JSONArray;
import org.json.JSONObject;

class APIResponse {

    @Nullable
    static APIResponse request(@Nonnull APIRequest req) throws IOException {
        String jpipRequest = req.toJpipRequest();
        try {
            APIResponse response = new APIResponse(JSONUtils.get(jpipRequest));
            if (response.message != null) {
                Message.warn("Warning", response.message);
            }
            if (response.error != null) {
                Log.error("Data query returned error: " + response.error);
                Message.err("Error getting the data", response.error, false);
                return null;
            }
            return response;
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting JPIP URL", e);
            Message.err("Socket timeout", "Socket timeout while requesting JPIP URL", false);
        } catch (Exception e) {
            throw new IOException("Invalid response for " + jpipRequest, e);
        }
        return null;
    }

    private final URI uri;
    private final String message;
    private final String error;

    private APIResponse(JSONObject data) throws IOException {
        try {
            uri = new URI(data.getString("uri"));
            message = data.optString("message", null);
            error = data.optString("error", null);

            if (!data.isNull("frames")) {
                JSONArray arr = data.getJSONArray("frames");
                data.put("frames", arr.length()); // don't log timestamps, modifies input
            }
            Log.debug("Response: " + data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Nonnull
    URI getURI() {
        return uri;
    }

}
