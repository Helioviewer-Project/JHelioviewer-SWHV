package org.helioviewer.jhv.io;

import java.io.IOException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.viewmodel.view.View;

public class LoadRemoteTask extends LoadURITask {

    private final int cadence;
    private final long startTime;
    private final long endTime;
    private final String instrument;
    private final String measurement;
    private final String detector;
    private final String observation;

    public LoadRemoteTask(int _cadence, long _startTime, long _endTime, String _observation, String _instrument, String _measurement, String _detector) {
        super(null, null);

        cadence = _cadence;
        startTime = _startTime;
        endTime = _endTime;
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
            view = APIRequestManager.requestAndOpenRemoteFile(null, Integer.toString(cadence), startTime, endTime, observation, instrument, measurement, detector, true);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file! ", e);
            Message.err("An error occured while opening the remote file! ", e.getMessage(), false);
        }
        return view;
    }

}
