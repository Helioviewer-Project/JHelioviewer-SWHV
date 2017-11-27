package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Locale;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.fitsview.FITSView;
import org.helioviewer.jhv.view.jp2view.JP2View;
import org.helioviewer.jhv.view.simpleimageview.SimpleImageView;

public class APIRequestManager {

    public static URI requestRemoteFile(APIRequest req) throws IOException {
        try {
            APIResponse response = new APIResponse(JSONUtils.getJSONStream(new DownloadStream(req.jpipRequest).getInput()));
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
                    Log.error("No data to load returned from " + req.jpipRequest);
                    Log.error("Server message: " + message);
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

    static View requestAndOpenRemoteFile(APIRequest req) throws IOException {
        URI uri = requestRemoteFile(req);
        return uri == null ? null : loadView(uri, req);
    }

    static View loadView(URI uri, APIRequest req) throws IOException {
        String scheme;
        if (uri == null || (scheme = uri.getScheme()) == null) {
            throw new IOException("Invalid URI: " + uri);
        }

        try {
            String loc = uri.toString().toLowerCase(Locale.ENGLISH);
            if (loc.endsWith(".fits") || loc.endsWith(".fts")) {
                return new FITSView(uri);
            } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
                 return new SimpleImageView(uri);
            } else {
                return new JP2View(uri, req);
            }
        } catch (InterruptedException ignore) {
            // nothing
        } catch (Exception e) {
            Log.debug("APIRequestManager.loadView(\"" + uri + "\") ", e);
            throw new IOException(e);
        }
        return null;
    }

}
