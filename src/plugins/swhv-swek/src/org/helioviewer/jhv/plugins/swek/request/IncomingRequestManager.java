package org.helioviewer.jhv.plugins.swek.request;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.JHVEventCacheRequestHandler;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKDownloadManager;

public class IncomingRequestManager implements JHVEventCacheRequestHandler {

    @Override
    public void handleRequestForInterval(JHVEventType eventType, Interval interval) {
        SWEKDownloadManager.downloadEventType(eventType, interval);
    }

}
