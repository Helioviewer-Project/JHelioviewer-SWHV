package org.helioviewer.jhv.plugins.swek.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.JHVEventCacheRequestHandler;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.container.cache.SWEKOperand;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;
import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModelListener;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.threads.JHVThread;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SWEKDownloadManager implements EventTypePanelModelListener, FilterManagerListener, JHVEventCacheRequestHandler {

    private static final int NUMBER_THREADS = 8;
    private static final long SIXHOURS = 1000 * 60 * 60 * 6;
    private static final ExecutorService downloadEventPool = new ThreadPoolExecutor(NUMBER_THREADS, NUMBER_THREADS, 10000L, TimeUnit.MILLISECONDS,
                                                                                    new PriorityBlockingQueue<>(2048, new ComparePriority()),
                                                                                    new JHVThread.NamedThreadFactory("SWEK Download"),
                                                                                    new ThreadPoolExecutor.DiscardPolicy()) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            JHVThread.afterExecute(r, t);
        }
    };
    private static final Map<SWEKEventType, ArrayList<DownloadWorker>> dwMap = new HashMap<>();
    private static final ArrayList<JHVEventType> activeEventTypes = new ArrayList<>();

    private static void stopDownloadingEventType(SWEKEventType eventType, boolean keepActive) {
        for (SWEKSupplier supplier : eventType.getSuppliers()) {
            stopDownloadingEventType(eventType, supplier, keepActive);
        }
    }

    private static void stopDownloadingEventType(SWEKEventType eventType, SWEKSupplier supplier, boolean keepActive) {
        if (dwMap.containsKey(eventType)) {
            ArrayList<DownloadWorker> dwMapOnDate = dwMap.get(eventType);
            for (Iterator<DownloadWorker> it = dwMapOnDate.iterator(); it.hasNext();) {
                DownloadWorker dw = it.next();
                if (dw.getJHVEventType().getSupplier().equals(supplier)) {
                    dw.stopWorker();
                    JHVEventCache.intervalsNotDownloaded(dw.getJHVEventType(), dw.getRequestInterval());
                    it.remove();
                }
            }
            if (dwMap.get(eventType).isEmpty()) {
                SWEKTreeModel.setStopLoading(eventType);
                dwMap.remove(eventType);
            }
        }
        JHVEventCache.removeEvents(JHVEventType.getJHVEventType(eventType, supplier), keepActive);
    }

    public static void workerForcedToStop(DownloadWorker worker) {
        removeFromDownloaderMap(worker);
        JHVEventCache.intervalsNotDownloaded(worker.getJHVEventType(), worker.getRequestInterval());
    }

    public static void workerFinished(DownloadWorker worker) {
        removeFromDownloaderMap(worker);
    }

    @Override
    public void newEventTypeAndSourceActive(SWEKEventType eventType, SWEKSupplier supplier) {
        JHVEventType jhvType = JHVEventType.getJHVEventType(eventType, supplier);
        addEventTypeToActiveEventTypeMap(jhvType);
        JHVEventCache.eventTypeActivated(jhvType);
    }

    @Override
    public void newEventTypeAndSourceInactive(SWEKEventType eventType, SWEKSupplier supplier) {
        removeEventTypeFromActiveEventTypeMap(JHVEventType.getJHVEventType(eventType, supplier));
        stopDownloadingEventType(eventType, supplier, false);
    }

    @Override
    public void filtersChanged(SWEKEventType swekEventType) {
        stopDownloadingEventType(swekEventType, true);
        JHVEventCache.reset(swekEventType);
        downloadSelectedSuppliers(swekEventType);
    }

    private static void removeFromDownloaderMap(DownloadWorker worker) {
        ArrayList<DownloadWorker> dwMapList = dwMap.get(worker.getEventType());
        if (dwMapList != null)
            dwMapList.remove(worker);

        boolean loadingCondition = (dwMapList != null) && !dwMapList.isEmpty();
        if (!loadingCondition)
            SWEKTreeModel.setStopLoading(worker.getEventType());
    }

    private static void addToDownloaderMap(SWEKEventType eventType, DownloadWorker dw) {
        ArrayList<DownloadWorker> dwMapList;
        if (dwMap.containsKey(eventType)) {
            dwMapList = dwMap.get(eventType);
        } else {
            dwMapList = new ArrayList<>();
            dwMap.put(eventType, dwMapList);
        }
        dwMapList.add(dw);
    }

    private static void addEventTypeToActiveEventTypeMap(JHVEventType jhvType) {
        activeEventTypes.add(jhvType);
    }

    private static void removeEventTypeFromActiveEventTypeMap(JHVEventType jhvType) {
        activeEventTypes.remove(jhvType);
    }

    private static List<SWEKParam> defineParameters(SWEKEventType eventType, SWEKSupplier supplier) {
        List<SWEKParam> params = new ArrayList<>();
        params.add(new SWEKParam("provider", supplier.getSupplierName(), SWEKOperand.EQUALS));
        Map<SWEKParameter, List<SWEKParam>> paramsPerEventParameter = FilterManager.getFilterForEventType(eventType);
        for (List<SWEKParam> paramPerParameter : paramsPerEventParameter.values()) {
            params.addAll(paramPerParameter);
        }
        return params;
    }

    private static void downloadSelectedSuppliers(SWEKEventType swekEventType) {
        for (JHVEventType jhvType : activeEventTypes) {
            if (jhvType.getEventType() == swekEventType)
                downloadForAllDates(jhvType);
        }
    }

    private static void downloadForAllDates(JHVEventType jhvType) {
        Collection<Interval> allIntervals = JHVEventCache.getAllRequestIntervals(jhvType);
        for (Interval interval : allIntervals) {
            startDownloadEventType(jhvType, interval);
        }
    }

    @Override
    public void handleRequestForInterval(JHVEventType jhvType, Interval interval) {
        startDownloadEventType(jhvType, interval);
    }

    private static void startDownloadEventType(JHVEventType jhvType, Interval interval) {
        SWEKSupplier supplier = jhvType.getSupplier();
        SWEKEventType eventType = jhvType.getEventType();
        List<SWEKParam> params = defineParameters(eventType, supplier);
        for (Interval intt : Interval.splitInterval(interval, 2)) {
            if (intt.start < System.currentTimeMillis() + SIXHOURS) {
                DownloadWorker dw = new DownloadWorker(jhvType, intt, params);
                SWEKTreeModel.setStartLoading(eventType);
                addToDownloaderMap(eventType, dw);
                downloadEventPool.execute(dw);
            }
        }
    }

    private static class ComparePriority<T extends DownloadWorker> implements Comparator<T> {
        @Override
        public int compare(T l1, T l2) {
            long start = Layers.getStartDate().milli;
            long d1 = l1.getRequestInterval().end - start;
            long d2 = l2.getRequestInterval().end - start;
            return d1 < d2 ? 1 : -1;
        }
    }

}
