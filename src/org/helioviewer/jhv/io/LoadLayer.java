package org.helioviewer.jhv.io;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.ManyView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.J2KView;
import org.helioviewer.jhv.view.uri.URIView;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;
import org.helioviewer.jhv.threads.JHVThread;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;

public class LoadLayer {

    public static Future<View> submit(@Nonnull ImageLayer layer, @Nonnull APIRequest req) {
        return EventQueueCallbackExecutor.pool.submit(new LoadRemote(layer, req), new Callback(layer));
    }

    public static void submit(@Nonnull ImageLayer layer, @Nonnull List<URI> uriList, boolean forceFITS) {
        EventQueueCallbackExecutor.pool.submit(new LoadURIImage(layer, uriList, forceFITS), new Callback(layer));
    }

    private record LoadRemote(ImageLayer layer, APIRequest req) implements Callable<View> {
        @Override
        public View call() throws Exception {
            URI uri = request(req.toJpipUrl());
            return uri == null ? null : loadView(layer.getExecutor(), req, uri, false);
        }
    }

    private record LoadURIImage(ImageLayer layer, List<URI> uriList, boolean forceFITS) implements Callable<View> {
        @Override
        public View call() throws Exception {
            DecodeExecutor executor = layer.getExecutor();
            if (uriList.size() == 1) {
                return loadView(executor, null, uriList.get(0), forceFITS);
            } else {
                List<View> views = uriList.parallelStream().map(uri -> {
                    try {
                        return loadView(executor, null, uri, forceFITS);
                    } catch (Exception e) {
                        Log.warn(uri.toString(), e);
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
                return new ManyView(views);
            }
        }
    }

    private record Callback(ImageLayer layer) implements FutureCallback<View> {

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
                Log.warn(t);
                return;
            }

            layer.unload();

            Log.error(t);
            Message.err("An error occurred opening the remote file", t.getMessage());
        }

    }

    private static View loadView(DecodeExecutor executor, APIRequest req, URI uri, boolean forceFITS) throws Exception {
        String loc = uri.toString().toLowerCase(Locale.ENGLISH);
        if (forceFITS || loc.endsWith(".fits") || loc.endsWith(".fts") || loc.endsWith(".fits.gz")) {
            return new URIView(executor, req, uri, URIView.URIType.FITS);
        } else if (loc.endsWith(".png") || loc.endsWith(".jpg") || loc.endsWith(".jpeg")) {
            return new URIView(executor, req, uri, URIView.URIType.GENERIC);
        } else {
            return new J2KView(executor, req, uri);
        }
    }

    private static URI request(String url) throws Exception {
        try {
            JSONObject data = JSONUtils.get(new URI(url));

            if (!data.isNull("frames")) {
                JSONArray arr = data.getJSONArray("frames");
                data.put("frames", arr.length()); // don't log timestamps, modifies input
            }
            Log.info(data.toString());

            String message = data.optString("message", null);
            if (message != null) {
                Message.warn("Warning", message);
            }
            String error = data.optString("error", null);
            if (error != null) {
                Log.error(error);
                Message.err("Error getting the data", error);
                return null;
            }
            return new URI(data.getString("uri"));
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting JPIP URL", e);
            Message.err("Socket timeout", "Socket timeout while requesting JPIP URL.");
        } catch (Exception e) {
            throw new Exception("Invalid response for " + url, e);
        }
        return null;
    }

}
