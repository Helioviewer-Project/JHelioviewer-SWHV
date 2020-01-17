package org.helioviewer.jhv.io;

import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.ManyView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.J2KView;
import org.helioviewer.jhv.view.uri.URIView;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;

public class LoadView {

    public static ListenableFuture<View> get(ImageLayer layer, APIRequest req) {
        return EventQueueCallbackExecutor.pool.submit(new LoadRemote(layer, req), new Callback(layer));
    }

    public static ListenableFuture<View> get(ImageLayer layer, URI... uriList) {
        return EventQueueCallbackExecutor.pool.submit(new LoadURI(layer, uriList), new Callback(layer));
    }

    public static ListenableFuture<View> getFITS(ImageLayer layer, URI uri) {
        return EventQueueCallbackExecutor.pool.submit(new LoadFITS(layer, uri), new Callback(layer));
    }

    private static class LoadRemote implements Callable<View> {

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

    private static class LoadURI implements Callable<View> {

        private final ImageLayer layer;
        private final URI[] uriList;

        LoadURI(ImageLayer _layer, URI... _uriList) {
            layer = _layer;
            uriList = _uriList;
        }

        @Override
        public View call() throws Exception {
            if (uriList == null || uriList.length == 0)
                throw new Exception("Invalid URI list");

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

    private static class LoadFITS implements Callable<View> {

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

    private static class Callback implements FutureCallback<View> {

        final ImageLayer layer;

        Callback(ImageLayer _layer) {
            layer = _layer;
        }

        @Override
        public void onSuccess(View result) {
            if (result != null) // LoadRemote can return null
                layer.setView(result);
            else
                layer.unload();
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            if (t instanceof CancellationException ||
                    t instanceof ClosedByInterruptException ||
                    t instanceof InterruptedIOException ||
                    t instanceof InterruptedException)
                return; // ignore

            layer.unload();

            Log.error("An error occurred while opening the remote file: ", t);
            Message.err("An error occurred while opening the remote file: ", t.getMessage(), false);
            // t.printStackTrace();
        }

    }

    @Nonnull
    private static View loadView(DecodeExecutor executor, APIRequest req, URI uri, APIResponse res) throws Exception {
        if (uri == null || uri.getScheme() == null) {
            throw new Exception("Invalid URI: " + uri);
        }

        String loc = uri.toString().toLowerCase(Locale.ENGLISH);
        if (loc.endsWith(".fits") || loc.endsWith(".fts")) {
            return new URIView(executor, req, uri, URIView.URIType.FITS);
        } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
            return new URIView(executor, req, uri, URIView.URIType.GENERIC);
        } else {
            return new J2KView(executor, req, uri, res);
        }
    }

}
