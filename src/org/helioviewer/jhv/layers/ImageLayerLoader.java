package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.app.Message;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.io.DataUri;
import org.helioviewer.jhv.io.DownloadLayer;
import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.io.NetFileCache;
import org.helioviewer.jhv.metadata.BasicMetaData;
import org.helioviewer.jhv.metadata.FitsMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.source.FITSSource;
import org.helioviewer.jhv.source.J2KSource;
import org.helioviewer.jhv.source.RasterSource;
import org.helioviewer.jhv.source.ResolutionSet;
import org.helioviewer.jhv.source.Source;
import org.helioviewer.jhv.time.JHVTime;

final class ImageLayerLoader {

    // baseName names the single source of a request load; warnings are for the user.
    record Result(Frames frames, @Nullable APIRequest request, @Nullable String baseName, List<String> warnings) {}

    private final Consumer<Result> onLoaded;
    private final Runnable onLoadFailed;

    // Owned by the EDT, including results queued after the worker has exited.
    private Thread loadThread;
    private DownloadLayer download;

    ImageLayerLoader(@Nonnull Consumer<Result> _onLoaded, @Nonnull Runnable _onLoadFailed) {
        onLoaded = _onLoaded;
        onLoadFailed = _onLoadFailed;
    }

    void load(APIRequest req) {
        load(() -> {
            APIRequest.Response response = req.resolve();
            List<String> warnings = new ArrayList<>();
            if (response.message() != null)
                warnings.add(response.message());
            return open(req, List.of(response.uri()), warnings);
        });
    }

    // Directories are expanded on the load thread, so cancelling covers the enumeration.
    void load(List<URI> uriList) {
        load(() -> open(null, FileUtils.resolveURIList(uriList)));
    }

