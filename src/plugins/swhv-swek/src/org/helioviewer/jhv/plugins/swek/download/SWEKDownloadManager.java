package org.helioviewer.jhv.plugins.swek.download;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.plugins.swek.SWEKPluginLocks;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModelListener;
import org.helioviewer.jhv.plugins.swek.request.IncomingRequestManager;
import org.helioviewer.jhv.plugins.swek.request.IncomingRequestManagerListener;
import org.helioviewer.jhv.plugins.swek.settings.SWEKProperties;

/**
 * 
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKDownloadManager implements DownloadWorkerListener, IncomingRequestManagerListener, EventTypePanelModelListener {

    /** Singleton instance of the SWE */
    private static SWEKDownloadManager instance;

    /** Threadpool for downloading events */
    private final ExecutorService downloadEventPool;

    /** The properties specific to the swek plugin */
    private final Properties swekProperties;

    /** Map holding the download workers order by event type and date */
    private final Map<SWEKEventType, Map<Date, DownloadWorker>> dwMap;

    /** Map with all the finished and busy downloads */
    private final Map<SWEKEventType, Map<SWEKSource, Set<Date>>> busyAndFinishedJobs;

    /** Map with all the finished and busy interval downloads */
    private final Map<SWEKEventType, Map<SWEKSource, Map<Date, Set<Date>>>> busyAndFinishedIntervalJobs;

    /** Map holding the active event types and its sources */
    private final Map<SWEKEventType, Set<SWEKSource>> activeEventTypes;

    /** Local instance of the request manager */
    private final IncomingRequestManager requestManager;

    /** Local instance of the event container */
    private final JHVEventContainer eventContainer;

    /**
     * private constructor of the SWEKDownloadManager
     */
    private SWEKDownloadManager() {
        swekProperties = SWEKProperties.getSingletonInstance().getSWEKProperties();
        downloadEventPool = Executors.newFixedThreadPool(Integer.parseInt(swekProperties.getProperty("plugin.swek.numberofthreads")));
        dwMap = new HashMap<SWEKEventType, Map<Date, DownloadWorker>>();
        activeEventTypes = new HashMap<SWEKEventType, Set<SWEKSource>>();
        requestManager = IncomingRequestManager.getSingletonInstance();
        busyAndFinishedJobs = new HashMap<SWEKEventType, Map<SWEKSource, Set<Date>>>();
        busyAndFinishedIntervalJobs = new HashMap<SWEKEventType, Map<SWEKSource, Map<Date, Set<Date>>>>();
        requestManager.addRequestManagerListener(this);
        eventContainer = JHVEventContainer.getSingletonInstance();
    }

    /**
     * Gets the singleton instance of the SWEKDownloadManager
     * 
     * @return The singleton instance
     */
    public static SWEKDownloadManager getSingletonInstance() {
        if (instance == null) {
            instance = new SWEKDownloadManager();
        }
        return instance;
    }

    /**
     * Stops downloading the event type for every source of the event type.
     * 
     * @param eventType
     *            the event type for which to stop downloads
     */
    public void stopDownloadingEventType(SWEKEventType eventType) {
        synchronized (SWEKPluginLocks.downloadLock) {
            Map<Date, DownloadWorker> dwMapOnDate = dwMap.get(eventType);
            for (DownloadWorker dw : dwMapOnDate.values()) {
                dw.stopWorker();
            }
        }
        removeFromBusyAndFinishedJobs(eventType);
        removeFromBusyAndFinishedIntervalJobs(eventType);
        for (SWEKSupplier supplier : eventType.getSuppliers()) {
            eventContainer.removeEvents(new JHVSWEKEventType(eventType.getEventName(), supplier.getSource().getSourceName(), supplier
                    .getSource().getProviderName()));
        }
    }

    /**
     * Stops downloading the event type for the given source.
     * 
     * @param eventType
     *            the event type for which to stop the downloads
     * @param source
     *            the source for which to stop the downloads
     */
    public void stopDownloadingEventType(SWEKEventType eventType, SWEKSource source) {
        synchronized (SWEKPluginLocks.downloadLock) {
            if (dwMap.containsKey(eventType)) {
                Map<Date, DownloadWorker> dwMapOnDate = dwMap.get(eventType);
                for (DownloadWorker dw : dwMapOnDate.values()) {
                    if (dw.getSource().equals(source)) {
                        dw.stopWorker();
                    }
                }
            }
            removeFromBusyAndFinishedJobs(eventType, source);
            removeFromBusyAndFinishedIntervalJobs(eventType, source);
            eventContainer.removeEvents(new JHVSWEKEventType(eventType.getEventName(), source.getSourceName(), source.getProviderName()));
        }
    }

    @Override
    public void workerStarted(DownloadWorker worker) {
        Log.debug("Worker " + worker + " is started");
    }

    @Override
    public void workerForcedToStop(DownloadWorker worker) {
        Log.debug("Worker " + worker + " is forced stopped");
        synchronized (SWEKPluginLocks.downloadLock) {
            removeWorkerFromMap(worker);
            removeFromBusyAndFinishedJobs(worker.getEventType(), worker.getSource(), worker.getDownloadStartDate());
        }
    }

    @Override
    public void workerFinished(DownloadWorker worker) {
        Log.debug("Worker " + worker + " is finished");
        synchronized (SWEKPluginLocks.downloadLock) {
            removeWorkerFromMap(worker);
        }
    }

    @Override
    public void newEventTypeAndSourceActive(SWEKEventType eventType, SWEKSource swekSource) {
        addEventTypeToActiveEventTypeMap(eventType, swekSource);
        downloadForAllDates(eventType, swekSource);
    }

    @Override
    public void newEventTypeAndSourceInActive(SWEKEventType eventType, SWEKSource swekSource) {
        synchronized (SWEKPluginLocks.treeSelectionLock) {
            removeEventTypeFromActiveEventTypeMap(eventType, swekSource);
        }
        stopDownloadingEventType(eventType, swekSource);

    }

    @Override
    public void newRequestForDate(Date date) {
        downloadAllSelectedEventTypes(date);
    }

    @Override
    public void newRequestForInterval(Interval<Date> interval) {
        downloadAllSelectedEventTypes(interval);
    }

    @Override
    public void newRequestForDateList(List<Date> dates) {
        downloadAllSelectedEventTypes(dates);
    }

    /**
     * Removes the event type from the busy and finished interval jobs.
     * 
     * @param eventType
     *            the event type to remove
     */
    private void removeFromBusyAndFinishedIntervalJobs(SWEKEventType eventType) {
        busyAndFinishedIntervalJobs.remove(eventType);
    }

    /**
     * Removes the event type from the busy and finished jobs.
     * 
     * @param eventType
     *            the event type to remove
     */
    private void removeFromBusyAndFinishedJobs(SWEKEventType eventType) {
        busyAndFinishedJobs.remove(eventType);

    }

    /**
     * Removes the source for a given event type from the busy and finished
     * interval jobs.
     * 
     * @param eventType
     *            the event type to remove
     * @param source
     *            the source to remove the interval type for
     */
    private void removeFromBusyAndFinishedIntervalJobs(SWEKEventType eventType, SWEKSource source) {
        if (busyAndFinishedIntervalJobs.containsKey(eventType)) {
            Map<SWEKSource, Map<Date, Set<Date>>> datesPerSource = busyAndFinishedIntervalJobs.get(eventType);
            datesPerSource.remove(source);
            busyAndFinishedIntervalJobs.put(eventType, datesPerSource);
        }
    }

    /**
     * Removes the source for a given event type from the busy and finished
     * jobs.
     * 
     * @param eventType
     *            the event type to remove
     * @param source
     *            the source to remove the interval type for
     */
    private void removeFromBusyAndFinishedJobs(SWEKEventType eventType, SWEKSource source) {
        if (busyAndFinishedJobs.containsKey(eventType)) {
            Map<SWEKSource, Set<Date>> datesPerSource = busyAndFinishedJobs.get(eventType);
            datesPerSource.remove(source);
            busyAndFinishedJobs.put(eventType, datesPerSource);
        }
    }

    /**
     * Removes the worker from the map with workers.
     * 
     * @param worker
     *            The worker to remove
     */
    private void removeWorkerFromMap(DownloadWorker worker) {
        synchronized (SWEKPluginLocks.downloadLock) {
            Map<Date, DownloadWorker> dwMapOnDate = dwMap.get(worker.getEventType());
            if (dwMapOnDate != null) {
                dwMapOnDate.remove(worker.getDownloadStartDate());
            }
        }
    }

    /**
     * Adds the downloader worker to the downloader map.
     * 
     * @param eventType
     *            The event type to add
     * @param date
     *            The date for which the event type was downloaded
     * @param dw
     *            The download worker used to download the event type
     */
    private void addToDownloaderMap(SWEKEventType eventType, Date date, DownloadWorker dw) {
        Map<Date, DownloadWorker> dwMapOnDate;
        if (!dwMap.containsKey(eventType)) {
            dwMap.put(eventType, new HashMap<Date, DownloadWorker>());
        }
        dwMapOnDate = dwMap.get(eventType);
        if (!dwMapOnDate.containsKey(date)) {
            dwMapOnDate.put(date, dw);
        } else {
            Log.debug("The event type : " + eventType + " is already downloaded for date : " + date + ". No extra download is done.");
        }
        dwMap.put(eventType, dwMapOnDate);
    }

    /**
     * Add the combination of an event type and a swek source to the list of
     * active event types.
     * 
     * @param eventType
     *            the event type to add
     * @param swekSource
     *            the swek source to add
     */
    private void addEventTypeToActiveEventTypeMap(SWEKEventType eventType, SWEKSource swekSource) {
        if (!activeEventTypes.containsKey(eventType)) {
            activeEventTypes.put(eventType, new HashSet<SWEKSource>());
        }
        Set<SWEKSource> sourceSet = activeEventTypes.get(eventType);
        sourceSet.add(swekSource);
        activeEventTypes.put(eventType, sourceSet);

    }

    /**
     * Removes the combination of an event type and a swek source from the list
     * of active event types.
     * 
     * @param eventType
     *            the event type to remove
     * @param swekSource
     *            the swek source to remove
     */
    private void removeEventTypeFromActiveEventTypeMap(SWEKEventType eventType, SWEKSource swekSource) {
        Set<SWEKSource> sourceSet = activeEventTypes.get(eventType);
        if (sourceSet != null) {
            sourceSet.remove(swekSource);
            activeEventTypes.put(eventType, sourceSet);
        }
    }

    /**
     * Downloads the given combination of event type and swek source for all the
     * already requested dates.
     * 
     * @param eventType
     *            the type to download
     * @param swekSource
     *            the source providing the event type
     */
    private void downloadForAllDates(SWEKEventType eventType, SWEKSource swekSource) {
        List<Date> allDates = requestManager.getAllRequestedDates();
        for (Date date : allDates) {
            startDownloadEventType(eventType, swekSource, date);
        }
        List<Interval<Date>> allIntervals = requestManager.getAllRequestedIntervals();
        for (Interval<Date> interval : allIntervals) {
            startDownloadEventType(eventType, swekSource, interval);
        }
    }

    /**
     * Downloads for the given date the events of the currently active
     * combinations of event type and swek sources.
     * 
     * @param date
     *            The date for which the event should be downloaded from the
     *            sources
     */
    private void downloadAllSelectedEventTypes(Date date) {
        synchronized (SWEKPluginLocks.treeSelectionLock) {
            for (SWEKEventType eventType : activeEventTypes.keySet()) {
                for (SWEKSource source : activeEventTypes.get(eventType)) {
                    startDownloadEventType(eventType, source, date);
                }
            }
        }
    }

    /**
     * Downloads for the given date the events of the currently active
     * combinations of event type and swek sources.
     * 
     * @param interval
     *            The interval for which the event should be downloaded from the
     *            sources
     */
    private void downloadAllSelectedEventTypes(Interval<Date> interval) {
        synchronized (SWEKPluginLocks.treeSelectionLock) {
            for (SWEKEventType eventType : activeEventTypes.keySet()) {
                for (SWEKSource source : activeEventTypes.get(eventType)) {
                    startDownloadEventType(eventType, source, interval);
                }
            }
        }
    }

    /**
     * Checks if a job is already busy or finished.
     * 
     * @param eventType
     *            the type that should be checked
     * @param source
     *            the source that provides the event type
     * @param date
     *            the date that should be checked
     * @return true if the combination was found, false if not
     */
    private boolean inBusyAndFinishedJobs(SWEKEventType eventType, SWEKSource source, Date date) {
        Map<SWEKSource, Set<Date>> sourcesAndDatesForEvent = busyAndFinishedJobs.get(eventType);
        if (sourcesAndDatesForEvent != null) {
            Set<Date> datesForEventAndSource = sourcesAndDatesForEvent.get(source);
            if (datesForEventAndSource != null) {
                return datesForEventAndSource.contains(date);
            }
        }
        return false;
    }

    /**
     * Checks if a job is already busy or finished.
     * 
     * @param eventType
     *            the type that should be checked
     * @param swekSource
     *            the source that provides the event type
     * @param interval
     *            the interval that should be checked
     * @return true if the cobination was found, false if not.
     */
    private boolean inBusyAndFinishedIntervalJobs(SWEKEventType eventType, SWEKSource swekSource, Interval<Date> interval) {
        Map<SWEKSource, Map<Date, Set<Date>>> sourcesAndDatesForEvent = busyAndFinishedIntervalJobs.get(eventType);
        if (sourcesAndDatesForEvent != null) {
            Map<Date, Set<Date>> datesForEventTypeAndSource = sourcesAndDatesForEvent.get(swekSource);
            if (datesForEventTypeAndSource != null) {
                Set<Date> endDatesForStartDate = datesForEventTypeAndSource.get(interval.getStart());
                if (endDatesForStartDate != null) {
                    return endDatesForStartDate.contains(interval.getEnd());
                }
            }
        }
        return false;
    }

    /**
     * Removes the combination of event type, source and date from the busy and
     * finished jobs.
     * 
     * @param eventType
     *            the event type to remove
     * @param source
     *            the source to remove
     * @param date
     *            the date to remove
     */
    private void removeFromBusyAndFinishedJobs(SWEKEventType eventType, SWEKSource source, Date date) {
        Map<SWEKSource, Set<Date>> sourcesAndDatesForEvent = busyAndFinishedJobs.get(eventType);
        if (sourcesAndDatesForEvent != null) {
            Set<Date> datesForEventAndSource = sourcesAndDatesForEvent.get(source);
            if (datesForEventAndSource != null) {
                datesForEventAndSource.remove(date);
            }
        }
    }

    /**
     * Starts downloading for every source the requested event type. This will
     * start a thread to download the events.
     * 
     * @param eventType
     *            The event type to download
     * @param date
     *            The date to download the event type for
     */
    private void startDownloadEventType(SWEKEventType eventType, Date date) {
        for (SWEKSupplier s : eventType.getSuppliers()) {
            startDownloadEventType(eventType, s.getSource(), date);
        }
    }

    /**
     * Starts downloading for one particular source the given event type.
     * 
     * @param eventType
     *            The event type to download
     * @param source
     *            The source from which to download the event type
     */
    private void startDownloadEventType(SWEKEventType eventType, SWEKSource source, Date date) {
        synchronized (SWEKPluginLocks.downloadLock) {
            DownloadWorker dw = new DownloadWorker(eventType, source, date);
            if (!inBusyAndFinishedJobs(eventType, source, date)) {
                dw.addDownloadWorkerListener(this);
                addToDownloaderMap(eventType, dw.getDownloadStartDate(), dw);
                addToBusyAndFinishedJobs(eventType, source, date);
                downloadEventPool.execute(dw);
            }
        }
    }

    /**
     * Starts downloading the event from the source of an interval.
     * 
     * @param eventType
     *            the event type to download
     * @param swekSource
     *            the source from which to download
     * @param interval
     *            the interval over which to download
     */
    private void startDownloadEventType(SWEKEventType eventType, SWEKSource swekSource, Interval<Date> interval) {
        synchronized (SWEKPluginLocks.downloadLock) {
            DownloadWorker dw = new DownloadWorker(eventType, swekSource, interval);
            if (!inBusyAndFinishedIntervalJobs(eventType, swekSource, interval)) {
                dw.addDownloadWorkerListener(this);
                addToDownloaderMap(eventType, dw.getDownloadStartDate(), dw);
                addToBusyAndFinishedIntervalJobs(eventType, swekSource, interval);
                downloadEventPool.execute(dw);
            }
        }
    }

    /**
     * Add event type, source, date to busy and finished jobs.
     * 
     * @param eventType
     *            the event type to add
     * @param source
     *            the source to add
     * @param date
     *            the date to add
     */
    private void addToBusyAndFinishedJobs(SWEKEventType eventType, SWEKSource source, Date date) {
        Map<SWEKSource, Set<Date>> sourcesForEventType = new HashMap<SWEKSource, Set<Date>>();
        Set<Date> dates = new HashSet<Date>();
        if (busyAndFinishedJobs.containsKey(eventType)) {
            sourcesForEventType = busyAndFinishedJobs.get(eventType);
            if (sourcesForEventType.containsKey(source)) {
                dates = sourcesForEventType.get(source);
            }
        }
        dates.add(date);
        sourcesForEventType.put(source, dates);
        busyAndFinishedJobs.put(eventType, sourcesForEventType);
    }

    /**
     * Adds event type, source, interval to busy and finished jobs.
     * 
     * @param eventType
     *            the event type to add
     * @param swekSource
     *            the source to add
     * @param interval
     *            the interval to add
     */
    private void addToBusyAndFinishedIntervalJobs(SWEKEventType eventType, SWEKSource swekSource, Interval<Date> interval) {
        Map<SWEKSource, Map<Date, Set<Date>>> sourcesForEventType = new HashMap<SWEKSource, Map<Date, Set<Date>>>();
        Map<Date, Set<Date>> datesPerSource = new HashMap<Date, Set<Date>>();
        Set<Date> endDate = new HashSet<Date>();
        if (busyAndFinishedIntervalJobs.containsKey(eventType)) {
            sourcesForEventType = busyAndFinishedIntervalJobs.get(eventType);
            if (sourcesForEventType.containsKey(swekSource)) {
                datesPerSource = sourcesForEventType.get(swekSource);
                if (datesPerSource.containsKey(interval.getStart())) {
                    endDate = datesPerSource.get(interval.getStart());
                }
            }
        }
        endDate.add(interval.getEnd());
        datesPerSource.put(interval.getStart(), endDate);
        sourcesForEventType.put(swekSource, datesPerSource);
        busyAndFinishedIntervalJobs.put(eventType, sourcesForEventType);
    }

    /**
     * Starts downloading all the selected events for a list of dates.
     * 
     * @param dates
     *            the list of dates for which to download all the selected
     *            events
     */
    private void downloadAllSelectedEventTypes(List<Date> dates) {
        synchronized (SWEKPluginLocks.downloadLock) {
            for (Date date : dates) {
                downloadAllSelectedEventTypes(date);
            }
        }
    }
}
