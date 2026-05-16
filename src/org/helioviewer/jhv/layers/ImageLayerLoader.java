package org.helioviewer.jhv.layers;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Message;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.DataUri.Format.Image;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.threads.Tasks;
import org.helioviewer.jhv.view.DecodeExecutor;
import org.helioviewer.jhv.view.ManyView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.J2KView;
import org.helioviewer.jhv.view.uri.URIView;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Throwables;

final class ImageLayerLoader {

    private ImageLayerLoader() {}

    static Future<View> submit(@Nonnull ImageLayer layer, @Nonnull APIRequest req) {
        return Tasks.submit("request", new LoadRemote(layer, req), result -> onSuccess(layer, result), (logContext, t) -> onFailure(layer, t));
    }

    static Future<View> submit(@Nonnull ImageLayer layer, @Nonnull List<URI> uriList) {
        return Tasks.submit(uriList.toString(), new LoadURIImage(layer, uriList), result -> onSuccess(layer, result), (logContext, t) -> onFailure(layer, t));
    }

    private record LoadRemote(ImageLayer layer, APIRequest req) implements Callable<View> {
        @Override
        public View call() throws Exception {
            URI uri = requestAPI(req.toJpipRequest());
            return uri == null ? null : createView(layer.getExecutor(), req, uri);
        }
    }

    private record LoadURIImage(ImageLayer layer, List<URI> uriList) implements Callable<View> {
        @Override
        public View call() throws Exception {
            return loadUri(layer.getExecutor(), uriList);
        }
    }

    private static void onSuccess(ImageLayer layer, View result) {
        if (result != null) // LoadRemote can return null
            layer.setView(result);
        else
            layer.unload();
    }

    private static void onFailure(ImageLayer layer, Throwable t) {
        if (JHVThread.isInterrupted(t)) { // ignore
            Log.warn(t);
            return;
        }
        layer.unload();

        Log.error(Throwables.getStackTraceAsString(t));
        Message.err("Error getting the data", t.getMessage());
    }

    private static View loadUri(DecodeExecutor executor, List<URI> uriList) throws Exception {
        if (uriList.size() == 1) {
            return createView(executor, null, uriList.getFirst());
        } else {
            List<View> views = uriList.parallelStream().map(uri -> {
                try {
                    return createView(executor, null, uri);
                } catch (Exception e) {
                    Log.warn(uri.toString(), e);
                    return null;
                }
            }).filter(Objects::nonNull).toList();
            return new ManyView(views);
        }
    }

    private static View createView(DecodeExecutor executor, APIRequest req, URI uri) throws Exception {
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
            return parseAPIResponse(JSONUtils.get(new URI(url)));
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting JPIP URL", e);
            Message.err("Socket timeout", "Socket timeout while requesting JPIP URL.");
        } catch (Exception e) {
            throw new Exception("Invalid response for " + url, e);
        }
        return null;
    }

    @Nullable
    private static URI parseAPIResponse(JSONObject data) throws Exception {
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
    }

}
