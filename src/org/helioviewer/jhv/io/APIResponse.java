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
    static URI get(@Nonnull APIRequest req) throws IOException {
        String jpipRequest = req.toJpipRequest();
        try {
            JSONObject data = JSONUtils.get(jpipRequest);

            if (!data.isNull("frames")) {
                JSONArray arr = data.getJSONArray("frames");
                data.put("frames", arr.length()); // don't log timestamps, modifies input
            }
            Log.debug("Response: " + data);

            String message = data.optString("message", null);
            if (message != null) {
                Message.warn("Warning", message);
            }
            String error = data.optString("error", null);
            if (error != null) {
                Log.error("Data query returned error: " + error);
                Message.err("Error getting the data", error, false);
                return null;
            }
            return new URI(data.getString("uri"));
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting JPIP URL", e);
            Message.err("Socket timeout", "Socket timeout while requesting JPIP URL", false);
        } catch (Exception e) {
            throw new IOException("Invalid response for " + jpipRequest, e);
        }
        return null;
    }

}
