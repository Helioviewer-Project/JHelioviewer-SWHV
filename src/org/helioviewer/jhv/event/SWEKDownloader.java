package org.helioviewer.jhv.event;

import java.awt.EventQueue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.filter.FilterManager;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.RequestCache;
import org.helioviewer.jhv.time.TimeUtils;

import com.google.common.collect.ArrayListMultimap;

public class SWEKDownloader {

    private static final double FACTOR = 0.2;
    private static final long REQUEST_REFRESH_INTERVAL = 60 * 60 * 1000L;
    private static final long FUTURE_REQUEST_MARGIN = 6 * REQUEST_REFRESH_INTERVAL;

    private static final int NUMBER_THREADS = 8;
    private static final FilterManager.Listener filterListener = SWEKDownloader::filtersChanged;
    private static Consumer<SWEKGroup> groupChanged = _ -> {};
    private static final ThreadPoolExecutor downloadPool = new ThreadPoolExecutor(
            NUMBER_THREADS, NUMBER_THREADS, 10000L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(2048),
            new AppThread.NamedThreadFactory("SWEK Download"),
            new ThreadPoolExecutor.DiscardPolicy());

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

                finishSuccess(EventDatabase.loadEvents(start, end, supplier, params));
            } catch (Throwable t) {
                finishFailure(t);
            }
        }

        private void finishSuccess(EventDatabase.EventBatch events) {
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

        private void publish(EventDatabase.EventBatch events) {
            EventCache.replaceEvents(events.sequence(), events.events(), events.associations());
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

    private static final Map<SWEKSupplier, RequestCache> requestedIntervals = new HashMap<>();
    private static final ArrayListMultimap<SWEKSupplier, Worker> workerMap = ArrayListMultimap.create();

    private static void requestFailed(SWEKSupplier eventType, long start, long end) {
        RequestCache cache = requestedIntervals.get(eventType);
        if (cache != null)
            cache.removeRequestedInterval(start, end);
    }

    public static boolean isSupplierActive(SWEKSupplier supplier) {
        return requestedIntervals.containsKey(supplier);
    }

    public static void setSupplierActive(SWEKSupplier supplier, boolean active) {
        if (active) {
            requestedIntervals.computeIfAbsent(supplier, _ -> new RequestCache());
            EventCache.fireEventCacheChanged();
        } else {
            stopDownloadSupplier(supplier, false);
        }
    }

    public static void requestForInterval(long start, long end) {
        long now = System.currentTimeMillis();
        long requestHorizon = now / REQUEST_REFRESH_INTERVAL * REQUEST_REFRESH_INTERVAL + FUTURE_REQUEST_MARGIN;
        long visibleEnd = Math.min(end, requestHorizon);
        if (start >= visibleEnd)
            return;

        long deltaT = Math.max((long) ((visibleEnd - start) * FACTOR), TimeUtils.DAY_IN_MILLIS);
        long requestStart = start - deltaT;
        long requestEnd = Math.min(visibleEnd + deltaT, requestHorizon);
        for (Map.Entry<SWEKSupplier, RequestCache> entry : requestedIntervals.entrySet()) {
            RequestCache cache = entry.getValue();
            if (!cache.getMissingIntervals(start, visibleEnd).isEmpty())
                startDownloadSupplier(entry.getKey(), cache.adaptRequestCache(requestStart, requestEnd));
        }
    }

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

    private static void stopDownloadSupplier(SWEKSupplier supplier, boolean keepActive) {
        for (Worker worker : workerMap.get(supplier))
            worker.stopWorker();
        workerMap.removeAll(supplier);
        if (keepActive)
            requestedIntervals.put(supplier, new RequestCache());
        else
            requestedIntervals.remove(supplier);
        EventCache.removeSupplier(supplier);
        updateGroupBusy(supplier.group());
    }

    private static void workerFailed(Worker worker) {
        requestFailed(worker.supplier, worker.start, worker.end);
        workerFinished(worker);
    }

    private static void workerFinished(Worker worker) {
        workerMap.remove(worker.supplier, worker);
        updateGroupBusy(worker.supplier.group());
    }

    private static void filtersChanged(SWEKSupplier supplier) {
        if (isSupplierActive(supplier))
            stopDownloadSupplier(supplier, true);
    }

    private static void startDownloadSupplier(SWEKSupplier supplier, List<Interval> intervals) {
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
