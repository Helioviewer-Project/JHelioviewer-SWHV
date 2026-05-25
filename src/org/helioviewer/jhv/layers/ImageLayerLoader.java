package org.helioviewer.jhv.layers;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.Message;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.DataUri.Format.Image;
import org.helioviewer.jhv.io.DownloadLayer;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.threads.LatestWorker;
import org.helioviewer.jhv.threads.Tasks;
import org.helioviewer.jhv.view.ManyView;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.j2k.J2KView;
import org.helioviewer.jhv.view.uri.URIView;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Throwables;

final class ImageLayerLoader {

    private final LatestWorker<ImageBuffer> executor = new LatestWorker<>("View-Decoder");
    private final Consumer<View> onViewLoaded;
    private final Runnable onUnload;

    private Future<View> loadFuture;
    private Future<?> downloadFuture;
    private int loadGeneration;

    ImageLayerLoader(@Nonnull Consumer<View> _onViewLoaded, @Nonnull Runnable _onUnload) {
        onViewLoaded = _onViewLoaded;
        onUnload = _onUnload;
    }

    void load(APIRequest req) {
        cancelLoad();
        int gen = ++loadGeneration;
        loadFuture = Tasks.submit("request", () -> {
                    URI uri = requestAPI(req.toJpipRequest());
                    return uri == null ? null : createView(req, uri);
                },
                result -> onSuccess(result, gen),
                (logContext, t) -> onFailure(t, gen));
    }

    void load(List<URI> uriList) {
        cancelLoad();
        int gen = ++loadGeneration;
        loadFuture = Tasks.submit(uriList.toString(), () -> loadUri(uriList),
                result -> onSuccess(result, gen),
                (logContext, t) -> onFailure(t, gen));
    }

    boolean isLoading() {
        return loadFuture != null;
    }

    void clearLoadFuture() {
        loadFuture = null;
    }

    void startDownload(APIRequest req, ImageLayer layer, String baseName, DownloadLayer.Progress progress) {
        cancelDownload();
        downloadFuture = DownloadLayer.submit(req, layer, baseName, progress);
    }

    void cancelLoad() {
        loadGeneration++; // Invalidate any pending callbacks
        if (loadFuture != null) {
            loadFuture.cancel(true);
            loadFuture = null;
        }
    }

    void cancelDownload() {
        if (downloadFuture != null) {
            downloadFuture.cancel(true);
            downloadFuture = null;
        }
    }

    void abolish() {
        cancelLoad();
        cancelDownload();
        executor.abolish();
    }

    private void onSuccess(View result, int gen) {
        if (gen != loadGeneration) {
            if (result != null) {
                result.abolish();
            }
            return;
        }
        if (result != null) {
            onViewLoaded.accept(result);
        } else {
            onUnload.run();
        }
    }

    private void onFailure(Throwable t, int gen) {
        if (gen != loadGeneration) {
            return;
        }
        if (JHVThread.isInterrupted(t)) {
            Log.warn(t);
            return;
        }
        onUnload.run();

        Log.error(Throwables.getStackTraceAsString(t));
        Message.err("Error getting the data", t.getMessage());
    }

    private View loadUri(List<URI> uriList) throws Exception {
        if (uriList.size() == 1) {
            return createView(null, uriList.getFirst());
        } else {
            List<View> views = uriList.parallelStream().map(uri -> {
                try {
                    return createView(null, uri);
                } catch (Exception e) {
                    Log.warn(uri.toString(), e);
                    return null;
                }
            }).filter(Objects::nonNull).toList();
            return new ManyView(views);
        }
    }

    private View createView(APIRequest req, URI uri) throws Exception {
        DataUri dataUri = NetFileCache.get(uri);
        return switch (dataUri.format()) {
            case Image.JPIP, Image.JP2, Image.JPX -> new J2KView(executor, req, dataUri);
            case Image.FITS, Image.PNG, Image.JPEG -> new URIView(executor, dataUri);
            case Image.ZIP -> loadZip(dataUri.uri());
            default -> throw new Exception("Unknown image type");
        };
    }

    private View loadZip(URI uriZip) throws Exception {
        List<URI> uriList = FileUtils.unZip(uriZip);
        return loadUri(uriList);
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
