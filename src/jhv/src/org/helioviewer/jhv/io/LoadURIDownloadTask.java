package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.viewmodel.view.View;

public class LoadURIDownloadTask extends LoadURITask {

    public LoadURIDownloadTask(URI _uri, URI _downloadURI) {
        super(_uri, _downloadURI);
        setThreadName("MAIN--LoadURIDownload");
    }

    @Override
    protected View backgroundWork() {
        FileDownloader filedownloader = new FileDownloader();
        URI newUri = filedownloader.downloadFromHTTP(uri, true);

        View view = null;
        try {
            view = APIRequestManager.loadView(newUri, uri);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file!", e);
            Message.err("An error occured while opening the remote file!", e.getMessage(), false);
        }
        return view;
    }

}
