package org.helioviewer.jhv.plugins.swek.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.container.cache.SWEKOperand;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModelListener;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.plugins.swek.settings.SWEKProperties;
import org.helioviewer.jhv.threads.JHVThread;

/**
 *
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKDownloadManager implements EventTypePanelModelListener, FilterManagerListener {

    private static final long SIXHOURS = 1000 * 60 * 60 * 6;
    private static SWEKDownloadManager instance;
    private final ExecutorService downloadEventPool;
    private final Map<SWEKEventType, ArrayList<DownloadWorker>> dwMap;
    private final ArrayList<JHVEventType> activeEventTypes;
    private final JHVEventContainer eventContainer;
    private final FilterManager filterManager;
    private final SWEKTreeModel treeModel;

    private SWEKDownloadManager() {
        dwMap = new HashMap<SWEKEventType, ArrayList<DownloadWorker>>();
        activeEventTypes = new ArrayList<JHVEventType>();

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
            eventContainer.removeEvents(JHVEventType.getJHVEventType(eventType, supplier), keepActive);
        }
    }

    public void stopDownloadingEventType(SWEKEventType eventType, SWEKSource source, SWEKSupplier supplier, boolean keepActive) {
        if (dwMap.containsKey(eventType)) {
            ArrayList<DownloadWorker> dwMapOnDate = dwMap.get(eventType);
            for (DownloadWorker dw : dwMapOnDate) {
                if (dw.getJHVEventType().getSupplier().equals(supplier)) {
                    dw.stopWorker();
                }
            }
        }
        treeModel.setStopLoading(eventType);

        dwMap.remove(eventType);
        eventContainer.removeEvents(JHVEventType.getJHVEventType(eventType, supplier), keepActive);
    }

    public void workerForcedToStop(DownloadWorker worker) {
        removeFromDownloaderMap(worker);
        JHVEventContainer.getSingletonInstance().intervalsNotDownloaded(worker.getJHVEventType(), worker.getRequestInterval());
    }

    public void workerFinished(DownloadWorker worker) {
        removeFromDownloaderMap(worker);
    }

    @Override
    public void newEventTypeAndSourceActive(SWEKEventType eventType, SWEKSupplier supplier) {
        JHVEventType jhvType = JHVEventType.getJHVEventType(eventType, supplier);
        addEventTypeToActiveEventTypeMap(jhvType);
        JHVEventContainer.getSingletonInstance().eventTypeActivated(jhvType);
    }

    @Override
    public void newEventTypeAndSourceInActive(SWEKEventType eventType, SWEKSupplier supplier) {
        removeEventTypeFromActiveEventTypeMap(JHVEventType.getJHVEventType(eventType, supplier));
        stopDownloadingEventType(eventType, supplier.getSource(), supplier, false);
    }

    public void newRequestForInterval(JHVEventType eventType, Interval interval) {
        downloadEventType(eventType, interval);
    }

    @Override
    public void filtersAdded(SWEKEventType swekEventType) {
        stopDownloadingEventType(swekEventType, true);
        JHVEventContainer.getSingletonInstance().reset(swekEventType);
        downloadSelectedSuppliers(swekEventType);
    }

    @Override
    public void filtersRemoved(SWEKEventType swekEventType, SWEKParameter parameter) {
        stopDownloadingEventType(swekEventType, true);
        JHVEventContainer.getSingletonInstance().reset(swekEventType);
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

    private void addEventTypeToActiveEventTypeMap(JHVEventType jhvType) {
        activeEventTypes.add(jhvType);
    }

    private void removeEventTypeFromActiveEventTypeMap(JHVEventType jhvType) {
        activeEventTypes.remove(jhvType);
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

    private void downloadEventType(JHVEventType eventType, Interval interval) {
        SWEKEventType swekEventType = eventType.getEventType();
        SWEKSupplier supplier = eventType.getSupplier();
        if (swekEventType != null && supplier != null) {
            startDownloadEventType(eventType, interval);
        } else {
            Log.debug("SWEKType: " + swekEventType + " SWEKSupplier: " + supplier);
        }
    }

    private void downloadSelectedSuppliers(SWEKEventType swekEventType) {
        for (JHVEventType jhvType : activeEventTypes) {
            if (jhvType.getEventType() == swekEventType)
                downloadForAllDates(jhvType);
        }
    }

    private void downloadForAllDates(JHVEventType jhvType) {
        Collection<Interval> allIntervals = JHVEventContainer.getSingletonInstance().getAllRequestIntervals(jhvType);
        for (Interval interval : allIntervals) {
            startDownloadEventType(jhvType, interval);
        }
    }

    private void startDownloadEventType(JHVEventType jhvType, Interval interval) {
        SWEKSupplier supplier = jhvType.getSupplier();
        SWEKEventType eventType = jhvType.getEventType();
        List<SWEKParam> params = defineParameters(eventType, supplier);
        for (Interval intt : Interval.splitInterval(interval, 7)) {
            if (intt.start.getTime() < System.currentTimeMillis() + SIXHOURS) {
                DownloadWorker dw = new DownloadWorker(jhvType, intt, params);
                treeModel.setStartLoading(eventType);
                addToDownloaderMap(eventType, dw);
                downloadEventPool.execute(dw);
            }
        }
    }

    private static class ComparePriority<T extends DownloadWorker> implements Comparator<T> {
        @Override
        public int compare(T l1, T l2) {
            JHVDate startDate = Layers.getStartDate();
            long start;
            if (Layers.getStartDate() != null)
                start = startDate.milli;
            else
                start = System.currentTimeMillis();
            long d1 = l1.getDownloadEndDate().getTime() - start;
            long d2 = l2.getDownloadEndDate().getTime() - start;
            return d1 < d2 ? 1 : -1;
        }
    }

}
