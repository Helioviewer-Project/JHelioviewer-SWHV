package org.helioviewer.jhv.events;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.events.filter.FilterManager;
import org.helioviewer.jhv.threads.JHVThread;

import com.google.common.collect.ArrayListMultimap;

@SuppressWarnings({"rawtypes", "unchecked"})
class SWEKDownloader implements FilterManager.Listener {

    private static final int NUMBER_THREADS = 8;
    private static final long SIXHOURS = 1000 * 60 * 60 * 6;
    private static final ThreadPoolExecutor downloadPool = new ThreadPoolExecutor(NUMBER_THREADS, NUMBER_THREADS, 10000L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(2048, new ComparePriority()),
            new JHVThread.NamedThreadFactory("SWEK Download"),
            new ThreadPoolExecutor.DiscardPolicy()) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            JHVThread.afterExecute(r, t);
        }
    };

    private record Worker(SWEKSupplier supplier, List<SWEK.Param> params, long start, long end) implements Runnable {
        @Override
        public void run() {
            boolean success = supplier.getSource().handler().remote2db(supplier, start, end, params);
            if (success) {
                List<Pair<Integer, Integer>> assocList = EventDatabase.associations2Program(start, end, supplier);
                List<JHVEvent> eventList = EventDatabase.events2Program(start, end, supplier, params);
                EventQueue.invokeLater(() -> {
                    assocList.forEach(JHVEventCache::addAssociation);
                    eventList.forEach(JHVEventCache::addEvent);
                    JHVEventCache.fireEventCacheChanged();

                    workerFinished(supplier, this);
                });
                EventDatabase.addDaterange2db(start, end, supplier);
            } else {
                EventQueue.invokeLater(() -> workerForcedToStop(supplier, this));
            }
        }

        void stopWorker() { // TBD
        }

    }

    private static final SWEKDownloader instance = new SWEKDownloader();
    private static final ArrayListMultimap<SWEKSupplier, Worker> workerMap = ArrayListMultimap.create();

    private SWEKDownloader() {
        FilterManager.addListener(this);
    }

    private static void stopDownloadSupplier(SWEKSupplier supplier, boolean keepActive) {
        workerMap.get(supplier).forEach(worker -> {
            worker.stopWorker();
            JHVEventCache.intervalNotDownloaded(supplier, worker.start(), worker.end());
        });
        JHVEventCache.removeSupplier(supplier, keepActive);
        supplier.getGroup().stoppedDownload();
    }

    private static void workerForcedToStop(SWEKSupplier supplier, Worker worker) {
        JHVEventCache.intervalNotDownloaded(supplier, worker.start(), worker.end());
        workerFinished(supplier, worker);
    }

    private static void workerFinished(SWEKSupplier supplier, Worker worker) {
        workerMap.remove(supplier, worker);
        if (workerMap.get(supplier).isEmpty())
            supplier.getGroup().stoppedDownload();
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
            startDownloadSupplier(supplier, JHVEventCache.getAllRequestIntervals(supplier));
        }
    }

    private static List<SWEK.Param> defineParameters(SWEKSupplier supplier) {
        List<SWEK.Param> params = new ArrayList<>();
        params.add(new SWEK.Param("provider", supplier.getSupplierName(), SWEK.Operand.EQUALS));
        FilterManager.getFilter(supplier).values().forEach(params::addAll);
        return params;
    }

    static void startDownloadSupplier(SWEKSupplier supplier, List<Interval> intervals) {
        List<SWEK.Param> params = defineParameters(supplier);
        for (Interval interval : intervals) {
            for (Interval intt : Interval.splitInterval(interval, 2)) {
                if (intt.start < System.currentTimeMillis() + SIXHOURS) {
                    Worker worker = new Worker(supplier, params, intt.start, intt.end);
                    downloadPool.execute(worker);
                    workerMap.put(supplier, worker);
                    supplier.getGroup().startedDownload();
                }
            }
        }
    }

    private static class ComparePriority<T extends Worker> implements Comparator<T> {
        @Override
        public int compare(T l1, T l2) {
            return Long.compare(l2.end(), l1.end());
        }
    }

}
