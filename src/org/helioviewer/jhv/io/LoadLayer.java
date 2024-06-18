package org.helioviewer.jhv.io;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.gui.Message;
import org.helioviewer.jhv.io.DataUri.Format.Image;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.ManyView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.J2KView;
import org.helioviewer.jhv.view.uri.URIView;
import org.helioviewer.jhv.threads.EDTCallbackExecutor;
import org.helioviewer.jhv.threads.JHVThread;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;

public class LoadLayer {

    public static Future<View> submit(@Nonnull ImageLayer layer, @Nonnull APIRequest req) {
        return EDTCallbackExecutor.pool.submit(new LoadRemote(layer, req), new Callback(layer));
    }

    public static void submit(@Nonnull ImageLayer layer, @Nonnull List<URI> uriList) {
        EDTCallbackExecutor.pool.submit(new LoadURIImage(layer, uriList), new Callback(layer));
    }

    private record LoadRemote(ImageLayer layer, APIRequest req) implements Callable<View> {
        @Override
        public View call() throws Exception {
            URI uri = requestAPI(req.toJpipUrl());
            return uri == null ? null : loadView(layer.getExecutor(), req, uri);
        }
    }

    private record LoadURIImage(ImageLayer layer, List<URI> uriList) implements Callable<View> {
        @Override
        public View call() throws Exception {
            return loadUri(layer.getExecutor(), uriList);
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

            Log.error(Throwables.getStackTraceAsString(t));
            Message.err("An error occurred opening the remote file", t.getMessage());
        }

    }

    private static View loadUri(DecodeExecutor executor, List<URI> uriList) throws Exception {
        if (uriList.size() == 1) {
            return loadView(executor, null, uriList.get(0));
        } else {
            List<View> views = uriList.parallelStream().map(uri -> {
                try {
                    return loadView(executor, null, uri);
                } catch (Exception e) {
                    Log.warn(uri.toString(), e);
                    return null;
                }
            }).filter(Objects::nonNull).toList();
            return new ManyView(views);
        }
    }

    private static View loadView(DecodeExecutor executor, APIRequest req, URI uri) throws Exception {
        DataUri dataUri = NetFileCache.get(uri);
        return switch (dataUri.format()) {
            case Image.JPIP, Image.JP2, Image.JPX -> new J2KView(executor, req, dataUri);
            case Image.FITS, Image.PNG, Image.JPEG -> new URIView(executor, dataUri);
            case Image.ZIP -> loadZip(executor, dataUri.uri());
            default -> throw new Exception("Unknown image type");
        };
    }

    private static View loadZip(DecodeExecutor executor, URI uriZip) throws Exception {
        List<URI> uriList = FileUtils.unZip(uriZip);
        return loadUri(executor, uriList);
    }

    @Nullable
    private static URI requestAPI(String url) throws Exception {
        try {
            // Log.info(url);
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
