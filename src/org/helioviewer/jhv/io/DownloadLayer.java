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
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import okio.Buffer;
import okio.Okio;
import okio.BufferedSource;
import okio.BufferedSink;

import com.google.common.util.concurrent.FutureCallback;

public class DownloadLayer {

    @Nullable
    public static Future<Void> submit(@Nonnull APIRequest req, @Nonnull ImageLayer layer, @Nonnull URI dst) {
        String path = dst.getPath();
        Path dstPath = Path.of(JHVDirectory.DOWNLOADS.getPath(), path.substring(Math.max(0, path.lastIndexOf('/'))));
        return EventQueueCallbackExecutor.pool.submit(new LayerDownload(req, layer, dstPath), new Callback(layer, dstPath));
    }

    private static final int BUFSIZ = 1024 * 1024;

    private record LayerDownload(APIRequest req, ImageLayer layer, Path dstPath) implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            URI uri = new URI(req.toFileRequest());
            try (NetClient nc = NetClient.of(uri); BufferedSource source = nc.getSource(); BufferedSink sink = Okio.buffer(Okio.sink(dstPath))) {
                long count = 0, contentLength = nc.getContentLength();
                long bytesRead, totalRead = 0;
                Buffer sinkBuffer = sink.getBuffer();
                while ((bytesRead = source.read(sinkBuffer, BUFSIZ)) != -1) {
                    totalRead += bytesRead;
                    count++;

                    if (count % 128 == 0) { // approx 1MB
                        int percent = contentLength > 0 ? (int) (100. / contentLength * totalRead + .5) : -1;
                        EventQueue.invokeLater(() -> layer.progressDownload(percent));
                    }
                }
            }
            return null;
        }
    }

    private record Callback(ImageLayer layer, Path dstPath) implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
            layer.doneDownload();
            LoadLayer.submit(layer, List.of(dstPath.toUri()), false);
            JHVGlobals.displayNotification(dstPath.toString());
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            layer.doneDownload();
            Log.error(t);
            try {
                Files.delete(dstPath);
            } catch(Exception e) {
                Log.error(e);
            }
        }

    }

}
