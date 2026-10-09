package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.image.DecodedImage;
import org.helioviewer.jhv.image.ImageProcessingSettings;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.DownloadLayer;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.source.J2KView;
import org.helioviewer.jhv.source.URIView;
import org.helioviewer.jhv.thread.LatestWorker;
import org.helioviewer.jhv.view.ManyView;
import org.helioviewer.jhv.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

final class ImageLayerLoader {

    private final LatestWorker<DecodedImage> executor = new LatestWorker<>("View-Decoder");
    private final ImageProcessingSettings processingSettings;
    private final Consumer<View> onViewLoaded;
    private final Runnable onLoadFailed;

    // Owned by the EDT, including results queued after the worker has exited.
    private Thread loadThread;
    private Future<?> downloadFuture;

    ImageLayerLoader(ImageProcessingSettings _processingSettings, @Nonnull Consumer<View> _onViewLoaded, @Nonnull Runnable _onLoadFailed) {
        processingSettings = _processingSettings;
        onViewLoaded = _onViewLoaded;
        onLoadFailed = _onLoadFailed;
    }

    void load(APIRequest req) {
        load(() -> createView(req, requestAPI(req.toJpipRequest())));
    }

    void load(List<URI> uriList) {
        load(() -> loadUri(uriList));
    }

    private void load(Callable<View> task) {
        cancelLoad();
        Thread nextLoad = Thread.ofVirtual().name("Image-Load").unstarted(() -> {
            Thread worker = Thread.currentThread();
            try {
                View result = task.call();
                // Unlike FutureTask cancellation, this always transfers the result for cleanup.
                EventQueue.invokeLater(() -> finishLoad(worker, result, null));
            } catch (Throwable t) {
                EventQueue.invokeLater(() -> finishLoad(worker, null, t));
            }
        });
        nextLoad.start();
        loadThread = nextLoad;
    }

    boolean isLoading() {
        return loadThread != null;
    }

    void startDownload(APIRequest req, ImageLayer layer, String baseName, DownloadLayer.Progress progress) {
        cancelDownload();
        downloadFuture = DownloadLayer.submit(req, layer, baseName, progress);
    }

    void cancelLoad() {
        if (loadThread != null) {
            loadThread.interrupt();
            loadThread = null;
        }
    }

    void cancelDownload() {
        if (downloadFuture != null) {
            downloadFuture.cancel(true);
            downloadFuture = null;
        }
    }

    // EDT teardown: the layer is already excluded from getImageLayers(), so no new decode may be submitted.
    void abolish() {
        cancelLoad();
        cancelDownload();
        executor.dispose();
    }

    private void finishLoad(Thread worker, @Nullable View result, @Nullable Throwable error) {
        if (worker != loadThread) {
            if (result != null)
                result.abolish();
            return;
        }
        loadThread = null;
        if (error == null) {
            onViewLoaded.accept(result);
            return;
        }
        onLoadFailed.run();
        Log.errorStack(error);
        Message.err("Error getting the data", error.getMessage());
    }

    private View loadUri(List<URI> uriList) throws Exception {
        if (uriList.size() == 1)
            return createView(null, uriList.getFirst());
        Thread worker = Thread.currentThread();
        // Keep JPIP initialization on the interruptible loader thread.
        boolean jpip = uriList.stream().anyMatch(uri -> "jpip".equalsIgnoreCase(uri.getScheme()) || "jpips".equalsIgnoreCase(uri.getScheme()));
        List<View> views = (jpip ? uriList.stream() : uriList.parallelStream()).map(uri -> {
            if (worker.isInterrupted())
                return null;
            try {
                return createView(null, uri);
            } catch (Exception e) {
                Log.warn(uri.toString(), e);
                return null;
            }
        }).filter(Objects::nonNull).toList();
        try {
            return new ManyView(views);
        } catch (Throwable t) {
            views.forEach(View::abolish);
            throw t;
        }
    }

    private View createView(APIRequest req, URI uri) throws Exception {
        DataUri dataUri = NetFileCache.get(uri);
        return switch (dataUri.format()) {
            case JPIP, JP2, JPX -> new J2KView(executor, req, dataUri, processingSettings);
            case FITS, PNG, JPEG -> new URIView(executor, dataUri, processingSettings);
            case ZIP -> loadUri(FileUtils.unZip(dataUri.uri()));
            default -> throw new Exception("Unknown image type");
        };
    }

    private URI requestAPI(String url) throws Exception {
        try {
            return parseAPIResponse(JSONUtils.get(new URI(url)));
        } catch (Exception e) {
            throw new Exception("Invalid response for " + url + ": " + e.getMessage(), e);
        }
    }

    private URI parseAPIResponse(JSONObject data) throws Exception {
        if (!data.isNull("frames")) {
            JSONArray arr = data.getJSONArray("frames");
            data.put("frames", arr.length()); // don't log timestamps, modifies input
        }
        Log.info(data.toString());

        String message = data.optString("message", null);
        if (message != null) {
            Thread worker = Thread.currentThread();
            EventQueue.invokeLater(() -> {
                if (worker == loadThread)
                    Message.warn("Warning", message);
            });
        }
        String error = data.optString("error", null);
        if (error != null) {
            throw new Exception(error);
        }
        return new URI(data.getString("uri"));
    }
}
