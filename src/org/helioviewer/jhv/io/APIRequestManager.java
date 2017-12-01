package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.log.Log;

public class APIRequestManager {

    public static URI requestRemoteFile(APIRequest req) throws IOException {
        try (NetClient nc = new NetClient(req.jpipRequest)) {
            APIResponse response = new APIResponse(JSONUtils.decodeJSON(nc.getReader()));
            // Just some error from the server
            String error = response.getError();
            if (error != null) {
                Log.error("Data query returned error: " + error);
                Message.err("Error getting the data", Message.formatMessageString(error), false);
                return null;
            }

            // Try to load
            String message = response.getMessage();
            URI uri = response.getURI();
            if (uri == null) {
                // We did not get a reply to load data or no reply at all
                if (message != null) {
                    Log.error("Server message for " + req.jpipRequest + " : " + message);
                    Message.err("Server could not return data", Message.formatMessageString(message), false);
                } else {
                    Log.error("Did not find URI in response to " + req.jpipRequest);
                    Message.err("No data source response", "While quering the data source, the server did not provide an answer.", false);
                }
            } else {
                // The server wants to load the data
                if (message != null) {
                    Message.warn("Warning", Message.formatMessageString(message));
                }
                return uri;
            }
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting JPIP URL", e);
            Message.err("Socket timeout", "Socket timeout while requesting JPIP URL", false);
        } catch (Exception e) {
            throw new IOException("Invalid response for " + req.jpipRequest, e);
        }
        return null;
    }

}
