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
    private final List<Interval<Date>> intervalList;

    /** List of requested dates */
    private final List<Date> dateList;

    /**  */
    private final Map<Date, Set<Date>> uniqueInterval;

    /**
     * Private constructor.
     */
    private IncomingRequestManager() {
        eventContainer = JHVEventContainer.getSingletonInstance();
        eventContainer.registerHandler(this);
        listeners = new ArrayList<IncomingRequestManagerListener>();
        intervalList = new ArrayList<Interval<Date>>();
        dateList = new ArrayList<Date>();
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
    public List<Date> getAllRequestedDates() {
        synchronized (SWEKPluginLocks.requestLock) {
            return dateList;
        }
    }

    /**
     * Gets all the requested intervals
     * 
     * @return the list of requested intervals
     */
    public List<Interval<Date>> getAllRequestedIntervals() {
        synchronized (SWEKPluginLocks.requestLock) {
            List<Interval<Date>> tempIntervals = new ArrayList<Interval<Date>>();
            tempIntervals.addAll(intervalList);
            return tempIntervals;
        }
    }

    @Override
    public void handleRequestForDate(Date date) {
        synchronized (SWEKPluginLocks.requestLock) {
            dateList.add(date);
            fireNewDateRequested(date);
        }
    }

    @Override
    public void handleRequestForInterval(Date startDate, Date endDate) {
        synchronized (SWEKPluginLocks.requestLock) {
            if (addToUniqueInterval(startDate, endDate)) {
                Interval<Date> interval = new Interval<Date>(startDate, endDate);
                intervalList.add(interval);
                fireNewIntervalRequested(interval);
            }
        }
    }

    @Override
    public void handleRequestForDateList(List<Date> dates) {
        synchronized (SWEKPluginLocks.requestLock) {
            for (Date date : dates) {
                dateList.add(date);
            }
            firedNewDateListRequested(dates);
        }

    }

    /**
     * Informs the listeners about a new date that was requested
     * 
     * @param date
     *            the date that was requested
     */
    private void fireNewDateRequested(Date date) {
        for (IncomingRequestManagerListener l : listeners) {
            l.newRequestForDate(date);
        }
    }

    /**
     * Informs the listeners about a new interval that was requested.
     * 
     * @param interval
     *            interval that was requested
     */
    private void fireNewIntervalRequested(Interval<Date> interval) {
        for (IncomingRequestManagerListener l : listeners) {
            l.newRequestForInterval(interval);
        }
    }

    /**
     * Informs the listeners about a new date list that was requested.
     * 
     * @param dates
     *            list of dates that was requested
     */
    private void firedNewDateListRequested(List<Date> dates) {
        for (IncomingRequestManagerListener l : listeners) {
            l.newRequestForDateList(dates);
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
