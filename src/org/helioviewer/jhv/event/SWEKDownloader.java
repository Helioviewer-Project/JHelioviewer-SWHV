package org.helioviewer.jhv.event;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
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

public class SWEKDownloader implements FilterManager.Listener {

    private static final int NUMBER_THREADS = 8;
    private static final long SIXHOURS = 1000 * 60 * 60 * 6;
    private static Consumer<SWEKGroup> groupChanged = _ -> {};
    private static final ThreadPoolExecutor downloadPool = new ThreadPoolExecutor(
            NUMBER_THREADS, NUMBER_THREADS, 10000L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>(2048),
            new AppThread.NamedThreadFactory("SWEK Download"),
            new ThreadPoolExecutor.DiscardPolicy());

    private record LoadedEvents(List<JHVEvent.Link> associations, List<JHVEvent> events) {}

    private static final class Download implements Callable<LoadedEvents> {
        private final SWEKSupplier supplier;
        private final List<SWEK.Param> params;
        private final long start;
        private final long end;

        private volatile boolean cancelled;

        Download(SWEKSupplier _supplier, List<SWEK.Param> _params, long _start, long _end) {
            supplier = _supplier;
            params = _params;
            start = _start;
            end = _end;
        }

        @Override
        public LoadedEvents call() {
            try {
                if (!isDownloaded() && !fetchAndStoreRemote())
                    return null;
            } catch (Exception e) {
                if (!AppThread.isInterrupted(e))
                    Log.error("Error loading SWEK", e);
                return null;
            }
            if (cancelled)
                return null;

            return new LoadedEvents(
                    EventDatabase.associations2Program(start, end, supplier),
                    EventDatabase.events2Program(start, end, supplier, params));
        }

        private boolean fetchAndStoreRemote() throws Exception {
            List<JHVEvent.LinkRef> associations = new ArrayList<>();
            int page = 0;
            boolean overmax = true;
            while (overmax) {
                if (cancelled)
                    return false;

                SWEKHandler.RemotePage remotePage = supplier.source().handler().fetchPage(supplier, start, end, params, page);
                EventDatabase.storeEvents(remotePage.events(), supplier);
                associations.addAll(remotePage.associations());
                overmax = remotePage.overmax();
                page++;
            }
            if (cancelled)
                return false;

            return EventDatabase.storeAssociations(associations) != -1;
        }

        private boolean isDownloaded() {
            for (Interval interval : EventDatabase.db2daterange(supplier)) {
                if (interval.start() <= start && interval.end() >= end) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class Worker extends FutureTask<LoadedEvents> implements Comparable<Worker> {
        private final Download download;

        Worker(SWEKSupplier _supplier, List<SWEK.Param> _params, long _start, long _end) {
            this(new Download(_supplier, _params, _start, _end));
        }

        private Worker(Download _download) {
            super(_download);
            download = _download;
        }

        @Override
        protected void done() {
            if (download.cancelled)
                return;

            EventQueue.invokeLater(() -> {
                if (download.cancelled)
                    return;
                try {
                    LoadedEvents events = get();
                    if (events != null)
                        onSuccess(events);
                    else
                        onFailure(null);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    onFailure(e.getCause());
                } catch (CancellationException ignored) {
                    // Cancelled workers are removed by stopDownloadSupplier.
                }
            });
        }

        private void onSuccess(LoadedEvents events) {
            events.associations().forEach(JHVEventCache::addAssociation);
            events.events().forEach(JHVEventCache::addEvent);
            JHVEventCache.fireEventCacheChanged();
            EventDatabase.addDaterange2db(download.start, download.end, download.supplier);
            workerFinished(this);
        }

        private void onFailure(Throwable t) {
            if (t != null && !AppThread.isInterrupted(t))
                Log.error(t);
            workerFailed(this);
        }

        void stopWorker() {
            download.cancelled = true;
            cancel(false);
            downloadPool.remove(this);
        }

        @Override
        public int compareTo(Worker other) {
            return Long.compare(other.download.end, download.end);
        }
    }

    private static final SWEKDownloader instance = new SWEKDownloader();
    private static final ArrayListMultimap<SWEKSupplier, Worker> workerMap = ArrayListMultimap.create();

    private SWEKDownloader() {
        FilterManager.addListener(this);
    }

    public static void setGroupChangedCallback(Consumer<SWEKGroup> callback) {
        groupChanged = callback;
    }

    public static void clearGroupChangedCallback() {
        groupChanged = _ -> {};
    }

    public static boolean isGroupBusy(SWEKGroup group) {
        for (SWEKSupplier supplier : workerMap.keySet()) {
            if (supplier.group() == group && !workerMap.get(supplier).isEmpty())
                return true;
        }
        return false;
    }

    private static void updateGroupBusy(SWEKGroup group) {
        EventQueue.invokeLater(() -> groupChanged.accept(group));
    }

    static void stopDownloadSupplier(SWEKSupplier supplier, boolean keepActive) {
        for (Worker worker : workerMap.get(supplier)) {
            worker.stopWorker();
            JHVEventCache.intervalNotDownloaded(supplier, worker.download.start, worker.download.end);
        }
        workerMap.removeAll(supplier);
        JHVEventCache.removeSupplier(supplier, keepActive);
        updateGroupBusy(supplier.group());
    }

    private static void workerFailed(Worker worker) {
        JHVEventCache.intervalNotDownloaded(worker.download.supplier, worker.download.start, worker.download.end);
        workerFinished(worker);
    }

    private static void workerFinished(Worker worker) {
        workerMap.remove(worker.download.supplier, worker);
        updateGroupBusy(worker.download.supplier.group());
    }

    @Override
    public void filtersChanged(SWEKSupplier supplier) {
        stopDownloadSupplier(supplier, true);
        if (JHVEventCache.isSupplierActive(supplier)) {
            startDownloadSupplier(supplier, JHVEventCache.getAllRequestIntervals(supplier));
        }
    }

    private static List<SWEK.Param> defineParameters(SWEKSupplier supplier) {
        List<SWEK.Param> params = new ArrayList<>();
        FilterManager.getFilters(supplier).values().forEach(params::addAll);
        return params;
    }

    static void startDownloadSupplier(SWEKSupplier supplier, List<Interval> intervals) {
        List<SWEK.Param> params = defineParameters(supplier);
        SWEKGroup group = supplier.group();
        long latestStart = System.currentTimeMillis() + SIXHOURS;
        for (Interval interval : intervals) {
            for (Interval intt : Interval.splitInterval(interval, 2)) {
                if (intt.start() < latestStart) {
                    Worker worker = new Worker(supplier, params, intt.start(), intt.end());
                    downloadPool.execute(worker);
                    workerMap.put(supplier, worker);
                    updateGroupBusy(group);
                }
            }
        }
    }
}
