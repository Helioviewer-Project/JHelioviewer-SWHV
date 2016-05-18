package org.helioviewer.jhv.io;

import java.io.IOException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.viewmodel.view.View;

public class LoadRemoteTask extends LoadURITask {

    private final String observatory;
    private final String instrument;
    private final String measurement;
    private final String detector;
    private final long startTime;
    private final long endTime;
    private final int cadence;

    public LoadRemoteTask(String _observatory, String _instrument, String _measurement, String _detector, long _startTime, long _endTime, int _cadence) {
        super(null, null);

        observatory = _observatory;
        instrument = _instrument;
        measurement = _measurement;
        detector = _detector;
        startTime = _startTime;
        endTime = _endTime;
        cadence = _cadence;
        setThreadName("MAIN--LoadRemote");
    }

    @Override
    protected View backgroundWork() {
        View view = null;
        try {
            view = APIRequestManager.requestAndOpenRemoteFile(null, observatory, instrument, measurement, detector, startTime, endTime, cadence, true);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file! ", e);
            Message.err("An error occured while opening the remote file! ", e.getMessage(), false);
        }
        return view;
    }

}
