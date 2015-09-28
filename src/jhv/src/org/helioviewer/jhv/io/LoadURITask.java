package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;

import javax.swing.SwingWorker;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableDummy;
import org.helioviewer.viewmodel.view.View;

public class LoadURITask extends SwingWorker<View, Void> {

    private final RenderableDummy dummy;
    protected final URI uri, downloadURI;

    public LoadURITask(URI _uri, URI _downloadURI) {
        uri = _uri;
        downloadURI = _downloadURI;

        dummy = new RenderableDummy(this);
        ImageViewerGui.getRenderableContainer().addBeforeRenderable(dummy);
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
            ImageViewerGui.getRenderableContainer().removeRenderable(dummy);
            try {
                Layers.addLayer(get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
