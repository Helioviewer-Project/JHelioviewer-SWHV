package org.helioviewer.jhv.plugins.swek.request;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.JHVEventContainerRequestHandler;
import org.helioviewer.jhv.plugins.swek.SWEKPluginLocks;

public class IncomingRequestManager implements JHVEventContainerRequestHandler {

    /** The singleton instance */
    private static IncomingRequestManager instance;

    /** Local instance of the JHVEventContainer */
    private final JHVEventContainer eventContainer;

    /** The listeners */
    private final List<IncomingRequestManagerListener> listeners;

    /** List of requested intervals */
    private final Map<Long, Interval<Date>> intervalList;

    /** List of requested dates */
    private final Map<Long, List<Date>> dateList;

    /**  */
    private final Map<Date, Set<Date>> uniqueInterval;

    /**
     * Private constructor.
     */
    private IncomingRequestManager() {
        eventContainer = JHVEventContainer.getSingletonInstance();
        eventContainer.registerHandler(this);
        listeners = new ArrayList<IncomingRequestManagerListener>();
        intervalList = new HashMap<Long, Interval<Date>>();
        dateList = new HashMap<Long, List<Date>>();
        uniqueInterval = new HashMap<Date, Set<Date>>();
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
    public Map<Long, List<Date>> getAllRequestedDates() {
        synchronized (SWEKPluginLocks.requestLock) {
            return dateList;
        }
    }

    /**
     * Gets all the requested intervals
     * 
     * @return the list of requested intervals
     */
    public Map<Long, Interval<Date>> getAllRequestedIntervals() {
        synchronized (SWEKPluginLocks.requestLock) {
            return intervalList;
        }
    }

    @Override
    public void handleRequestForDate(Date date, Long requestID) {
        synchronized (SWEKPluginLocks.requestLock) {
            ArrayList<Date> dates = new ArrayList<Date>();
            dates.add(date);
            dateList.put(requestID, dates);
            fireNewDateRequested(date, requestID);
        }
    }

    @Override
    public void handleRequestForInterval(Date startDate, Date endDate, Long requestID) {
        synchronized (SWEKPluginLocks.requestLock) {
            if (addToUniqueInterval(startDate, endDate)) {
                Interval<Date> interval = new Interval<Date>(startDate, endDate);
                intervalList.put(requestID, interval);
                fireNewIntervalRequested(interval, requestID);
            }
        }
    }

    @Override
    public void handleRequestForDateList(List<Date> dates, Long requestID) {
        synchronized (SWEKPluginLocks.requestLock) {
            dateList.put(requestID, dates);
            firedNewDateListRequested(dates, requestID);
        }

    }

    @Override
    public void removeRequestID(Long requestID) {
        synchronized (SWEKPluginLocks.requestLock) {
            if (dateList.containsKey(requestID)) {
                dateList.remove(requestID);
                fireStopRequest(requestID);
            } else if (intervalList.containsKey(requestID)) {
                intervalList.remove(requestID);
                fireStopRequest(requestID);
            }
        }
    }

    private void fireStopRequest(Long requestID) {
        for (IncomingRequestManagerListener l : listeners) {
            l.stopRequest(requestID);
        }
    }

    /**
     * Informs the listeners about a new date that was requested
     * 
     * @param date
     *            the date that was requested
     * @param requestID
     */
    private void fireNewDateRequested(Date date, Long requestID) {
        for (IncomingRequestManagerListener l : listeners) {
            l.newRequestForDate(date, requestID);
        }
    }

    /**
     * Informs the listeners about a new interval that was requested.
     * 
     * @param interval
     *            interval that was requested
     * @param requestID
     */
    private void fireNewIntervalRequested(Interval<Date> interval, Long requestID) {
        for (IncomingRequestManagerListener l : listeners) {
            l.newRequestForInterval(interval, requestID);
        }
    }

    /**
     * Informs the listeners about a new date list that was requested.
     * 
     * @param dates
     *            list of dates that was requested
     * @param requestID
     */
    private void firedNewDateListRequested(List<Date> dates, Long requestID) {
        for (IncomingRequestManagerListener l : listeners) {
            l.newRequestForDateList(dates, requestID);
        }
    }

    /**
     * Adds the start and end time to the unique start and end times.
     * 
     * @param startDate
     *            the start date to add
     * @param endDate
     *            the end date to add
     * @return true if the start and end date were added, false if the interval
     *         was already in the list.
     */
    private boolean addToUniqueInterval(Date startDate, Date endDate) {
        Set<Date> uniqueEndDates = new HashSet<Date>();
        if (uniqueInterval.containsKey(startDate)) {
            uniqueEndDates = uniqueInterval.get(startDate);
            if (uniqueEndDates.contains(endDate)) {
                return false;
            }
        }
        uniqueEndDates.add(endDate);
        uniqueInterval.put(startDate, uniqueEndDates);
        return true;
    }

}
