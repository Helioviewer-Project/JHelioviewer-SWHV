package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log2;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import okio.Buffer;
import okio.Okio;
import okio.BufferedSource;
import okio.BufferedSink;

import com.google.common.util.concurrent.FutureCallback;

public record DownloadLayer(ImageLayer layer, File dstFile, URI uri) implements Callable<File> {

    @Nullable
    public static Future<File> submit(@Nonnull ImageLayer layer, @Nonnull APIRequest req, @Nonnull URI uri) {
        URI downloadURI;
        try {
            downloadURI = new URI(req.toFileRequest());
        } catch (Exception e) { // should not happen
            return null;
        }

        String uriPath = uri.getPath();
        File dstFile = new File(JHVDirectory.REMOTEFILES.getPath(), uriPath.substring(Math.max(0, uriPath.lastIndexOf('/')))).getAbsoluteFile();
        return EventQueueCallbackExecutor.pool.submit(new DownloadLayer(layer, dstFile, downloadURI), new Callback(layer, dstFile));
    }

    private static final int BUFSIZ = 1024 * 1024;

    @Override
    public File call() throws Exception {
        try (NetClient nc = NetClient.of(uri); BufferedSource source = nc.getSource(); BufferedSink sink = Okio.buffer(Okio.sink(dstFile))) {
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
        return dstFile;
    }

    private record Callback(ImageLayer layer, File dstFile) implements FutureCallback<File> {

        @Override
        public void onSuccess(File result) {
            layer.doneDownload();
            if (result != null) {
                LoadLayer.submit(layer, List.of(result.toURI()), false);
                EventQueue.invokeLater(() -> JHVGlobals.displayNotification(result.toString()));
            }
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            layer.doneDownload();
            dstFile.delete();
            Log2.error(t);
        }

    }

}
