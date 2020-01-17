package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.ManyView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.J2KView;
import org.helioviewer.jhv.view.uri.URIView;

import com.google.common.util.concurrent.FutureCallback;

class LoadNG {

    static class LoadRemote implements Callable<View> {

        private final ImageLayer layer;
        private final APIRequest req;

        LoadRemote(ImageLayer _layer, APIRequest _req) {
            layer = _layer;
            req = _req;
        }

        @Override
        public View call() throws Exception {
            APIResponse res = APIRequestManager.requestRemoteFile(req);
            return res == null ? null : loadView(layer.getExecutor(), req, res.getURI(), res);
        }

    }

    static class LoadView implements Callable<View> {

        private final ImageLayer layer;
        private final URI[] uriList;

        LoadView(ImageLayer _layer, URI... _uriList) {
            layer = _layer;
            uriList = _uriList;
        }

        @Override
        public View call() throws Exception {
            if (uriList == null || uriList.length == 0)
                throw new IOException("Invalid URI list");

            DecodeExecutor executor = layer.getExecutor();
            if (uriList.length == 1) {
                return loadView(executor, null, uriList[0], null);
            } else {
                ArrayList<View> views = new ArrayList<>(uriList.length);
                for (URI uri : uriList)
                    views.add(loadView(executor, null, uri, null));
                return new ManyView(views);
            }
        }

    }

    static class LoadFITS implements Callable<View> {

        private final ImageLayer layer;
        private final URI uri;

        LoadFITS(ImageLayer _layer, URI _uri) {
            layer = _layer;
            uri = _uri;
        }

        @Override
        public View call() throws Exception {
            if (uri == null)
                throw new Exception("Invalid URI");
            return new URIView(layer.getExecutor(), null, uri, URIView.URIType.FITS);
        }

    }

    static class Callback implements FutureCallback<View> {

        final ImageLayer layer;

        Callback(ImageLayer _layer) {
            layer = _layer;
        }

        @Override
        public void onSuccess(View result) {
            if (result != null)
                layer.setView(result);
            else
                layer.unload();
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            layer.unload();

            Log.error("An error occurred while opening the remote file: ", t);
            Message.err("An error occurred while opening the remote file: ", t.getMessage(), false);
            // t.printStackTrace();
        }

    }

    @Nullable
    private static View loadView(DecodeExecutor executor, APIRequest req, URI uri, APIResponse res) throws IOException {
        if (uri == null || uri.getScheme() == null) {
            throw new IOException("Invalid URI: " + uri);
        }

        try {
            String loc = uri.toString().toLowerCase(Locale.ENGLISH);
            if (loc.endsWith(".fits") || loc.endsWith(".fts")) {
                return new URIView(executor, req, uri, URIView.URIType.FITS);
            } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
                return new URIView(executor, req, uri, URIView.URIType.GENERIC);
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
