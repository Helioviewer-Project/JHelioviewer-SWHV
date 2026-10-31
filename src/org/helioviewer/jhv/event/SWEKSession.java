package org.helioviewer.jhv.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.UiThread;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.RequestCache;
import org.helioviewer.jhv.time.TimeUtils;

// One plugin activation. Mutable state and notifications belong to the UI thread;
// background details queries use the immutable catalog and serialized database.
public final class SWEKSession implements AutoCloseable {

    public record EventDetails(SolarEvent event, List<SolarEvent> relatedEvents) {}

    public interface Listener {
        void cacheUpdated();

        void highlightChanged();
    }

    private static final double FACTOR = 0.2;
    private static final long REQUEST_REFRESH_INTERVAL = 60 * 60 * 1000L;
    private static final long FUTURE_REQUEST_MARGIN = 6 * REQUEST_REFRESH_INTERVAL;

    private static final int NUMBER_THREADS = 8;
    private static final ThreadPoolExecutor downloadPool = AppThread.createIdleExecutor(
            "SWEK-Download", NUMBER_THREADS, new PriorityBlockingQueue<>(2048), new ThreadPoolExecutor.DiscardPolicy());

    private static final class SupplierRequests {
        private final SWEKSupplier supplier;
        private final List<SWEK.Param> params;
        private final RequestCache intervals = new RequestCache();
        private final List<Worker> workers = new ArrayList<>();
        private volatile boolean cancelled;

        SupplierRequests(SWEKSupplier _supplier, List<SWEK.Param> _params) {
            supplier = _supplier;
            params = _params;
        }
    }

