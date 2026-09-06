package org.helioviewer.jhv.event;

import java.awt.EventQueue;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.filter.FilterManager;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.time.Interval;

import com.google.common.collect.ArrayListMultimap;

public class SWEKDownloader {

    private static final int NUMBER_THREADS = 8;
    private static final FilterManager.Listener filterListener = SWEKDownloader::filtersChanged;
    private static Consumer<SWEKGroup> groupChanged = _ -> {};
    private static final ThreadPoolExecutor downloadPool = new ThreadPoolExecutor(
            NUMBER_THREADS, NUMBER_THREADS, 10000L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(2048),
            new AppThread.NamedThreadFactory("SWEK Download"),
            new ThreadPoolExecutor.DiscardPolicy());

    private record LoadedEvents(List<SolarEvent> events, List<SolarEvent.Link> associations) {}

    private static final class Worker implements Runnable, Comparable<Worker> {
        private final SWEKSupplier supplier;
        private final List<SWEK.Param> params;
        private final long start;
        private final long end;

        private volatile boolean cancelled;

        Worker(SWEKSupplier _supplier, List<SWEK.Param> _params, long _start, long _end) {
            supplier = _supplier;
            params = _params;
            start = _start;
            end = _end;
        }

        @Override
        public void run() {
            try {
                if (!ensureStored()) {
                    finishFailure(null);
                    return;
                }
                if (cancelled)
                    return;

                LoadedEvents events = new LoadedEvents(
                        EventDatabase.events2Program(start, end, supplier, params),
                        EventDatabase.associations2Program(start, end, supplier));
                finishSuccess(events);
            } catch (Throwable t) {
                finishFailure(t);
            }
        }

        private void finishSuccess(LoadedEvents events) {
            if (!cancelled) {
                EventQueue.invokeLater(() -> {
                    if (!cancelled)
                        publish(events);
                });
            }
        }

        private void finishFailure(Throwable t) {
            if (cancelled)
                return;

            if (t != null && !AppThread.isInterrupted(t)) {
                Log.error("Error loading SWEK", t);
            }

            EventQueue.invokeLater(() -> {
                if (!cancelled)
                    workerFailed(this);
            });
        }

        private void publish(LoadedEvents events) {
            events.events().forEach(EventCache::addEvent);
            events.associations().forEach(EventCache::addAssociation);
            EventCache.fireEventCacheChanged();
            workerFinished(this);
        }

        private boolean ensureStored() throws Exception {
            if (EventDatabase.isStored(start, end, supplier))
                return true;
            if (!fetchAndStoreRemote())
                return false;

            return EventDatabase.addStoredInterval(start, end, supplier);
        }

        private boolean fetchAndStoreRemote() throws Exception {
            int page = 0;
            boolean overmax = true;
            while (overmax) {
                if (cancelled)
                    return false;

                SWEKHandler.RemotePage remotePage = supplier.source().handler().fetchPage(supplier, start, end, page);
                if (!EventDatabase.storeRemotePage(remotePage, supplier))
                    return false;
                overmax = remotePage.overmax();
                page++;
            }
            return !cancelled;
        }

        void stopWorker() {
            cancelled = true;
            downloadPool.remove(this);
        }

        @Override
        public int compareTo(Worker other) {
            return Long.compare(other.end, end);
        }
    }

    private static final ArrayListMultimap<SWEKSupplier, Worker> workerMap = ArrayListMultimap.create();

    public static void installFilterListener() {
        FilterManager.addListener(filterListener);
    }

    public static void removeFilterListener() {
        FilterManager.removeListener(filterListener);
    }

    public static void setGroupChangedCallback(Consumer<SWEKGroup> callback) {
        groupChanged = callback;
    }

    public static void clearGroupChangedCallback() {
        groupChanged = _ -> {};
    }

    public static boolean isGroupBusy(SWEKGroup group) {
        for (SWEKSupplier supplier : workerMap.keySet()) {
            if (supplier.group() == group)
                return true;
        }
        return false;
    }

    private static void updateGroupBusy(SWEKGroup group) {
        EventQueue.invokeLater(() -> groupChanged.accept(group));
    }

    static void stopDownloadSupplier(SWEKSupplier supplier, boolean keepActive) {
        for (Worker worker : workerMap.get(supplier))
            worker.stopWorker();
        workerMap.removeAll(supplier);
        EventCache.removeSupplier(supplier, keepActive);
        updateGroupBusy(supplier.group());
    }

    private static void workerFailed(Worker worker) {
        EventCache.requestFailed(worker.supplier, worker.start, worker.end);
        workerFinished(worker);
    }

    private static void workerFinished(Worker worker) {
        workerMap.remove(worker.supplier, worker);
        updateGroupBusy(worker.supplier.group());
    }

    private static void filtersChanged(SWEKSupplier supplier) {
        if (EventCache.isSupplierActive(supplier))
            stopDownloadSupplier(supplier, true);
    }

    static void startDownloadSupplier(SWEKSupplier supplier, List<Interval> intervals) {
        List<SWEK.Param> params = FilterManager.getFilters(supplier);
        SWEKGroup group = supplier.group();
        boolean started = false;
        for (Interval interval : intervals) {
            for (Interval intt : Interval.splitInterval(interval, 2)) {
                Worker worker = new Worker(supplier, params, intt.start(), intt.end());
                downloadPool.execute(worker);
                workerMap.put(supplier, worker);
                started = true;
            }
        }
        if (started)
            updateGroupBusy(group);
    }
}
