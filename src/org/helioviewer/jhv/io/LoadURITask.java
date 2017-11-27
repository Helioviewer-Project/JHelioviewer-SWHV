package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.view.ProxyView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.fitsview.FITSView;
import org.helioviewer.jhv.view.jp2view.JP2View;
import org.helioviewer.jhv.view.simpleimageview.SimpleImageView;

class LoadURITask extends JHVWorker<View, Void> {

    private final ImageLayer imageLayer;
    private final URI uri;

    static void get(URI _uri) {
        String scheme = _uri.getScheme();
        ImageLayer layer = ImageLayer.create(null);
        if ("http".equals(scheme) || "https".equals(scheme))
            JHVGlobals.getExecutorService().execute(new DownloadViewTask(layer, new ProxyView(_uri)));
        else
            JHVGlobals.getExecutorService().execute(new LoadURITask(layer, _uri));
    }

    LoadURITask(ImageLayer _imageLayer, URI _uri) {
        uri = _uri;
        imageLayer = _imageLayer;
        setThreadName("MAIN--LoadURI");
    }

    @Override
    protected View backgroundWork() {
        try {
            return loadView(uri, null);
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

    private static View loadView(URI uri, APIRequest req) throws IOException {
        if (uri == null || uri.getScheme() == null) {
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
            Log.debug("loadView(\"" + uri + "\") ", e);
            throw new IOException(e);
        }
        return null;
    }

    protected static View requestAndOpenRemoteFile(APIRequest req) throws IOException {
        URI uri = APIRequestManager.requestRemoteFile(req);
        return uri == null ? null : loadView(uri, req);
    }

}
