package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.log.Log;

class APIRequestManager {

    @Nullable
    public static APIResponse requestRemoteFile(@Nonnull APIRequest req) throws IOException {
        String jpipRequest = req.toJpipRequest();
        try {
            APIResponse response = new APIResponse(JSONUtils.get(jpipRequest));
            // Just some error from the server
            String error = response.getError();
            if (error != null) {
                Log.error("Data query returned error: " + error);
                Message.err("Error getting the data", Message.formatMessage(error), false);
                return null;
            }

            // Try to load
            String message = response.getMessage();
            URI uri = response.getURI();
            if (uri == null) {
                // We did not get a reply to load data or no reply at all
                if (message != null) {
                    Log.error("Server message for " + jpipRequest + " : " + message);
                    Message.err("Server could not return data", Message.formatMessage(message), false);
                }/* else { most likely was cancelled
                    Log.error("Did not find URI in response to " + jpipRequest);
                    Message.err("No data source response", "While quering the data source, the server did not provide an answer.", false);
                }*/
                return null;
            } else {
                // The server wants to load the data
                if (message != null) {
                    Message.warn("Warning", Message.formatMessage(message));
                }
                return response;
            }
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting JPIP URL", e);
            Message.err("Socket timeout", "Socket timeout while requesting JPIP URL", false);
        } catch (Exception e) {
            throw new IOException("Invalid response for " + jpipRequest, e);
        }
        return null;
    }

}
