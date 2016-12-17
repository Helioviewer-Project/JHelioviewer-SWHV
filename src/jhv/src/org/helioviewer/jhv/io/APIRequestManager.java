package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Locale;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.fitsview.FITSView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ImageCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;
import org.helioviewer.jhv.viewmodel.view.simpleimageview.SimpleImageView;

public class APIRequestManager {

    public static URI requestRemoteFile(APIRequest req) throws IOException {
        try {
            APIResponse response = new APIResponse(new DownloadStream(req.jpipRequest).getInput());
            // Could we handle the answer from the server
            if (!response.hasData()) {
                Log.error("Could not understand server answer from " + req.jpipRequest);
                Message.err("Invalid Server reply", "The server data could not be parsed.", false);
                return null;
            }
            // Just some error from the server
            String error = response.getString("error");
            if (error != null) {
                Log.error("Data query returned error: " + error);
                Message.err("Error getting the data", Message.formatMessageString(error), false);
                return null;
            }

            // Try to load
            if (response.getURI() != null) {
                // The server wants to load us the data
                String message = response.getString("message");
                if (message != null) {
                    Message.warn("Warning", Message.formatMessageString(message));
                }
                return response.getURI();
            } else {
                // We did not get a reply to load data or no reply at all
                String message = response.getString("message");
                if (message != null) {
                    Log.error("No data to load returned from " + req.jpipRequest);
                    Log.error("Server message: " + message);
                    Message.err("Server could not return data", Message.formatMessageString(message), false);
                } else {
                    Log.error("Did not find URI in response to " + req.jpipRequest);
                    Message.err("No data source response", "While quering the data source, the server did not provide an answer.", false);
                }
            }
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting JPIP URL", e);
            Message.err("Socket timeout", "Socket timeout while requesting JPIP URL", false);
        }

        return null;
    }

    public static View requestAndOpenRemoteFile(APIRequest req) throws IOException {
        return loadView(requestRemoteFile(req), req);
    }

    public static View loadView(URI uri, APIRequest req) throws IOException {
        if (uri == null || uri.getScheme() == null) {
            throw new IOException("Invalid URI");
        }

        URI downloadURI = req == null ? uri : req.fileRequest;
        try {
            String loc = uri.toString().toLowerCase(Locale.ENGLISH);
            if (loc.endsWith(".fits") || loc.endsWith(".fts")) {
                return new FITSView(uri);
            } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
                 return new SimpleImageView(uri);
            } else {
                JP2Image image = loc.contains("callisto") ? new JP2ImageCallisto(uri, downloadURI) : new JP2Image(uri, downloadURI);
                View view = image instanceof JP2ImageCallisto ? new JP2ViewCallisto(image) : new JP2View(image);
                view.setAPIRequest(req);
                return view;
            }
        } catch (InterruptedException ignore) {
            // nothing
        } catch (Exception e) {
            Log.debug("APIRequestManager.loadView(\"" + uri + "\", \"" + downloadURI + "\") ", e);
            throw new IOException(e.getMessage());
        }
        return null;
    }

}
