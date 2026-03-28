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
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.threads.Tasks;

import okio.Buffer;
import okio.Okio;
import okio.BufferedSource;
import okio.BufferedSink;

public class DownloadLayer {

    @Nullable
    public static Future<Path> submit(@Nonnull APIRequest req, @Nonnull ImageLayer layer, @Nonnull String baseName) {
        Path dstPath = Path.of(JHVDirectory.DOWNLOADS.getPath(), baseName);
        return Tasks.submit(baseName, new LayerDownload(req, layer, dstPath), result -> onSuccess(layer, result), (logContext, t) -> onFailure(layer, t));
    }

    private static final int BUFSIZ = 1024 * 1024;

    private record LayerDownload(APIRequest req, ImageLayer layer, Path dstPath) implements Callable<Path> {
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
                            EventQueue.invokeLater(() -> layer.progressDownload(percent));
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

    private static void onSuccess(ImageLayer layer, Path result) {
        layer.doneDownload();
        LoadLayer.submit(layer, List.of(result.toUri()));
        JHVGlobals.displayNotification(result.toString());
    }

    private static void onFailure(ImageLayer layer, Throwable t) {
        layer.doneDownload();
        Log.error(t);
    }

}
