package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;

import javax.swing.SwingWorker;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.viewmodel.view.View;

public class LoadURITask extends SwingWorker<View, Void> {

    private final RenderableImageLayer imageLayer;
    protected final URI uri, downloadURI;

    public LoadURITask(URI _uri, URI _downloadURI) {
        uri = _uri;
        downloadURI = _downloadURI;

        imageLayer = new RenderableImageLayer(this);
        ImageViewerGui.getRenderableContainer().addBeforeRenderable(imageLayer);
        Displayer.display(); // ensures the dummy text is displayed
    }

    @Override
    protected View doInBackground() {
        Thread.currentThread().setName("LoadURI");
        View view = null;

        try {
            view = APIRequestManager.loadView(uri, downloadURI);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file!", e);
            Message.err("An error occured while opening the remote file!", e.getMessage(), false);
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
