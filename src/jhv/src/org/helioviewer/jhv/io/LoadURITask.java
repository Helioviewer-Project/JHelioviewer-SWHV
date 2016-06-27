package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.viewmodel.view.View;

public class LoadURITask extends JHVWorker<View, Void> {

    private final ImageLayer imageLayer;
    private final URI downloadURI;
    protected final URI uri;

    public LoadURITask(ImageLayer _imageLayer, URI _uri, URI _downloadURI) {
        uri = _uri;
        downloadURI = _downloadURI;

        if (_imageLayer == null) {
            imageLayer = new ImageLayer(this);
            ImageViewerGui.getRenderableContainer().addBeforeRenderable(imageLayer);
            Displayer.display(); // ensures the dummy text is displayed
        } else {
            imageLayer = _imageLayer;
            imageLayer.unsetView();
        }
        setThreadName("MAIN--LoadURI");
    }

    @Override
    protected View backgroundWork() {
        View view = null;
        try {
            view = APIRequestManager.loadView(uri, downloadURI, null);
        } catch (IOException e) {
            Log.error("An error occurred while opening the remote file: ", e);
            Message.err("An error occurred while opening the remote file: ", e.getMessage(), false);
        }
        return view;
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            try {
                View view = get();
                if (view != null) {
                    imageLayer.setView(view);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            ImageViewerGui.getRenderableContainer().removeRenderable(imageLayer);
        }
    }

}
