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
import org.helioviewer.jhv.data.event.SWEKOperand;
import org.helioviewer.jhv.data.event.SWEKParam;
import org.helioviewer.jhv.data.event.SWEKParameter;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.helioviewer.jhv.data.event.filter.FilterManager;
import org.helioviewer.jhv.data.event.filter.FilterManagerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.threads.JHVThread;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SWEKDownloadManager implements FilterManagerListener, JHVEventCacheRequestHandler {

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
    private static final Map<SWEKSupplier, ArrayList<DownloadWorker>> supplierMap = new HashMap<>();

    private static final SWEKDownloadManager instance = new SWEKDownloadManager();

    private SWEKDownloadManager() {
        JHVEventCache.registerHandler(this);
        FilterManager.addFilterManagerListener(this);
    }

    private static void stopDownloadSupplier(SWEKSupplier supplier, boolean keepActive) {
        ArrayList<DownloadWorker> workerList = supplierMap.get(supplier);
        if (workerList != null) {
            for (Iterator<DownloadWorker> it = workerList.iterator(); it.hasNext();) {
                DownloadWorker worker = it.next();
                if (worker.getSupplier() == supplier) {
                    worker.stopWorker();
                    JHVEventCache.intervalsNotDownloaded(supplier, worker.getRequestInterval());
                    it.remove();
                }
            }
            if (workerList.isEmpty()) {
                SWEKTreeModel.setStopLoading(supplier.getGroup());
                supplierMap.remove(supplier);
            }
        }
        JHVEventCache.removeSupplier(supplier, keepActive);
    }

    public static void workerForcedToStop(DownloadWorker worker) {
        removeFromDownloaderMap(worker);
        JHVEventCache.intervalsNotDownloaded(worker.getSupplier(), worker.getRequestInterval());
    }

    public static void workerFinished(DownloadWorker worker) {
        removeFromDownloaderMap(worker);
    }

    public static void activateSupplier(SWEKSupplier supplier, boolean active) {
        if (active)
            JHVEventCache.supplierActivated(supplier);
        else
            stopDownloadSupplier(supplier, false);
    }

    @Override
    public void filtersChanged(SWEKSupplier supplier) {
        stopDownloadSupplier(supplier, true);
        if (supplier.isSelected())
            downloadForAllDates(supplier);
    }

    private static void removeFromDownloaderMap(DownloadWorker worker) {
        ArrayList<DownloadWorker> workerList = supplierMap.get(worker.getSupplier());
        if (workerList != null)
            workerList.remove(worker);
        if (workerList == null || workerList.isEmpty())
            SWEKTreeModel.setStopLoading(worker.getSupplier().getGroup());
    }

    private static void addToDownloaderMap(DownloadWorker worker) {
        ArrayList<DownloadWorker> workerList = supplierMap.computeIfAbsent(worker.getSupplier(), k -> new ArrayList<>());
        workerList.add(worker);
    }

    private static List<SWEKParam> defineParameters(SWEKSupplier supplier) {
        List<SWEKParam> params = new ArrayList<>();
        params.add(new SWEKParam("provider", supplier.getSupplierName(), SWEKOperand.EQUALS));
        Map<SWEKParameter, List<SWEKParam>> paramsPerEventParameter = FilterManager.getFilter(supplier);
        for (List<SWEKParam> paramPerParameter : paramsPerEventParameter.values()) {
            params.addAll(paramPerParameter);
        }
        return params;
    }

    private static void downloadForAllDates(SWEKSupplier supplier) {
        Collection<Interval> allIntervals = JHVEventCache.getAllRequestIntervals(supplier);
        for (Interval interval : allIntervals) {
            startDownloadSupplier(supplier, interval);
        }
    }

    @Override
    public void handleRequestForInterval(SWEKSupplier supplier, Interval interval) {
        startDownloadSupplier(supplier, interval);
    }

    private static void startDownloadSupplier(SWEKSupplier supplier, Interval interval) {
        List<SWEKParam> params = defineParameters(supplier);
        for (Interval intt : Interval.splitInterval(interval, 2)) {
            if (intt.start < System.currentTimeMillis() + SIXHOURS) {
                DownloadWorker worker = new DownloadWorker(supplier, intt, params);
                SWEKTreeModel.setStartLoading(supplier.getGroup());
                addToDownloaderMap(worker);
                downloadEventPool.execute(worker);
            }
        }
    }

    private static class ComparePriority<T extends DownloadWorker> implements Comparator<T> {
        @Override
        public int compare(T l1, T l2) {
            long start = Layers.getStartTime();
            long d1 = l1.getRequestInterval().end - start;
            long d2 = l2.getRequestInterval().end - start;
            return d1 == d2 ? 0 : (d1 < d2 ? 1 : -1);
        }
    }

}
