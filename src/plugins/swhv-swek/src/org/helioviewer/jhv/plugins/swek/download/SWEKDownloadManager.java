package org.helioviewer.jhv.plugins.swek.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class SWEKDownloadManager implements IncomingRequestManagerListener, EventTypePanelModelListener, FilterManagerListener {

    /** Singleton instance of the SWE */
    private static SWEKDownloadManager instance;

    /** Threadpool for downloading events */
    private final ExecutorService downloadEventPool;

    /** Map holding the download workers order by event type and date */
    private final Map<SWEKEventType, ArrayList<DownloadWorker>> dwMap;

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
        dwMap = new HashMap<SWEKEventType, ArrayList<DownloadWorker>>();
        activeEventTypes = new HashMap<SWEKEventType, Map<SWEKSource, Set<SWEKSupplier>>>();
        requestManager = IncomingRequestManager.getSingletonInstance();
        requestManager.addRequestManagerListener(this);
        eventContainer = JHVEventContainer.getSingletonInstance();
        filterManager = FilterManager.getSingletonInstance();
        filterManager.addFilterManagerListener(this);
        treeModel = SWEKTreeModel.getSingletonInstance();
        PriorityBlockingQueue<Runnable> priorityQueue = new PriorityBlockingQueue<Runnable>(2048, new ComparePriority());
        int numOfThread = Integer.parseInt(SWEKProperties.getSingletonInstance().getSWEKProperties().getProperty("plugin.swek.numberofthreads"));
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

    public void stopDownloadingEventType(SWEKEventType eventType, boolean keepActive) {

        if (dwMap.containsKey(eventType)) {
            ArrayList<DownloadWorker> dwMapList = dwMap.get(eventType);
            for (DownloadWorker dw : dwMapList) {
                dw.stopWorker();
                treeModel.setStopLoading(eventType, dw);
            }
        }
        dwMap.remove(eventType);

        for (SWEKSupplier supplier : eventType.getSuppliers()) {
            eventContainer.removeEvents(new JHVSWEKEventType(eventType.getEventName(), supplier.getSource().getSourceName(), supplier.getSupplierName()), keepActive);
        }
    }

    public void stopDownloadingEventType(SWEKEventType eventType, SWEKSource source, SWEKSupplier supplier, boolean keepActive) {
        if (dwMap.containsKey(eventType)) {
            ArrayList<DownloadWorker> dwMapOnDate = dwMap.get(eventType);
            for (DownloadWorker dw : dwMapOnDate) {
                if (dw.getSupplier().equals(supplier)) {
                    dw.stopWorker();
                    treeModel.setStopLoading(eventType, dw);
                }
            }
        }
        dwMap.remove(eventType);
        eventContainer.removeEvents(new JHVSWEKEventType(eventType.getEventName(), source.getSourceName(), supplier.getSupplierName()), keepActive);
    }

    public void workerForcedToStop(DownloadWorker worker) {
        removeWorkerFromMap(worker);
        JHVEventContainer.getSingletonInstance().intervalsNotDownloaded(worker.getJHVEventType(), worker.getRequestInterval());
    }

    public void workerFinished(DownloadWorker worker) {
        removeWorkerFromMap(worker);
    }

    @Override
    public void newEventTypeAndSourceActive(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier) {
        addEventTypeToActiveEventTypeMap(eventType, swekSource, supplier);
        JHVEventContainer.getSingletonInstance().eventTypeActivated(new JHVSWEKEventType(eventType.getEventName(), swekSource.getSourceName(), supplier.getSupplierName()));
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
        ArrayList<DownloadWorker> dwMapList = dwMap.get(worker.getEventType());
        if (dwMapList != null)
            dwMapList.remove(worker);
        treeModel.setStopLoading(worker.getEventType(), worker);
    }

    private void addToDownloaderMap(SWEKEventType eventType, DownloadWorker dw) {
        ArrayList<DownloadWorker> dwMapList;
        if (dwMap.containsKey(eventType)) {
            dwMapList = dwMap.get(eventType);
        }
        else {
            dwMapList = new ArrayList<DownloadWorker>();
            dwMap.put(eventType, dwMapList);
        }
        dwMapList.add(dw);
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
    private List<SWEKParam> defineParameters(SWEKEventType eventType, SWEKSupplier supplier) {
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
        List<SWEKParam> params = defineParameters(eventType, supplier);
        for (Interval<Date> intt : Interval.splitInterval(interval, 2)) {
            DownloadWorker dw = new DownloadWorker(eventType, swekSource, supplier, intt, params, configInstance.getSWEKRelatedEvents());
            treeModel.setStartLoading(eventType, dw);
            addToDownloaderMap(eventType, dw);
            downloadEventPool.execute(dw);
        }
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
            long d1 = l1.getDownloadEndDate().getTime() - Layers.getStartDate().milli;
            long d2 = l2.getDownloadEndDate().getTime() - Layers.getStartDate().milli;
            return d1 < d2 ? 1 : -1;
        }
    }

}
