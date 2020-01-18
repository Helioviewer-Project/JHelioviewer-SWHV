package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.io.File;
import java.net.URI;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.threads.JHVExecutor;
import org.helioviewer.jhv.threads.JHVWorker;

import okio.Buffer;
import okio.Okio;
import okio.BufferedSource;
import okio.BufferedSink;

public class DownloadRemote extends JHVWorker<Void, Void> {

    public static Future<?> get(ImageLayer layer, APIRequest req, URI uri) {
        return JHVExecutor.cachedPool.submit(new DownloadRemote(layer, req, uri));
    }

    private static final int BUFSIZ = 1024 * 1024;

    private final URI uri;
    private final URI downloadURI;
    private final ImageLayer layer;

    private DownloadRemote(ImageLayer _layer, APIRequest req, URI _uri) {
        layer = _layer;
        uri = _uri;

        if (req == null)
            downloadURI = uri;
        else {
            URI dURI;
            try {
                dURI = new URI(req.toFileRequest());
            } catch (Exception e) {
                dURI = uri;
            }
            downloadURI = dURI;
        }
        setThreadName("MAIN--DownloadRemote");
    }

    @Nullable
    @Override
    protected Void backgroundWork() {
        File dstFile = new File(JHVDirectory.REMOTEFILES.getPath(), uri.getPath().substring(Math.max(0, uri.getPath().lastIndexOf('/')))).getAbsoluteFile();
        if (downloadURI.equals(dstFile.toURI())) // avoid self-destruction
            return null;

        boolean failed = false;
        try (NetClient nc = NetClient.of(downloadURI); BufferedSink sink = Okio.buffer(Okio.sink(dstFile))) {
            long count = 0, contentLength = nc.getContentLength();

            BufferedSource source = nc.getSource();
            long bytesRead, totalRead = 0;
            Buffer sinkBuffer = sink.getBuffer();
            while ((bytesRead = source.read(sinkBuffer, BUFSIZ)) != -1) {
                totalRead += bytesRead;
                count++;

                if (count % 128 == 0) { // approx 1MB
                    int percent = contentLength > 0 ? (int) (100. / contentLength * totalRead + .5) : -1;
                    EventQueue.invokeLater(() -> layer.progressDownloadView(percent));
                }
            }
        } catch (Exception e) {
            failed = true;
            e.printStackTrace();
        } finally {
            try {
                if (failed || isCancelled())
                    dstFile.delete();
                else { // reload from disk
                    LoadView.get(layer, dstFile.toURI());
                    EventQueue.invokeLater(() -> JHVGlobals.displayNotification(dstFile.toString()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void done() {
        layer.doneDownloadView();
    }

}
