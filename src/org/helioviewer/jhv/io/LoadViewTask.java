package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.fitsview.FITSView;
import org.helioviewer.jhv.view.jp2view.JP2View;
import org.helioviewer.jhv.view.simpleimageview.SimpleImageView;

class LoadViewTask extends JHVWorker<View, Void> {

    private final ImageLayer imageLayer;
    protected final URI uri;

    LoadViewTask(ImageLayer _imageLayer, URI _uri) {
        uri = _uri;
        imageLayer = _imageLayer;
        setThreadName("MAIN--LoadURI");
    }

    @Nullable
    @Override
    protected View backgroundWork() {
        try {
            return loadView(uri, null, null);
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
                View view = get();
                if (view != null) {
                    imageLayer.setView(view);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            imageLayer.unload();
        }
    }

    @Nullable
    private static View loadView(URI uri, APIRequest req, APIResponse res) throws IOException {
        if (uri == null || uri.getScheme() == null) {
            throw new IOException("Invalid URI: " + uri);
        }

        try {
            String loc = uri.toString().toLowerCase(Locale.ENGLISH);
            if (loc.endsWith(".fits") || loc.endsWith(".fts")) {
                return new FITSView(uri, req);
            } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
                 return new SimpleImageView(uri, req);
            } else {
                return new JP2View(uri, req, res);
            }
        } catch (InterruptedException ignore) {
            // nothing
        } catch (Exception e) {
            Log.debug("loadView(\"" + uri + "\") ", e);
            throw new IOException(e);
        }
        return null;
    }

    @Nullable
    protected static View requestAndOpenRemoteFile(APIRequest req) throws IOException {
        APIResponse res = APIRequestManager.requestRemoteFile(req);
        return res == null ? null : loadView(res.getURI(), req, res);
    }

}
