package org.helioviewer.jhv.io;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
import org.helioviewer.jhv.threads.JHVThread;

import com.google.common.util.concurrent.FutureCallback;

public class LoadLayer {

    public static Future<View> submit(@Nonnull ImageLayer layer, @Nonnull APIRequest req) {
        return EventQueueCallbackExecutor.pool.submit(new LoadRemote(layer, req), new Callback(layer));
    }

    public static void submit(@Nonnull ImageLayer layer, @Nonnull List<URI> uriList) {
        EventQueueCallbackExecutor.pool.submit(new LoadURI(layer, uriList), new Callback(layer));
    }

    public static void submitFITS(@Nonnull ImageLayer layer, @Nonnull URI uri) {
        EventQueueCallbackExecutor.pool.submit(new LoadFITS(layer, uri), new Callback(layer));
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
        private final List<URI> uriList;

        LoadURI(ImageLayer _layer, List<URI> _uriList) {
            layer = _layer;
            uriList = _uriList;
        }

        @Override
        public View call() throws Exception {
            DecodeExecutor executor = layer.getExecutor();
            if (uriList.size() == 1) {
                return loadView(executor, null, uriList.get(0), null);
            } else {
                List<View> views = uriList.parallelStream().map(uri -> {
                    try {
                        return loadView(executor, null, uri, null);
                    } catch (Exception e) {
                        Log.error(e);
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
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
            if (JHVThread.isInterrupted(t)) { // ignore
                Log.info(t);
                return;
            }

            layer.unload();

            Log.error("An error occurred while opening the remote file:", t);
            Message.err("An error occurred while opening the remote file:", t.getMessage(), false);
        }

    }

    @Nonnull
    private static View loadView(DecodeExecutor executor, APIRequest req, URI uri, APIResponse res) throws Exception {
        String loc = uri.toString().toLowerCase(Locale.ENGLISH);
        if (loc.endsWith(".fits") || loc.endsWith(".fts") || loc.endsWith(".fits.gz")) {
            return new URIView(executor, req, uri, URIView.URIType.FITS);
        } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
            return new URIView(executor, req, uri, URIView.URIType.GENERIC);
        } else {
            return new J2KView(executor, req, uri, res);
        }
    }

}
