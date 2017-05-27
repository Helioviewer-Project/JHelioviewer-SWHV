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
import org.helioviewer.jhv.data.cache.JHVEventCache;
import org.helioviewer.jhv.data.cache.JHVEventCacheRequestHandler;
import org.helioviewer.jhv.data.cache.SWEKOperand;
import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.event.SWEKParam;
import org.helioviewer.jhv.data.event.SWEKParameter;
import org.helioviewer.jhv.data.event.SWEKSupplier;
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
    private static final Map<SWEKGroup, ArrayList<DownloadWorker>> dwMap = new HashMap<>();
    private static final ArrayList<SWEKSupplier> activeEventTypes = new ArrayList<>();

    private static final SWEKDownloadManager instance = new SWEKDownloadManager();

    public static SWEKDownloadManager getSingletonInstance() {
        return instance;
    }

    private SWEKDownloadManager() {
        JHVEventCache.registerHandler(this);
    }

    private static void stopDownloadingGroup(SWEKGroup group, boolean keepActive) {
        for (SWEKSupplier supplier : group.getSuppliers()) {
            stopDownloadingEventType(group, supplier, keepActive);
        }
    }

    private static void stopDownloadingEventType(SWEKGroup group, SWEKSupplier supplier, boolean keepActive) {
        if (dwMap.containsKey(group)) {
            ArrayList<DownloadWorker> dwMapOnDate = dwMap.get(group);
            for (Iterator<DownloadWorker> it = dwMapOnDate.iterator(); it.hasNext();) {
                DownloadWorker dw = it.next();
                if (dw.getSupplier().equals(supplier)) {
                    dw.stopWorker();
                    JHVEventCache.intervalsNotDownloaded(dw.getSupplier(), dw.getRequestInterval());
                    it.remove();
                }
            }
            if (dwMap.get(group).isEmpty()) {
                SWEKTreeModel.setStopLoading(group);
                dwMap.remove(group);
            }
        }
        JHVEventCache.removeEvents(supplier, keepActive);
    }

    public static void workerForcedToStop(DownloadWorker worker) {
        removeFromDownloaderMap(worker);
        JHVEventCache.intervalsNotDownloaded(worker.getSupplier(), worker.getRequestInterval());
    }

    public static void workerFinished(DownloadWorker worker) {
        removeFromDownloaderMap(worker);
    }

    @Override
    public void activateGroupAndSupplier(SWEKGroup group, SWEKSupplier supplier, boolean active) {
        if (active) {
            addEventTypeToActiveEventTypeMap(supplier);
            JHVEventCache.eventTypeActivated(supplier);
        } else {
            removeEventTypeFromActiveEventTypeMap(supplier);
            stopDownloadingEventType(group, supplier, false);
        }
    }

    @Override
    public void filtersChanged(SWEKGroup group) {
        stopDownloadingGroup(group, true);
        JHVEventCache.reset(group);
        downloadSelectedSuppliers(group);
    }

    private static void removeFromDownloaderMap(DownloadWorker worker) {
        ArrayList<DownloadWorker> dwMapList = dwMap.get(worker.getGroup());
        if (dwMapList != null)
            dwMapList.remove(worker);

        boolean loadingCondition = (dwMapList != null) && !dwMapList.isEmpty();
        if (!loadingCondition)
            SWEKTreeModel.setStopLoading(worker.getGroup());
    }

    private static void addToDownloaderMap(SWEKGroup group, DownloadWorker dw) {
        ArrayList<DownloadWorker> dwMapList;
        if (dwMap.containsKey(group)) {
            dwMapList = dwMap.get(group);
        } else {
            dwMapList = new ArrayList<>();
            dwMap.put(group, dwMapList);
        }
        dwMapList.add(dw);
    }

    private static void addEventTypeToActiveEventTypeMap(SWEKSupplier jhvType) {
        activeEventTypes.add(jhvType);
    }

    private static void removeEventTypeFromActiveEventTypeMap(SWEKSupplier jhvType) {
        activeEventTypes.remove(jhvType);
    }

    private static List<SWEKParam> defineParameters(SWEKGroup group, SWEKSupplier supplier) {
        List<SWEKParam> params = new ArrayList<>();
        params.add(new SWEKParam("provider", supplier.getSupplierName(), SWEKOperand.EQUALS));
        Map<SWEKParameter, List<SWEKParam>> paramsPerEventParameter = FilterManager.getFilterForGroup(group);
        for (List<SWEKParam> paramPerParameter : paramsPerEventParameter.values()) {
            params.addAll(paramPerParameter);
        }
        return params;
    }

    private static void downloadSelectedSuppliers(SWEKGroup group) {
        for (SWEKSupplier jhvType : activeEventTypes) {
            if (jhvType.getGroup() == group)
                downloadForAllDates(jhvType);
        }
    }

    private static void downloadForAllDates(SWEKSupplier jhvType) {
        Collection<Interval> allIntervals = JHVEventCache.getAllRequestIntervals(jhvType);
        for (Interval interval : allIntervals) {
            startDownloadEventType(jhvType, interval);
        }
    }

    @Override
    public void handleRequestForInterval(SWEKSupplier jhvType, Interval interval) {
        startDownloadEventType(jhvType, interval);
    }

    private static void startDownloadEventType(SWEKSupplier supplier, Interval interval) {
        SWEKGroup group = supplier.getGroup();
        List<SWEKParam> params = defineParameters(group, supplier);
        for (Interval intt : Interval.splitInterval(interval, 2)) {
            if (intt.start < System.currentTimeMillis() + SIXHOURS) {
                DownloadWorker dw = new DownloadWorker(supplier, intt, params);
                SWEKTreeModel.setStartLoading(group);
                addToDownloaderMap(group, dw);
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
