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

    private static SWEKDownloadManager instance;
    private final ExecutorService downloadEventPool;
    private final Map<SWEKEventType, ArrayList<DownloadWorker>> dwMap;
    private final Map<SWEKEventType, Map<SWEKSource, Set<SWEKSupplier>>> activeEventTypes;
    private final IncomingRequestManager requestManager;
    private final JHVEventContainer eventContainer;
    private final FilterManager filterManager;
    private final SWEKTreeModel treeModel;
    private final SWEKConfigurationManager configInstance = SWEKConfigurationManager.getSingletonInstance();

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
            }
        }
        treeModel.setStopLoading(eventType);

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
                }
            }
        }
        treeModel.setStopLoading(eventType);

        dwMap.remove(eventType);
        eventContainer.removeEvents(new JHVSWEKEventType(eventType.getEventName(), source.getSourceName(), supplier.getSupplierName()), keepActive);
    }

    public void workerForcedToStop(DownloadWorker worker) {
        removeFromDownloaderMap(worker);
        JHVEventContainer.getSingletonInstance().intervalsNotDownloaded(worker.getJHVEventType(), worker.getRequestInterval());
    }

    public void workerFinished(DownloadWorker worker) {
        removeFromDownloaderMap(worker);
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

    private void removeFromDownloaderMap(DownloadWorker worker) {
        ArrayList<DownloadWorker> dwMapList = dwMap.get(worker.getEventType());

        if (dwMapList != null)
            dwMapList.remove(worker);

        boolean loadingCondition = (dwMapList != null) && !dwMapList.isEmpty();
        if (!loadingCondition)
            treeModel.setStopLoading(worker.getEventType());
    }

    private void addToDownloaderMap(SWEKEventType eventType, DownloadWorker dw) {
        ArrayList<DownloadWorker> dwMapList;
        if (dwMap.containsKey(eventType)) {
            dwMapList = dwMap.get(eventType);
        } else {
            dwMapList = new ArrayList<DownloadWorker>();
            dwMap.put(eventType, dwMapList);
        }
        dwMapList.add(dw);
    }

    private void addEventTypeToActiveEventTypeMap(SWEKEventType eventType, SWEKSource source, SWEKSupplier swekSupplier) {
        Map<SWEKSource, Set<SWEKSupplier>> sourcesPerEventType = new HashMap<SWEKSource, Set<SWEKSupplier>>();
        Set<SWEKSupplier> supplierPerSource;
        if (activeEventTypes.containsKey(eventType)) {
            sourcesPerEventType = activeEventTypes.get(eventType);
            if (sourcesPerEventType.containsKey(source)) {
                supplierPerSource = sourcesPerEventType.get(source);
            } else {
                supplierPerSource = new HashSet<SWEKSupplier>();
            }
        } else {
            supplierPerSource = new HashSet<SWEKSupplier>();
        }
        supplierPerSource.add(swekSupplier);
        sourcesPerEventType.put(source, supplierPerSource);
        activeEventTypes.put(eventType, sourcesPerEventType);
    }

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

    private List<SWEKParam> defineParameters(SWEKEventType eventType, SWEKSupplier supplier) {
        List<SWEKParam> params = new ArrayList<SWEKParam>();
        params.add(new SWEKParam("provider", supplier.getSupplierName(), SWEKOperand.EQUALS));
        Map<SWEKParameter, List<SWEKParam>> paramsPerEventParameter = filterManager.getFilterForEventType(eventType);
        for (List<SWEKParam> paramPerParameter : paramsPerEventParameter.values()) {
            params.addAll(paramPerParameter);
        }
        return params;
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

    private void downloadSelectedSuppliers(SWEKEventType swekEventType) {
        if (activeEventTypes.containsKey(swekEventType)) {
            for (SWEKSource source : activeEventTypes.get(swekEventType).keySet()) {
                for (SWEKSupplier supplier : activeEventTypes.get(swekEventType).get(source)) {
                    downloadForAllDates(swekEventType, source, supplier);
                }
            }
        }
    }

    private void downloadForAllDates(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier) {
        Collection<Interval<Date>> allIntervals = JHVEventContainer.getSingletonInstance().getAllRequestIntervals(new JHVSWEKEventType(eventType.getEventName(), swekSource.getSourceName(), supplier.getSupplierName()));
        for (Interval<Date> interval : allIntervals) {
            startDownloadEventType(eventType, swekSource, interval, supplier);
        }
    }

    private void startDownloadEventType(SWEKEventType eventType, SWEKSource swekSource, Interval<Date> interval, SWEKSupplier supplier) {
        List<SWEKParam> params = defineParameters(eventType, supplier);
        for (Interval<Date> intt : Interval.splitInterval(interval, 2)) {
            DownloadWorker dw = new DownloadWorker(eventType, swekSource, supplier, intt, params, configInstance.getSWEKRelatedEvents());
            treeModel.setStartLoading(eventType);
            addToDownloaderMap(eventType, dw);
            downloadEventPool.execute(dw);
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
