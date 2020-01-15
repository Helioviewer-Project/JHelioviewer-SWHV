package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.ManyView;
import org.helioviewer.jhv.view.URIView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.J2KView;

class LoadViewTask extends JHVWorker<View, Void> {

    private final ImageLayer imageLayer;
    protected final URI[] uriList;

    LoadViewTask(ImageLayer _imageLayer, URI... _uriList) {
        uriList = _uriList;
        imageLayer = _imageLayer;
        setThreadName("MAIN--LoadURI");
    }

    @Nullable
    @Override
    protected View backgroundWork() {
        try {
            if (uriList == null || uriList.length == 0)
                throw new IOException("Invalid URI list");

            DecodeExecutor executor = new DecodeExecutor();
            if (uriList.length == 1) {
                return loadView(executor, null, uriList[0], null);
            } else {
                ArrayList<View> views = new ArrayList<>(uriList.length);
                for (URI uri : uriList)
                    views.add(loadView(executor, null, uri, null));
                return new ManyView(views);
            }
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
    protected static View loadView(DecodeExecutor executor, APIRequest req, URI uri, APIResponse res) throws IOException {
        if (uri == null || uri.getScheme() == null) {
            throw new IOException("Invalid URI: " + uri);
        }

        try {
            String loc = uri.toString().toLowerCase(Locale.ENGLISH);
            if (loc.endsWith(".fits") || loc.endsWith(".fts")) {
                return new URIView(executor, req, uri, URIView.URIType.FITS);
            } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
                return new URIView(executor, req, uri, URIView.URIType.SIMPLE);
            } else {
                return new J2KView(executor, req, uri, res);
            }
        } catch (InterruptedException ignore) {
            // nothing
        } catch (Exception e) {
            Log.debug("loadView(\"" + uri + "\") ", e);
            throw new IOException(e);
        }
        return null;
    }

}
