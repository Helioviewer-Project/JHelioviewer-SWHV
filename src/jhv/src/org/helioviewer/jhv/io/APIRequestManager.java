package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.EventDispatchQueue;
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

    public static View requestAndOpenRemoteFile(APIRequest req, boolean errorMessage) throws IOException {
        try {
            APIResponse response = new APIResponse(new DownloadStream(req.jpipRequest).getInput());
            // Could we handle the answer from the server
            if (!response.hasData()) {
                Log.error("Could not understand server answer from " + req.jpipRequest);
                if (errorMessage) {
                    Message.err("Invalid Server reply", "The server data could not be parsed.", false);
                }
                return null;
            }
            // Just some error from the server
            String error = response.getString("error");
            if (error != null) {
                Log.error("Data query returned error: " + error);
                if (errorMessage) {
                    Message.err("Error getting the data", Message.formatMessageString(error), false);
                }
                return null;
            }

            // Try to load
            if (response.getURI() != null) {
                // The server wants to load us the data
                String message = response.getString("message");
                if (message != null && errorMessage) {
                    Message.warn("Warning", Message.formatMessageString(message));
                }
                return loadView(response.getURI(), req);
            } else {
                // We did not get a reply to load data or no reply at all
                String message = response.getString("message");
                if (message != null) {
                    Log.error("No data to load returned from " + req.jpipRequest);
                    Log.error("Server message: " + message);
                    if (errorMessage) {
                        Message.err("Server could not return data", Message.formatMessageString(message), false);
                    }
                } else {
                    Log.error("Did not find URI in response to " + req.jpipRequest);
                    if (errorMessage) {
                        Message.err("No data source response", "While quering the data source, the server did not provide an answer.", false);
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting JPIP URL", e);
            Message.err("Socket timeout", "Socket timeout while requesting JPIP URL", false);
        }

        return null;
    }

    static View loadView(URI uri, APIRequest req) throws IOException {
        if (uri == null || uri.getScheme() == null) {
            throw new IOException("Invalid URI");
        }

        URI downloadURI;
        if (req != null)
            downloadURI = req.fileRequest;
        else
            downloadURI = uri;

        try {
            String loc = uri.toString().toLowerCase(Locale.ENGLISH);
            if (loc.endsWith(".fits") || loc.endsWith(".fts")) {
                return new FITSView(uri);
            } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
                 return new SimpleImageView(uri);
            } else {
                JP2Image jp2Image;
                if (loc.contains("callisto"))
                    jp2Image = new JP2ImageCallisto(uri, downloadURI);
                else
                    jp2Image = new JP2Image(uri, downloadURI);

                JP2View view = EventDispatchQueue.invokeAndWait(new AllocateJP2View(jp2Image));
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

    private static class AllocateJP2View implements Callable<JP2View> {
        private final AtomicReference<JP2Image> refJP2Image = new AtomicReference<>();

        public AllocateJP2View(JP2Image jp2Image) {
            refJP2Image.set(jp2Image);
        }

        @Override
        public JP2View call() {
            JP2View view;
            JP2Image jp2Image = refJP2Image.get();
            if (jp2Image instanceof JP2ImageCallisto) {
                view = new JP2ViewCallisto();
            } else {
                view = new JP2View();
            }
            view.setJP2Image(jp2Image);

            return view;
        }
    }

}
