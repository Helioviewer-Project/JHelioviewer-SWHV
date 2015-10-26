package org.helioviewer.jhv.io;

import java.io.IOException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.viewmodel.view.View;

public class LoadRemoteTask extends LoadURITask {

    private final boolean image;
    private final int cadence;
    private final String instrument;
    private final String measurement;
    private final String detector;
    private final String observation;
    private final String starttime;
    private final String endtime;

    public LoadRemoteTask(boolean _image, int _cadence, String _starttime, String _endtime, String _observation, String _instrument, String _measurement, String _detector) {
        super(null, null);

        image = _image;
        cadence = _cadence;
        starttime = _starttime;
        endtime = _endtime;
        observation = _observation;
        instrument = _instrument;
        measurement = _measurement;
        detector = _detector;
        setThreadName("MAIN--LoadRemote");
    }

    @Override
    protected View backgroundWork() {
        View view = null;
        try {
            if (image)
                view = APIRequestManager.requestAndOpenRemoteFile(null, starttime, "", observation, instrument, measurement, detector, true);
            else
                view = APIRequestManager.requestAndOpenRemoteFile(Integer.toString(cadence), starttime, endtime, observation, instrument, measurement, detector, true);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file!", e);
            Message.err("An error occured while opening the remote file!", e.getMessage(), false);
        }
        return view;
    }

}
