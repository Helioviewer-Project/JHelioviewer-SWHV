package org.helioviewer.jhv.plugins.swek.request;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    /**
     * Private constructor.
     */
    private IncomingRequestManager() {
        this.eventContainer = JHVEventContainer.getSingletonInstance();
        this.eventContainer.registerHandler(this);
        this.listeners = new ArrayList<IncomingRequestManagerListener>();
        this.intervalList = new ArrayList<Interval<Date>>();
        this.dateList = new ArrayList<Date>();
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
        this.listeners.add(l);
    }

    /**
     * Removes request manager listener.
     * 
     * @param l
     *            the listener to remove
     */
    public void removeRequestManagerListener(IncomingRequestManagerListener l) {
        this.listeners.remove(l);
    }

    /**
     * Gets all the requested dates
     * 
     * @return the list of all requested dates
     */
    public List<Date> getAllRequestedDates() {
        synchronized (SWEKPluginLocks.requestLock) {
            return this.dateList;
        }
    }

    /**
     * Gets all the requested intervals
     * 
     * @return the list of requested intervals
     */
    public List<Interval<Date>> getAllRequestedIntervals() {
        synchronized (SWEKPluginLocks.requestLock) {
            return this.intervalList;
        }
    }

    @Override
    public void handleRequestForDate(Date date) {
        synchronized (SWEKPluginLocks.requestLock) {
            this.dateList.add(date);
            fireNewDateRequested(date);
        }
    }

    @Override
    public void handleRequestForInterval(Date startDate, Date endDate) {
        synchronized (SWEKPluginLocks.requestLock) {
            Interval<Date> interval = new Interval<Date>(startDate, endDate);
            this.intervalList.add(interval);
            fireNewIntervalRequested(interval);
        }
    }

    @Override
    public void handleRequestForDateList(List<Date> dates) {
        synchronized (SWEKPluginLocks.requestLock) {
            for (Date date : dates) {
                this.dateList.add(date);
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
        for (IncomingRequestManagerListener l : this.listeners) {
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
        for (IncomingRequestManagerListener l : this.listeners) {
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
        for (IncomingRequestManagerListener l : this.listeners) {
            l.newRequestForDateList(dates);
        }
    }
}
