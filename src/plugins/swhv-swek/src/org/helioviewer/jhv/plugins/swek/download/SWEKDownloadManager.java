package org.helioviewer.jhv.plugins.swek.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.swek.config.SWEKConfigurationManager;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModelListener;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.plugins.swek.request.IncomingRequestManager;
import org.helioviewer.jhv.plugins.swek.request.IncomingRequestManagerListener;
import org.helioviewer.jhv.plugins.swek.settings.SWEKProperties;
import org.helioviewer.jhv.threads.JHVThread;

/**
 *
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKDownloadManager implements DownloadWorkerListener, IncomingRequestManagerListener, EventTypePanelModelListener, FilterManagerListener {

    /** Singleton instance of the SWE */
    private static SWEKDownloadManager instance;

    /** Threadpool for downloading events */
    private final PriorityBlockingQueue<Runnable> priorityQueue;
    private final ExecutorService downloadEventPool;

    /** The properties specific to the swek plugin */
    private final Properties swekProperties;

    /** Map holding the download workers order by event type and date */
    private final Map<SWEKEventType, Map<Date, DownloadWorker>> dwMap;

    /** Map holding the active event types and its sources */
    private final Map<SWEKEventType, Map<SWEKSource, Set<SWEKSupplier>>> activeEventTypes;

    /** Local instance of the request manager */
    private final IncomingRequestManager requestManager;

    /** Local instance of the event container */
    private final JHVEventContainer eventContainer;

    /** Local instance of filter manager */
    private final FilterManager filterManager;

    /** The instance of the SWEKTreeModel */
    private final SWEKTreeModel treeModel;

    /** The configuration manager instance */
    private final SWEKConfigurationManager configInstance = SWEKConfigurationManager.getSingletonInstance();

    /**
     * private constructor of the SWEKDownloadManager
     */
    private SWEKDownloadManager() {
        swekProperties = SWEKProperties.getSingletonInstance().getSWEKProperties();
        dwMap = new HashMap<SWEKEventType, Map<Date, DownloadWorker>>();
        activeEventTypes = new HashMap<SWEKEventType, Map<SWEKSource, Set<SWEKSupplier>>>();
        requestManager = IncomingRequestManager.getSingletonInstance();
        requestManager.addRequestManagerListener(this);
        eventContainer = JHVEventContainer.getSingletonInstance();
        filterManager = FilterManager.getSingletonInstance();
        filterManager.addFilterManagerListener(this);
        treeModel = SWEKTreeModel.getSingletonInstance();
        priorityQueue = new PriorityBlockingQueue<Runnable>(2048, new ComparePriority());
        int numOfThread = Integer.parseInt(swekProperties.getProperty("plugin.swek.numberofthreads"));
        downloadEventPool = new ThreadPoolExecutor(numOfThread, numOfThread, 10000L, TimeUnit.MILLISECONDS, priorityQueue, new JHVThread.NamedThreadFactory("SWEK Download"), new ThreadPoolExecutor.DiscardPolicy());
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
    public void stopDownloadingEventType(SWEKEventType eventType, boolean keepActive) {
        if (dwMap.containsKey(eventType)) {
            Map<Date, DownloadWorker> dwMapOnDate = dwMap.get(eventType);
            for (DownloadWorker dw : dwMapOnDate.values()) {
                dw.stopWorker();
            }
        }

        for (SWEKSupplier supplier : eventType.getSuppliers()) {
            eventContainer.removeEvents(new JHVSWEKEventType(eventType.getEventName(), supplier.getSource().getSourceName(), supplier.getSupplierName()), keepActive);
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
    public void stopDownloadingEventType(SWEKEventType eventType, SWEKSource source, SWEKSupplier supplier, boolean keepActive) {
        if (dwMap.containsKey(eventType)) {
            Map<Date, DownloadWorker> dwMapOnDate = dwMap.get(eventType);
            for (DownloadWorker dw : dwMapOnDate.values()) {
                if (dw.getSupplier().equals(supplier)) {
                    dw.stopWorker();
                }
            }
        }

        eventContainer.removeEvents(new JHVSWEKEventType(eventType.getEventName(), source.getSourceName(), supplier.getSupplierName()), keepActive);
    }

    @Override
    public void workerStarted(DownloadWorker worker) {
        treeModel.setStartLoading(worker.getEventType(), worker);
    }

    @Override
    public void workerForcedToStop(DownloadWorker worker) {
        treeModel.setStopLoading(worker.getEventType(), worker);
        removeWorkerFromMap(worker);
        JHVEventContainer.getSingletonInstance().intervalsNotDownloaded(worker.getJHVEventType(), worker.getRequestInterval());
    }

    @Override
    public void workerFinished(DownloadWorker worker) {
        treeModel.setStopLoading(worker.getEventType(), worker);
        removeWorkerFromMap(worker);
    }

    @Override
    public void newEventTypeAndSourceActive(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier) {
        addEventTypeToActiveEventTypeMap(eventType, swekSource, supplier);
        JHVEventContainer.getSingletonInstance().eventTypeActivated(new JHVSWEKEventType(eventType.getEventName(), swekSource.getSourceName(), supplier.getSupplierName()));
        // downloadForAllDates(eventType, swekSource, supplier);
    }

    @Override
    public void newEventTypeAndSourceInActive(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier) {
        removeEventTypeFromActiveEventTypeMap(eventType, swekSource, supplier);
        stopDownloadingEventType(eventType, swekSource, supplier, false);
    }

    @Override
    public void newRequestForInterval(JHVEventType eventType, Interval<Date> interval) {
        downloadEventType(eventType, interval);
    }

    private void downloadEventType(JHVEventType eventType, Interval<Date> interval) {
        SWEKEventType swekEventType = SWEKConfigurationManager.getSingletonInstance().getEventType(eventType.getEventType());
        SWEKSource source = SWEKConfigurationManager.getSingletonInstance().getSWEKSource(eventType.getEventSource());
        SWEKSupplier supplier = SWEKConfigurationManager.getSingletonInstance().getSWEKSupplier(eventType.getEventProvider(), eventType.getEventType());
        if (swekEventType != null && source != null && supplier != null) {
            startDownloadEventType(swekEventType, source, interval, supplier);
        } else {
            Log.debug("SWEKType: " + swekEventType + " SWEKSource: " + source + " SWEKSupplier: " + supplier);
        }
    }

    @Override
    public void filtersAdded(SWEKEventType swekEventType) {
        stopDownloadingEventType(swekEventType, true);
        downloadSelectedSuppliers(swekEventType);
    }

    @Override
    public void filtersRemoved(SWEKEventType swekEventType, SWEKParameter parameter) {
        stopDownloadingEventType(swekEventType, true);
        downloadSelectedSuppliers(swekEventType);
    }

    /**
     * Removes the worker from the map with workers.
     *
     * @param worker
     *            The worker to remove
     */
    private void removeWorkerFromMap(DownloadWorker worker) {
        Map<Date, DownloadWorker> dwMapOnDate = dwMap.get(worker.getEventType());
        if (dwMapOnDate != null) {
            dwMapOnDate.remove(worker.getDownloadStartDate());
        } else {
            Log.warn("Key should exist already");
            Thread.dumpStack();
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
     * @param requestID
     */
    private void addToDownloaderMap(SWEKEventType eventType, Date date, DownloadWorker dw) {
        Map<Date, DownloadWorker> dwMapOnDate = new HashMap<Date, DownloadWorker>();
        if (dwMap.containsKey(eventType)) {
            dwMapOnDate = dwMap.get(eventType);
        }
        dwMapOnDate.put(date, dw);
        dwMap.put(eventType, dwMapOnDate);
    }

    /**
     * Add the combination of an event type and a swek source to the list of
     * active event types.
     *
     * @param eventType
     *            the event type to add
     * @param swekSupplier
     *            the swek source to add
     */
    private void addEventTypeToActiveEventTypeMap(SWEKEventType eventType, SWEKSource source, SWEKSupplier swekSupplier) {
        Map<SWEKSource, Set<SWEKSupplier>> sourcesPerEventType = new HashMap<SWEKSource, Set<SWEKSupplier>>();
        Set<SWEKSupplier> supplierPerSource = new HashSet<SWEKSupplier>();
        if (activeEventTypes.containsKey(eventType)) {
            sourcesPerEventType = activeEventTypes.get(eventType);
            if (sourcesPerEventType.containsKey(source)) {
                supplierPerSource = sourcesPerEventType.get(source);
            }
        }
        supplierPerSource.add(swekSupplier);
        sourcesPerEventType.put(source, supplierPerSource);
        activeEventTypes.put(eventType, sourcesPerEventType);

    }

    /**
     * Removes the combination of an event type, a swek source and a swek
     * supplier from the list of active event types.
     *
     * @param eventType
     *            the event type to remove
     * @param swekSource
     *            the swek source to remove
     * @param supplier
     *            the supplier to remove
     */
    private void removeEventTypeFromActiveEventTypeMap(SWEKEventType eventType, SWEKSource source, SWEKSupplier swekSupplier) {
        Map<SWEKSource, Set<SWEKSupplier>> sourcesPerEventtype = activeEventTypes.get(eventType);
        if (sourcesPerEventtype != null) {
            Set<SWEKSupplier> supplierPerSource = sourcesPerEventtype.get(source);
            if (supplierPerSource != null) {
                supplierPerSource.remove(swekSupplier);
            }
            sourcesPerEventtype.put(source, supplierPerSource);
            activeEventTypes.put(eventType, sourcesPerEventtype);
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
     * @param supplier
     *            the supplier to producing the event
     */
    private void downloadForAllDates(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier) {

        Collection<Interval<Date>> allIntervals = JHVEventContainer.getSingletonInstance().getAllRequestIntervals(new JHVSWEKEventType(eventType.getEventName(), swekSource.getSourceName(), supplier.getSupplierName()));
        for (Interval<Date> interval : allIntervals) {
            startDownloadEventType(eventType, swekSource, interval, supplier);
        }
    }

    /**
     * Defines the parameters based on filters and provider.
     *
     * @param eventType
     *            the event type for which the parameters are defined
     * @param source
     *            the source from where the events are coming
     * @return the parameters
     */
    private List<SWEKParam> defineParameters(SWEKEventType eventType, SWEKSource source, SWEKSupplier supplier) {
        List<SWEKParam> params = new ArrayList<SWEKParam>();
        params.add(new SWEKParam("provider", supplier.getSupplierName(), SWEKOperand.EQUALS));
        Map<SWEKParameter, List<SWEKParam>> paramsPerEventParameter = filterManager.getFilterForEventType(eventType);
        for (List<SWEKParam> paramPerParameter : paramsPerEventParameter.values()) {
            params.addAll(paramPerParameter);
        }
        return params;
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
     * @param supplier
     *            the supplier providing the event
     */
    private void startDownloadEventType(SWEKEventType eventType, SWEKSource swekSource, Interval<Date> interval, SWEKSupplier supplier) {
        List<SWEKParam> params = defineParameters(eventType, swekSource, supplier);
        // for (Interval<Date> intt : Interval.splitInterval(interval, 7)) {
        DownloadWorker dw = new DownloadWorker(eventType, swekSource, supplier, interval, params, configInstance.getSWEKRelatedEvents());
        dw.addDownloadWorkerListener(this);
        addToDownloaderMap(eventType, dw.getDownloadStartDate(), dw);
        downloadEventPool.execute(dw);
        // }

    }

    /**
     * Downloads the event type for all the dates.
     *
     * @param swekEventType
     */
    private void downloadSelectedSuppliers(SWEKEventType swekEventType) {
        if (activeEventTypes.get(swekEventType) != null) {
            for (SWEKSource source : activeEventTypes.get(swekEventType).keySet()) {
                for (SWEKSupplier supplier : activeEventTypes.get(swekEventType).get(source)) {
                    downloadForAllDates(swekEventType, source, supplier);
                }
            }
        }
    }

    private static class ComparePriority<T extends DownloadWorker> implements Comparator<T> {

        @Override
        public int compare(T l1, T l2) {
            long d1 = l1.getDownloadEndDate().getTime() - Layers.getStartDate().getTime();
            long d2 = l2.getDownloadEndDate().getTime() - Layers.getStartDate().getTime();
            return d1 < d2 ? 1 : -1;
        }
    }

}
