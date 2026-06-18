package org.helioviewer.jhv.event;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.filter.FilterManager;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.time.Interval;

import com.google.common.collect.ArrayListMultimap;

class SWEKDownloader implements FilterManager.Listener {

    private static final int NUMBER_THREADS = 8;
    private static final long SIXHOURS = 1000 * 60 * 60 * 6;
    private static final ThreadPoolExecutor downloadPool = new ThreadPoolExecutor(
            NUMBER_THREADS, NUMBER_THREADS, 10000L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(2048),
            new AppThread.NamedThreadFactory("SWEK Download"),
            new ThreadPoolExecutor.DiscardPolicy());

    private record Worker(SWEKSupplier supplier, List<SWEK.Param> params,
                          long start, long end) implements Runnable, Comparable<Worker> {
        @Override
        public void run() {
            try {
                download();
            } catch (Throwable t) {
                if (!AppThread.isInterrupted(t))
                    Log.error(t);
            }
        }

        private void download() {
            boolean success = loadRemote();
            if (success) {
                List<JHVEvent.Link> assocList = EventDatabase.associations2Program(start, end, supplier);
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

        private boolean loadRemote() {
            if (isDownloaded())
                return true;

            List<JHVEvent.LinkRef> associations = new ArrayList<>();
            if (!supplier.getSource().handler().fetch(supplier, start, end, params, page -> storePage(page, associations)))
                return false;

            return EventDatabase.storeAssociations(associations) != -1;
        }

        private void storePage(SWEKHandler.RemotePage page, List<JHVEvent.LinkRef> associations) {
            EventDatabase.storeEvents(page.events(), supplier);
            associations.addAll(page.associations());
        }

        private boolean isDownloaded() {
            for (Interval interval : EventDatabase.db2daterange(supplier)) {
                if (interval.start() <= start && interval.end() >= end) {
                    return true;
                }
            }
            return false;
        }

        void stopWorker() { // TBD
        }

        @Override
        public int compareTo(Worker other) {
            return Long.compare(other.end, end);
        }
    }

    private static final SWEKDownloader instance = new SWEKDownloader();
    private static final ArrayListMultimap<SWEKSupplier, Worker> workerMap = ArrayListMultimap.create();

    private SWEKDownloader() {
        FilterManager.addListener(this);
    }

    private static void updateGroupBusy(SWEKGroup group) {
        for (SWEKSupplier supplier : workerMap.keySet()) {
            if (supplier.getGroup() == group && !workerMap.get(supplier).isEmpty()) {
                group.startedDownload();
                return;
            }
        }
        group.stoppedDownload();
    }

    private static void stopDownloadSupplier(SWEKSupplier supplier, boolean keepActive) {
        workerMap.get(supplier).forEach(worker -> {
            worker.stopWorker();
            JHVEventCache.intervalNotDownloaded(supplier, worker.start(), worker.end());
        });
        JHVEventCache.removeSupplier(supplier, keepActive);
        updateGroupBusy(supplier.getGroup());
    }

    private static void workerForcedToStop(SWEKSupplier supplier, Worker worker) {
        JHVEventCache.intervalNotDownloaded(supplier, worker.start(), worker.end());
        workerFinished(supplier, worker);
    }

    private static void workerFinished(SWEKSupplier supplier, Worker worker) {
        workerMap.remove(supplier, worker);
        updateGroupBusy(supplier.getGroup());
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
        if (supplier.isActive()) {
            startDownloadSupplier(supplier, JHVEventCache.getAllRequestIntervals(supplier));
        }
    }

    private static List<SWEK.Param> defineParameters(SWEKSupplier supplier) {
        List<SWEK.Param> params = new ArrayList<>();
        FilterManager.getFilter(supplier).values().forEach(params::addAll);
        return params;
    }

    static void startDownloadSupplier(SWEKSupplier supplier, List<Interval> intervals) {
        List<SWEK.Param> params = defineParameters(supplier);
        SWEKGroup group = supplier.getGroup();
        for (Interval interval : intervals) {
            for (Interval intt : Interval.splitInterval(interval, 2)) {
                if (intt.start() < System.currentTimeMillis() + SIXHOURS) {
                    Worker worker = new Worker(supplier, params, intt.start(), intt.end());
                    downloadPool.execute(worker);
                    workerMap.put(supplier, worker);
                    updateGroupBusy(group);
                }
            }
        }
    }
}
