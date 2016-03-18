package org.helioviewer.jhv.plugins.swek.request;

import java.util.Date;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainerRequestHandler;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKDownloadManager;

public class IncomingRequestManager implements JHVEventContainerRequestHandler {

    /** The singleton instance */
    private static IncomingRequestManager instance;

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance
     */
    public static IncomingRequestManager getSingletonInstance() {
        if (instance == null) {
            instance = new IncomingRequestManager();
        }
        return instance;
    }

    @Override
    public void handleRequestForInterval(JHVEventType eventType, Date startDate, Date endDate) {
        Interval<Date> interval = new Interval<Date>(startDate, endDate);
        SWEKDownloadManager.getSingletonInstance().newRequestForInterval(eventType, interval);
    }
}
