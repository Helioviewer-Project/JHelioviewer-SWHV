package org.helioviewer.jhv.io;

import java.io.File;
import java.net.URI;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.view.View;

import okio.Okio;
import okio.BufferedSink;

public class DownloadViewTask extends JHVWorker<Void, Void> {

    private final URI uri;
    private final URI downloadURI;
    private final ImageLayer layer;

    public DownloadViewTask(View view) {
        layer = view.getImageLayer();
        uri = view.getURI();

        APIRequest req = view.getAPIRequest();
        downloadURI = req == null ? uri : req.fileRequest;

        setThreadName("MAIN--DownloadView");
    }

    @Override
    protected Void backgroundWork() {
        File dstFile = new File(JHVDirectory.REMOTEFILES.getPath(), uri.getPath().substring(Math.max(0, uri.getPath().lastIndexOf('/')))).getAbsoluteFile();
        if (downloadURI.equals(dstFile.toURI())) // avoid self-destruction
            return null;

        boolean failed = false;
        try (NetClient nc = NetClient.of(downloadURI.toURL());
             BufferedSink sink = Okio.buffer(Okio.sink(dstFile))) {
            sink.writeAll(nc.getSource());
        } catch (Exception e) {
            failed = true;
            e.printStackTrace();
        } finally {
            try {
                if (failed || isCancelled())
                    dstFile.delete();
                else { // reload from disk
                    LoadURITask uriTask = new LoadURITask(layer, dstFile.toURI());
                    JHVGlobals.getExecutorService().execute(uriTask);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void done() {
        layer.getOptionsPanel().getRunningDifferencePanel().done();
    }

}
