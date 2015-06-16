package org.helioviewer.jhv.plugins.swek.request;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainerRequestHandler;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;

public class IncomingRequestManager implements JHVEventContainerRequestHandler {

    /** The singleton instance */
    private static IncomingRequestManager instance;

    /** The listeners */
    private final List<IncomingRequestManagerListener> listeners;

    /** List of requested dates */
    private final List<Date> dateList;

    /**
     * Private constructor.
     */
    private IncomingRequestManager() {
        listeners = new ArrayList<IncomingRequestManagerListener>();
        dateList = new ArrayList<Date>();
    }

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

    /**
     * Add a request manager listener.
     *
     * @param l
     *            the listener to add
     */
    public void addRequestManagerListener(IncomingRequestManagerListener l) {
        listeners.add(l);
    }

    /**
     * Removes request manager listener.
     *
     * @param l
     *            the listener to remove
     */
    public void removeRequestManagerListener(IncomingRequestManagerListener l) {
        listeners.remove(l);
    }

    /**
     * Gets all the requested dates
     *
     * @return the list of all requested dates
     */
    public List<Date> getAllRequestedDates() {
        return dateList;

    }

    @Override
    public void handleRequestForInterval(JHVEventType eventType, Date startDate, Date endDate) {
        Interval<Date> interval = new Interval<Date>(startDate, endDate);
        fireNewIntervalRequested(eventType, interval);
    }

    /**
     * Informs the listeners about a new interval that was requested.
     *
     * @param eventType
     *
     * @param interval
     *            interval that was requested
     * @param requestID
     */
    private void fireNewIntervalRequested(JHVEventType eventType, Interval<Date> interval) {
        for (IncomingRequestManagerListener l : listeners) {
            l.newRequestForInterval(eventType, interval);
        }
    }

}
