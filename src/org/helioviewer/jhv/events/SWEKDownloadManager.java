package org.helioviewer.jhv.events;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.events.gui.SWEKTreeModel;
import org.helioviewer.jhv.events.gui.filter.FilterManager;
import org.helioviewer.jhv.events.gui.filter.FilterManagerListener;
import org.helioviewer.jhv.threads.JHVThread;

import com.google.common.collect.ArrayListMultimap;

@SuppressWarnings({"rawtypes", "unchecked"})
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
    private static final ArrayListMultimap<SWEKSupplier, SWEKDownloadWorker> workerMap = ArrayListMultimap.create();

    private static final SWEKDownloadManager instance = new SWEKDownloadManager();

    private SWEKDownloadManager() {
        FilterManager.addListener(this);
    }

    private static void stopDownloadSupplier(SWEKSupplier supplier, boolean keepActive) {
        workerMap.get(supplier).forEach(worker -> {
            worker.stopWorker();
            JHVEventCache.intervalNotDownloaded(supplier, worker.getStart(), worker.getEnd());
        });
        SWEKTreeModel.setStopLoading(supplier.getGroup());
        JHVEventCache.removeSupplier(supplier, keepActive);
    }

    static void workerForcedToStop(SWEKDownloadWorker worker) {
        removeFromDownloaderMap(worker);
        JHVEventCache.intervalNotDownloaded(worker.getSupplier(), worker.getStart(), worker.getEnd());
    }

    static void workerFinished(SWEKDownloadWorker worker) {
        removeFromDownloaderMap(worker);
    }

    static void activateSupplier(SWEKSupplier supplier, boolean active) {
        if (active)
            JHVEventCache.supplierActivated(supplier);
        else
            stopDownloadSupplier(supplier, false);
    }

    @Override
    public void filtersChanged(SWEKSupplier supplier) {
        stopDownloadSupplier(supplier, true);
        if (supplier.isSelected()) {
            JHVEventCache.getAllRequestIntervals(supplier).forEach(interval -> startDownloadSupplier(supplier, interval));
        }
    }

    private static void removeFromDownloaderMap(SWEKDownloadWorker worker) {
        SWEKSupplier supplier = worker.getSupplier();
        workerMap.remove(supplier, worker);
        if (workerMap.get(supplier).isEmpty())
            SWEKTreeModel.setStopLoading(supplier.getGroup());
    }

    private static List<SWEKParam> defineParameters(SWEKSupplier supplier) {
        List<SWEKParam> params = new ArrayList<>();
        params.add(new SWEKParam("provider", supplier.getSupplierName(), SWEKOperand.EQUALS));
        FilterManager.getFilter(supplier).values().forEach(params::addAll);
        return params;
    }

    static void startDownloadSupplier(SWEKSupplier supplier, Interval interval) {
        List<SWEKParam> params = defineParameters(supplier);
        for (Interval intt : Interval.splitInterval(interval, 2)) {
            if (intt.start < System.currentTimeMillis() + SIXHOURS) {
                SWEKDownloadWorker worker = new SWEKDownloadWorker(supplier, intt.start, intt.end, params);
                downloadEventPool.execute(worker);
                workerMap.put(supplier, worker);
                SWEKTreeModel.setStartLoading(supplier.getGroup());
            }
        }
    }

    private static class ComparePriority<T extends SWEKDownloadWorker> implements Comparator<T> {
        @Override
        public int compare(T l1, T l2) {
            return Long.compare(l2.getEnd(), l1.getEnd());
        }
    }

}
