package org.helioviewer.jhv.io;

import java.io.IOException;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.viewmodel.view.View;

public class LoadRemoteTask extends LoadURITask {

    private final String sourceId;
    private final long startTime;
    private final long endTime;
    private final int cadence;

    public LoadRemoteTask(String _sourceId, long _startTime, long _endTime, int _cadence) {
        super(null, null);

        sourceId = _sourceId;
        startTime = _startTime;
        endTime = _endTime;
        cadence = _cadence;
        setThreadName("MAIN--LoadRemote");
    }

    @Override
    protected View backgroundWork() {
        View view = null;
        try {
            view = APIRequestManager.requestAndOpenRemoteFile(null, sourceId, startTime, endTime, cadence, true);
        } catch (IOException e) {
            Log.error("An error occured while opening the remote file: ", e);
            Message.err("An error occured while opening the remote file: ", e.getMessage(), false);
        }
        return view;
    }

}
