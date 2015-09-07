package org.helioviewer.jhv.io;

import java.io.IOException;

import javax.swing.SwingWorker;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.components.RenderableDummy;
import org.helioviewer.viewmodel.view.View;

public class LoadRemoteTask extends SwingWorker<View, Void> {

    private final RenderableDummy dummy;
    private final boolean image;
    private final boolean wasPlaying;
    private final String cadence;
    private final String instrument;
    private final String measurement;
    private final String detector;
    private final String observation;
    private final String starttime;
    private final String endtime;

    public LoadRemoteTask(boolean _image, String cadence, String starttime, String endtime, String observation, String instrument, String measurement, String detector) {
        image = _image;
        this.cadence = cadence;
        this.starttime = starttime;
        this.endtime = endtime;
        this.observation = observation;
        this.instrument = instrument;
        this.measurement = measurement;
        this.detector = detector;

        // due to CacheStrategy, layers load significantly faster when movie is paused
        wasPlaying = Layers.isMoviePlaying();
        if (wasPlaying)
            Layers.pauseMovie();

        dummy = new RenderableDummy();
        ImageViewerGui.getRenderableContainer().addBeforeRenderable(dummy);
    }

    @Override
    protected View doInBackground() {
        Thread.currentThread().setName("LoadRemote");
        View view = null;

        try {
            if (image)
                view = APIRequestManager.requestAndOpenRemoteFile(null, starttime, "", observation, instrument, measurement, detector, true);
            else
                view = APIRequestManager.requestAndOpenRemoteFile(cadence, starttime, endtime, observation, instrument, measurement, detector, true);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file!", e);
            Message.err("An error occured while opening the remote file!", e.getMessage(), false);
        }

        return view;
    }

    @Override
    protected void done() {
        ImageViewerGui.getRenderableContainer().removeRenderable(dummy);
        try {
            Layers.addLayer(get());
            if (wasPlaying)
                Layers.playMovie();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
