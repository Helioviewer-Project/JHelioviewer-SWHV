package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.Task;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public final class DownloadLayer {

    public interface Progress {
        void progress(int percent);

        void success(String result);

        void done();
    }

    // Owned by the EDT. Detaching the layer leaves the file download running.
    private final Future<Path> task;
    private @Nullable Consumer<Path> onDownloaded;

    public DownloadLayer(@Nonnull APIRequest req, @Nonnull String baseName, @Nonnull Progress progress, @Nonnull Consumer<Path> _onDownloaded) {
        onDownloaded = _onDownloaded;
        Path dstPath = Path.of(Directories.DOWNLOADS.getPath(), baseName);
        task = Task.submitBackground(
                new LayerDownload(req, progress, dstPath),
                result -> onSuccess(progress, result),
                t -> onFailure(progress, t));
    }

    public void detach() {
        onDownloaded = null;
    }

    public void cancel() {
        detach();
        task.cancel(true);
    }

    private static final int BUFSIZ = 1024 * 1024;
    private static final long PROGRESS_INTERVAL = 8L * BUFSIZ;

    private record LayerDownload(APIRequest req, Progress progress, Path dstPath) implements Callable<Path> {
        @Override
        public Path call() throws Exception {
            URI uri = new URI(req.toFileRequest());
            Path partial = Files.createTempFile(dstPath.getParent(), "jhv-", ".part");
            try {
                try (NetClient nc = NetClient.of(uri, false, NetClient.NetCache.BYPASS); BufferedSource source = nc.getSource(); BufferedSink sink = Okio.buffer(Okio.sink(partial))) {
                    long contentLength = nc.getContentLength();
                    long bytesRead, totalRead = 0, lastProgress = 0;
                    Buffer sinkBuffer = sink.getBuffer();
                    while ((bytesRead = source.read(sinkBuffer, BUFSIZ)) != -1) {
                        // stream out buffered data during download to avoid large memory growth
                        sink.emitCompleteSegments();

                        totalRead += bytesRead;
                        if (totalRead - lastProgress >= PROGRESS_INTERVAL) {
                            lastProgress = totalRead;
                            int percent = contentLength > 0 ? (int) (100. / contentLength * totalRead + .5) : -1;
                            EventQueue.invokeLater(() -> progress.progress(percent));
                        }
                    }
                }
                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedIOException("Download canceled");
                // Publish only after the download and its output stream have finished.
                Files.move(partial, dstPath, StandardCopyOption.ATOMIC_MOVE);
                return dstPath;
            } catch (Exception e) {
                try {
                    Files.deleteIfExists(partial);
                } catch (Exception e2) {
                    Log.error(e2);
                }
                throw e;
            }
        }
    }

    private void onSuccess(Progress progress, Path result) {
        progress.done();
        progress.success(result.toString());
        Consumer<Path> callback = onDownloaded;
        detach();
        if (callback != null)
            callback.accept(result);
    }

    private void onFailure(Progress progress, Throwable t) {
        detach();
        progress.done();
        if (AppThread.isInterrupted(t)) {
            Log.warn(t);
            return;
        }
        Log.error(t);
    }

}
