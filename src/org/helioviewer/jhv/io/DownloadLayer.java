package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.EventQueueCallbackExecutor;

import okio.Buffer;
import okio.Okio;
import okio.BufferedSource;
import okio.BufferedSink;

import com.google.common.util.concurrent.FutureCallback;

public class DownloadLayer implements Callable<File> {

    public static Future<File> submit(@Nonnull ImageLayer layer, @Nonnull APIRequest req, @Nonnull URI uri) {
        URI downloadURI;
        try {
            downloadURI = new URI(req.toFileRequest());
        } catch (Exception e) { // should not happen
            return null;
        }

        File dstFile = new File(JHVDirectory.REMOTEFILES.getPath(), uri.getPath().substring(Math.max(0, uri.getPath().lastIndexOf('/')))).getAbsoluteFile();
        return EventQueueCallbackExecutor.pool.submit(new DownloadLayer(layer, dstFile, downloadURI), new Callback(layer, dstFile));
    }

    private static final int BUFSIZ = 1024 * 1024;

    private final ImageLayer layer;
    private final File dstFile;
    private final URI uri;

    private DownloadLayer(ImageLayer _layer, File _dstFile, URI _uri) {
        layer = _layer;
        dstFile = _dstFile;
        uri = _uri;
    }

    @Override
    public File call() throws Exception {
        try (NetClient nc = NetClient.of(uri); BufferedSink sink = Okio.buffer(Okio.sink(dstFile))) {
            long count = 0, contentLength = nc.getContentLength();

            BufferedSource source = nc.getSource();
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

    private static class Callback implements FutureCallback<File> {

        private final ImageLayer layer;
        private final File dstFile;

        Callback(ImageLayer _layer, File _dstFile) {
            layer = _layer;
            dstFile = _dstFile;
        }

        @Override
        public void onSuccess(File result) {
            layer.doneDownload();
            if (result != null) {
                LoadLayer.submit(layer, List.of(result.toURI()));
                EventQueue.invokeLater(() -> JHVGlobals.displayNotification(result.toString()));
            }
        }

        @Override
        public void onFailure(@Nonnull Throwable t) {
            layer.doneDownload();
            dstFile.delete();
            Log.error("DownloadRemote", t);
        }

    }

}
