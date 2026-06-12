package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.thread.JHVThread;
import org.helioviewer.jhv.thread.Tasks;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class DownloadLayer {

    public interface Progress {
        void progress(int percent);

        void success(String result);

        void done();
    }

    @Nullable
    public static Future<Path> submit(@Nonnull APIRequest req, @Nonnull ImageLayer layer, @Nonnull String baseName, @Nonnull Progress progress) {
        Path dstPath = Path.of(JHVDirectory.DOWNLOADS.getPath(), baseName);
        return Tasks.submit(baseName,
                new LayerDownload(req, progress, dstPath),
                result -> onSuccess(layer, progress, result),
                (logContext, t) -> onFailure(progress, t));
    }

    private static final int BUFSIZ = 1024 * 1024;

    private record LayerDownload(APIRequest req, Progress progress, Path dstPath) implements Callable<Path> {
        @Override
        public Path call() throws Exception {
            URI uri = new URI(req.toFileRequest());
            try {
                try (NetClient nc = NetClient.of(uri); BufferedSource source = nc.getSource(); BufferedSink sink = Okio.buffer(Okio.sink(dstPath))) {
                    long count = 0, contentLength = nc.getContentLength();
                    long bytesRead, totalRead = 0;
                    Buffer sinkBuffer = sink.getBuffer();
                    while ((bytesRead = source.read(sinkBuffer, BUFSIZ)) != -1) {
                        // stream out buffered data during download to avoid large memory growth
                        sink.emitCompleteSegments();

                        totalRead += bytesRead;
                        count++;
                        if (count % 8 == 0) { // approx 8MB with BUFSIZ=1MB
                            int percent = contentLength > 0 ? (int) (100. / contentLength * totalRead + .5) : -1;
                            EventQueue.invokeLater(() -> progress.progress(percent));
                        }
                    }
                    sink.flush();
                }
                return dstPath;
            } catch (Exception e) {
                try {
                    Files.deleteIfExists(dstPath);
                } catch (Exception e2) {
                    Log.error(e2);
                }
                throw e;
            }
        }
    }

    private static void onSuccess(ImageLayer layer, Progress progress, Path result) {
        progress.done();
        layer.load(List.of(result.toUri()));
        progress.success(result.toString());
    }

    private static void onFailure(Progress progress, Throwable t) {
        progress.done();
        if (JHVThread.isInterrupted(t)) {
            Log.warn(t);
            return;
        }
        Log.error(t);
    }

}
