package org.helioviewer.jhv.event;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.event.filter.FilterManager;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.RequestCache;
import org.helioviewer.jhv.time.TimeUtils;

public class SWEKDownloader {

    private static final double FACTOR = 0.2;
    private static final long REQUEST_REFRESH_INTERVAL = 60 * 60 * 1000L;
    private static final long FUTURE_REQUEST_MARGIN = 6 * REQUEST_REFRESH_INTERVAL;

    private static final int NUMBER_THREADS = 8;
    private static final FilterManager.Listener filterListener = SWEKDownloader::filtersChanged;
    private static Consumer<SWEKGroup> groupChanged = _ -> {};
    private static final ThreadPoolExecutor downloadPool = AppThread.createIdleExecutor(
            "SWEK-Download", NUMBER_THREADS, new PriorityBlockingQueue<>(2048), new ThreadPoolExecutor.DiscardPolicy());

    private static final class SupplierRequests {
        private final SWEKSupplier supplier;
        private final List<SWEK.Param> params;
        private final RequestCache intervals = new RequestCache();
        private final List<Worker> workers = new ArrayList<>();
        private volatile boolean cancelled;

        SupplierRequests(SWEKSupplier _supplier) {
            supplier = _supplier;
            params = FilterManager.getFilters(supplier);
        }
    }

    private static final class Worker implements Runnable, Comparable<Worker> {
        private final SupplierRequests requests;
        private final long start;
        private final long end;

        Worker(SupplierRequests _requests, long _start, long _end) {
            requests = _requests;
            start = _start;
            end = _end;
        }

        @Override
        public void run() {
            EventBatch events = null;
            try {
                if (ensureStored() && !requests.cancelled)
                    events = EventDatabase.loadEvents(start, end, requests.supplier, requests.params);
            } catch (Throwable t) {
                if (!requests.cancelled && !AppThread.isInterrupted(t))
                    Log.error("Error loading SWEK", t);
            }
            finish(events);
        }

        private void finish(@Nullable EventBatch events) {
            if (requests.cancelled)
                return;

            EventQueue.invokeLater(() -> {
                if (requests.cancelled)
                    return;
                try {
                    if (events == null)
                        requests.intervals.removeRequestedInterval(start, end);
                    else
                        EventCache.replaceEvents(events);
                } finally {
                    requests.workers.remove(this);
                    updateGroupBusy(requests.supplier.group());
                }
            });
        }

        private boolean ensureStored() throws Exception {
            if (EventDatabase.isStored(start, end, requests.supplier))
                return true;
            if (!fetchAndStoreRemote())
                return false;

            return EventDatabase.addStoredInterval(start, end, requests.supplier);
        }

        private boolean fetchAndStoreRemote() throws Exception {
            int page = 0;
            boolean overmax = true;
            while (overmax) {
                if (requests.cancelled)
                    return false;

                SWEKHandler.RemotePage remotePage = requests.supplier.source().handler().fetchPage(requests.supplier, start, end, page);
                if (!EventDatabase.storeRemotePage(remotePage, requests.supplier))
                    return false;
                overmax = remotePage.overmax();
                page++;
            }
            return !requests.cancelled;
        }

        @Override
        public int compareTo(Worker other) {
            return Long.compare(other.end, end);
        }
    }

    private static final Map<SWEKSupplier, SupplierRequests> activeSuppliers = new HashMap<>();

    public static boolean isSupplierActive(SWEKSupplier supplier) {
        return activeSuppliers.containsKey(supplier);
    }

    public static void setSupplierActive(SWEKSupplier supplier, boolean active) {
        if (active) {
            activeSuppliers.computeIfAbsent(supplier, SupplierRequests::new);
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
        for (SupplierRequests requests : activeSuppliers.values()) {
            if (!requests.intervals.getMissingIntervals(start, visibleEnd).isEmpty())
                startDownloadSupplier(requests, requests.intervals.adaptRequestCache(requestStart, requestEnd));
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
        for (SupplierRequests requests : activeSuppliers.values()) {
            if (requests.supplier.group() == group && !requests.workers.isEmpty())
                return true;
        }
        return false;
    }

    private static void updateGroupBusy(SWEKGroup group) {
        EventQueue.invokeLater(() -> groupChanged.accept(group));
    }

    private static void stopDownloadSupplier(SWEKSupplier supplier, boolean keepActive) {
        SupplierRequests requests = activeSuppliers.get(supplier);
        if (requests != null) {
            requests.cancelled = true;
            requests.workers.forEach(downloadPool::remove);
            requests.workers.clear();
        }
        if (keepActive)
            activeSuppliers.put(supplier, new SupplierRequests(supplier));
        else
            activeSuppliers.remove(supplier);
        EventCache.removeSupplier(supplier);
        updateGroupBusy(supplier.group());
    }

    private static void filtersChanged(SWEKSupplier supplier) {
        if (isSupplierActive(supplier))
            stopDownloadSupplier(supplier, true);
    }

    private static void startDownloadSupplier(SupplierRequests requests, List<Interval> intervals) {
        SWEKGroup group = requests.supplier.group();
        boolean started = false;
        for (Interval interval : intervals) {
            for (Interval intt : Interval.splitInterval(interval, 2)) {
                Worker worker = new Worker(requests, intt.start(), intt.end());
                downloadPool.execute(worker);
                requests.workers.add(worker);
                started = true;
            }
        }
        if (started)
            updateGroupBusy(group);
    }
}