    private final class Worker implements Runnable, Comparable<Worker> {
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
                    events = database.loadEvents(start, end, requests.supplier, requests.params);
            } catch (Throwable t) {
                if (!requests.cancelled && !AppThread.isInterrupted(t))
                    Log.error("Error loading SWEK", t);
            }
            finish(events);
        }

        private void finish(@Nullable EventBatch events) {
            if (requests.cancelled)
                return;

            UiThread.post(() -> {
                if (requests.cancelled)
                    return;
                try {
                    if (events == null)
                        requests.intervals.removeRequestedInterval(start, end);
                    else
                        replaceEvents(events);
                } finally {
                    requests.workers.remove(this);
                    updateGroupBusy(requests.supplier.group());
                }
            });
        }

        private boolean ensureStored() throws Exception {
            if (database.isStored(start, end, requests.supplier))
                return true;
            if (!fetchAndStoreRemote())
                return false;

            return database.addStoredInterval(start, end, requests.supplier);
        }

        private boolean fetchAndStoreRemote() throws Exception {
            int page = 0;
            boolean overmax = true;
            while (overmax) {
                if (requests.cancelled)
                    return false;

                SWEKHandler.RemotePage remotePage = requests.supplier.source().handler().fetchPage(requests.supplier, start, end, page, catalog.indexedParameters(requests.supplier));
                if (requests.cancelled || !database.storeRemotePage(remotePage, requests.supplier))
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

    private final Map<SWEKSupplier, SupplierRequests> activeSuppliers = new HashMap<>();

    private final SWEKCatalog catalog;
    private final EventDatabase database;
    private final HashMap<SWEKSupplier, List<SWEK.Param>> filters = new HashMap<>();
    private Consumer<SWEKGroup> groupChanged = _ -> {};
    private volatile boolean closed;
    private final Set<Listener> listeners = new HashSet<>();
    private EventCollection eventCollection = new EventCollection();

    private RelatedEvents lastHighlighted = null;

    public SWEKSession(SWEKCatalog _catalog) {
        catalog = _catalog;
        database = new EventDatabase(catalog);
    }

    public SWEKCatalog catalog() {
        return catalog;
    }

    private List<SWEK.Param> getFilters(SWEKSupplier supplier) {
        return filters.getOrDefault(supplier, List.of());
    }

    public void setFilters(SWEKSupplier supplier, List<SWEK.Param> values) {
        if (closed) return;
        if (values.isEmpty()) filters.remove(supplier);
        else filters.put(supplier, List.copyOf(values));
        if (isSupplierActive(supplier))
            stopDownloadSupplier(supplier, true);
    }

    public boolean isSupplierActive(SWEKSupplier supplier) {
        return activeSuppliers.containsKey(supplier);
    }

    public void setSupplierActive(SWEKSupplier supplier, boolean active) {
        if (closed) return;
        if (active) {
            activeSuppliers.computeIfAbsent(supplier, value -> new SupplierRequests(value, getFilters(value)));
            fireEventCacheChanged();
        } else {
            stopDownloadSupplier(supplier, false);
        }
    }

    public void requestForInterval(long start, long end) {
        if (closed) return;
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

    public boolean isGroupBusy(SWEKGroup group) {
        for (SupplierRequests requests : activeSuppliers.values()) {
            if (requests.supplier.group() == group && !requests.workers.isEmpty())
                return true;
        }
        return false;
    }

    public void setGroupChangedCallback(Consumer<SWEKGroup> callback) {
        if (!closed) groupChanged = callback;
    }

    public EventDetails getEventDetails(int id) throws Exception {
        if (closed) throw new CancellationException("SWEK session closed");
        return database.getEventDetails(id);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        for (SupplierRequests requests : activeSuppliers.values())
            cancel(requests);
        activeSuppliers.clear();
        groupChanged = _ -> {};
        listeners.clear();
        lastHighlighted = null;
        eventCollection = new EventCollection();
        filters.clear();
        database.close();
    }

    public void addListener(Listener listener) {
        if (!closed) listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void fireEventCacheChanged() {
        listeners.forEach(Listener::cacheUpdated);
    }

    public boolean isHighlighted(RelatedEvents event) {
        return event == lastHighlighted;
    }

    public void highlight(RelatedEvents event) {
        if (closed) return;
        if (event == lastHighlighted) return;
        lastHighlighted = event;
        fireHighlightChanged();
    }

    private void fireHighlightChanged() {
        listeners.forEach(Listener::highlightChanged);
    }

    void replaceEvents(EventBatch batch) {
        if (closed) return;
        boolean highlightCached = eventCollection.contains(lastHighlighted);
        eventCollection.replace(batch);
        if (highlightCached && !eventCollection.contains(lastHighlighted))
            highlight(null);
        fireEventCacheChanged();
    }

    @Nullable
    public RelatedEvents getRelatedEvents(int id) {
        return eventCollection.getRelatedEvents(id);
    }

    public List<RelatedEvents> getEvents(long start, long end) {
        return eventCollection.getEvents(start, end);
    }

    void removeSupplier(SWEKSupplier supplier) {
        if (lastHighlighted != null && lastHighlighted.getEvents().stream().anyMatch(event ->
                event.getSupplier() == supplier && eventCollection.getRelatedEvents(event.getUniqueID()) == lastHighlighted))
            highlight(null);
        eventCollection.removeSupplier(supplier);
        fireEventCacheChanged();
    }

    private void updateGroupBusy(SWEKGroup group) {
        UiThread.post(() -> {
            if (!closed) groupChanged.accept(group);
        });
    }

    private static void cancel(SupplierRequests requests) {
        requests.cancelled = true;
        requests.workers.forEach(downloadPool::remove);
        requests.workers.clear();
    }

    private void stopDownloadSupplier(SWEKSupplier supplier, boolean keepActive) {
        SupplierRequests requests = activeSuppliers.get(supplier);
        if (requests != null)
            cancel(requests);
        if (keepActive)
            activeSuppliers.put(supplier, new SupplierRequests(supplier, getFilters(supplier)));
        else
            activeSuppliers.remove(supplier);
        removeSupplier(supplier);
        updateGroupBusy(supplier.group());
    }

    private void startDownloadSupplier(SupplierRequests requests, List<Interval> intervals) {
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