    private void load(Callable<Result> task) {
        detachDownload();
        cancelLoad();
        Thread nextLoad = Thread.ofVirtual().name("Image-Load").unstarted(() -> {
            Thread worker = Thread.currentThread();
            try {
                Result result = task.call();
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

    void startDownload(APIRequest req, String baseName, DownloadLayer.Progress progress, Consumer<Path> onDownloaded) {
        detachDownload();
        download = new DownloadLayer(req, baseName, progress, onDownloaded);
        // The displayed movie may already be awaiting a replacement.
        if (isLoading())
            download.detach();
    }

    void cancelLoad() {
        if (loadThread != null) {
            loadThread.interrupt();
            loadThread = null;
        }
    }

    void cancelDownload() {
        if (download != null) {
            download.cancel();
            download = null;
        }
    }

    private void detachDownload() {
        if (download != null)
            download.detach();
    }

    void abolish() {
        cancelLoad();
        detachDownload();
    }

    private void finishLoad(Thread worker, @Nullable Result result, @Nullable Throwable error) {
        if (worker != loadThread) {
            if (result != null)
                result.frames().close();
            return;
        }
        loadThread = null;
        if (error == null) {
            onLoaded.accept(result);
            if (!result.warnings().isEmpty())
                Message.warn("Warning", String.join("\n", result.warnings()));
            return;
        }
        onLoadFailed.run();
        Log.errorStack(error);
        Message.err("Error getting the data", error.getMessage());
    }

    // A source and its frames in file order.
    private record Opened(Source source, DataUri dataUri, List<Frames.Frame> frames) {}

    static Result open(@Nullable APIRequest request, List<URI> uris) throws Exception {
        return open(request, uris, new ArrayList<>());
    }

    // The loader owns every source it opens until the timeline does. Warnings are reported together after the load.
    private static Result open(@Nullable APIRequest request, List<URI> uris, List<String> warnings) throws Exception {
        List<String> failures = new ArrayList<>();
        List<Opened> opened = new ArrayList<>();
        try {
            opened.addAll(openMany(request, uris, failures));
            if (opened.isEmpty())
                throw new Exception(failures.isEmpty() ? "No image files found." : String.join("\n", failures));

            List<Frames.Frame> all = new ArrayList<>();
            for (Opened o : opened)
                all.addAll(o.frames);
            all.sort(Comparator.comparingLong(f -> f.time().milli));

            // A time already present drops the later frame; a source left without frames is closed.
            List<Frames.Frame> kept = new ArrayList<>(all.size());
            List<String> dropped = new ArrayList<>();
            for (Frames.Frame frame : all) {
                if (!kept.isEmpty() && kept.getLast().time().milli == frame.time().milli)
                    dropped.add(frame.metaData().getDisplayName() + " " + frame.time());
                else
                    kept.add(frame);
            }
            warnings.addAll(failures);
            if (!dropped.isEmpty()) {
                warnings.add("Skipped " + dropped.size() + " frame(s) with a time already present:\n" + String.join("\n", dropped));
                Set<Source> retained = new HashSet<>();
                for (Frames.Frame frame : kept)
                    retained.add(frame.source());
                for (Opened o : opened) {
                    if (!retained.contains(o.source))
                        o.source.close();
                }
            }
            String baseName = opened.size() == 1 ? opened.getFirst().dataUri.baseName() : null;
            return new Result(new Frames(kept), request, baseName, warnings);
        } catch (Throwable t) {
            for (Opened o : opened) {
                try {
                    o.source.close();
                } catch (Exception e) {
                    Log.error(e);
                }
            }
            throw t;
        }
    }

    // Several URIs tolerate failures of some; one URI fails the load.
    private static List<Opened> openMany(@Nullable APIRequest request, List<URI> uris, List<String> failures) throws Exception {
        if (uris.size() == 1)
            return openUri(request, uris.getFirst(), failures);
        Thread worker = Thread.currentThread();
        // Keep JPIP initialization on the interruptible loader thread.
        boolean jpip = uris.stream().anyMatch(uri -> "jpip".equalsIgnoreCase(uri.getScheme()) || "jpips".equalsIgnoreCase(uri.getScheme()));
        return (jpip ? uris.stream() : uris.parallelStream()).flatMap(uri -> {
            if (worker.isInterrupted())
                return Stream.empty();
            try {
                return openUri(request, uri, failures).stream();
            } catch (Exception e) {
                Log.warn(uri.toString(), e);
                synchronized (failures) {
                    failures.add(e.getMessage());
                }
                return Stream.empty();
            }
        }).toList();
    }

    private static List<Opened> openUri(@Nullable APIRequest request, URI uri, List<String> failures) throws Exception {
        DataUri dataUri = NetFileCache.get(uri);
        if (dataUri.format() == DataUri.Format.ZIP)
            return openMany(null, FileUtils.unZip(dataUri.uri()), failures);

        Source source = null;
        try {
            source = switch (dataUri.format()) {
                case JPIP -> J2KSource.open(dataUri.uri());
                case JP2, JPX -> new J2KSource(dataUri.file().toPath());
                case FITS -> new FITSSource(dataUri.file());
                case PNG, JPEG -> new RasterSource(dataUri.file());
                default -> throw new Exception("Unknown image type");
            };
            List<Frames.Frame> frames = frames(source, dataUri);
            String[] cacheKey = new String[frames.size()];
            if (request != null) {
                for (int i = 0; i < cacheKey.length; i++) {
                    Frames.Frame frame = frames.get(i);
                    if (frame.metaData() instanceof FitsMetaData metadata)
                        cacheKey[i] = request.sourceId() + "+" + metadata.getCacheTimestamp();
                }
            }
            source.start(cacheKey);
            return List.of(new Opened(source, dataUri, frames));
        } catch (Exception e) {
            if (source != null)
                source.close();
            throw new Exception(e.getMessage() + ": " + dataUri, e);
        }
    }

    // Interprets the metadata once per frame and requires increasing times.
    private static List<Frames.Frame> frames(Source source, DataUri dataUri) throws Exception {
        int count = source.frames();
        List<Frames.Frame> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String xml;
            MetaData m;
            try {
                xml = source.xml(i);
                if (xml == null)
                    throw new Exception("Missing XML metadata");
                m = new FitsMetaData(new XMLMetaDataContainer(xml), dataUri.sourceUri());
            } catch (Exception e) {
                if (count > 1)
                    throw new Exception("Frame " + i + " has no usable Helioviewer metadata: " + e.getMessage(), e);
                xml = Frames.EMPTY_METAXML;
                ResolutionSet.Level level = source.levels(0).getLevel(0);
                m = new BasicMetaData(level.width(), level.height(), dataUri.baseName(), dataUri.sourceUri());
                Log.warn("Helioviewer metadata missing for " + dataUri.baseName() + " frame " + i, e);
            }
            JHVTime time = m.getViewpoint().time;
            if (i > 0) {
                JHVTime previous = list.get(i - 1).time();
                if (time.milli <= previous.milli)
                    throw new Exception((time.milli == previous.milli ? "Duplicate frame timestamp" : "Out-of-order frame timestamp")
                            + "\nFrame " + (i - 1) + ": " + previous + "\nFrame " + i + ": " + time);
            }
            list.add(new Frames.Frame(time, m, xml, source, i));
        }
        return list;
    }

}
