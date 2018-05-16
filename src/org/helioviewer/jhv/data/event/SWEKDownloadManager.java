package org.helioviewer.jhv.data.event;

import java.util.ArrayList;
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
import org.helioviewer.jhv.data.gui.SWEKTreeModel;
import org.helioviewer.jhv.data.gui.filter.FilterManager;
import org.helioviewer.jhv.data.gui.filter.FilterManagerListener;
import org.helioviewer.jhv.threads.JHVThread;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SWEKDownloadManager implements FilterManagerListener {

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
    private static final HashMap<SWEKSupplier, ArrayList<SWEKDownloadWorker>> supplierMap = new HashMap<>();

    private static final SWEKDownloadManager instance = new SWEKDownloadManager();

    private SWEKDownloadManager() {
        FilterManager.addFilterManagerListener(this);
    }

    private static void stopDownloadSupplier(SWEKSupplier supplier, boolean keepActive) {
        ArrayList<SWEKDownloadWorker> workerList = supplierMap.get(supplier);
        if (workerList != null) {
            for (Iterator<SWEKDownloadWorker> it = workerList.iterator(); it.hasNext();) {
                SWEKDownloadWorker worker = it.next();
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

    public static void workerForcedToStop(SWEKDownloadWorker worker) {
        removeFromDownloaderMap(worker);
        JHVEventCache.intervalsNotDownloaded(worker.getSupplier(), worker.getRequestInterval());
    }

    public static void workerFinished(SWEKDownloadWorker worker) {
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

    private static void removeFromDownloaderMap(SWEKDownloadWorker worker) {
        ArrayList<SWEKDownloadWorker> workerList = supplierMap.get(worker.getSupplier());
        if (workerList != null)
            workerList.remove(worker);
        if (workerList == null || workerList.isEmpty())
            SWEKTreeModel.setStopLoading(worker.getSupplier().getGroup());
    }

    private static void addToDownloaderMap(SWEKDownloadWorker worker) {
        ArrayList<SWEKDownloadWorker> workerList = supplierMap.computeIfAbsent(worker.getSupplier(), k -> new ArrayList<>());
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
        for (Interval interval : JHVEventCache.getAllRequestIntervals(supplier)) {
            startDownloadSupplier(supplier, interval);
        }
    }

    public static void startDownloadSupplier(SWEKSupplier supplier, Interval interval) {
        List<SWEKParam> params = defineParameters(supplier);
        for (Interval intt : Interval.splitInterval(interval, 2)) {
            if (intt.start < System.currentTimeMillis() + SIXHOURS) {
                SWEKDownloadWorker worker = new SWEKDownloadWorker(supplier, intt, params);
                SWEKTreeModel.setStartLoading(supplier.getGroup());
                addToDownloaderMap(worker);
                downloadEventPool.execute(worker);
            }
        }
    }

    private static class ComparePriority<T extends SWEKDownloadWorker> implements Comparator<T> {
        @Override
        public int compare(T l1, T l2) {
            return Long.compare(l2.getRequestInterval().end, l1.getRequestInterval().end);
        }
    }

}
