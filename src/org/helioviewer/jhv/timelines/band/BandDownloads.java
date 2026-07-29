package org.helioviewer.jhv.timelines.band;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.Interval;
import org.helioviewer.jhv.time.TimeUtils;

final class BandDownloads {

    private static final int DOWNLOAD_THREADS = 8;
    private static final ThreadPoolExecutor downloadPool = new ThreadPoolExecutor(
            DOWNLOAD_THREADS, DOWNLOAD_THREADS, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new AppThread.NamedThreadFactory("Timeline-Download"));
    private static final HashMap<RequestKey, Download> pendingDownloads = new HashMap<>();
    private static final ArrayList<Download> activeDownloads = new ArrayList<>();
    private static boolean submitPendingScheduled;
    private static boolean purgeNeeded;

    private record RequestKey(String dataset, Interval interval) {}

    private static final class Download {
        final RequestKey key;
        final String title;
        final HashMap<BandType, Band> subscribers = new HashMap<>();
        final HashSet<BandType> requestedTypes = new HashSet<>();
        Future<List<BandData>> future;

        Download(RequestKey _key, String _title) {
            key = _key;
            title = _title;
        }
    }

    static boolean hasCatalog(Band band) {
        String baseUrl = band.getBandType().getBaseUrl();
        return !baseUrl.isEmpty() && BandReaderHapi.hasCatalog(baseUrl);
    }

    static void start(Band band, List<Interval> intervals) {
        String baseUrl = band.getBandType().getBaseUrl();
        BandReaderHapi.DatasetRef dataset = BandReaderHapi.dataset(baseUrl);
        for (Interval interval : intervals) {
            RequestKey key = new RequestKey(dataset.key(), interval);
            Download download = findActiveDownload(key, band.getBandType());
            if (download == null) {
                download = pendingDownloads.computeIfAbsent(key,
                        ignored -> new Download(key, dataset.title()));
            }
            download.subscribers.put(band.getBandType(), band);
        }
        schedulePending();
        band.downloadStateChanged();
    }

    static void stop(Band band) {
        Iterator<Map.Entry<RequestKey, Download>> pending = pendingDownloads.entrySet().iterator();
        while (pending.hasNext()) {
            Download download = pending.next().getValue();
            download.subscribers.values().removeIf(subscriber -> subscriber == band);
            if (download.subscribers.isEmpty())
                pending.remove();
        }

        Iterator<Download> active = activeDownloads.iterator();
        while (active.hasNext()) {
            Download download = active.next();
            download.subscribers.values().removeIf(subscriber -> subscriber == band);
            if (download.subscribers.isEmpty()) {
                download.future.cancel(true);
                active.remove();
                purgeNeeded = true;
            }
        }
    }

    static boolean isActive(Band band) {
        return pendingDownloads.values().stream().anyMatch(download -> download.subscribers.containsValue(band))
                || activeDownloads.stream().anyMatch(download -> download.subscribers.containsValue(band));
    }

    private static Download findActiveDownload(RequestKey key, BandType type) {
        for (Download download : activeDownloads) {
            if (download.key.equals(key) && download.requestedTypes.contains(type))
                return download;
        }
        return null;
    }

    private static void schedulePending() {
        if (!submitPendingScheduled && !pendingDownloads.isEmpty()) {
            submitPendingScheduled = true;
            EventQueue.invokeLater(BandDownloads::submitPending);
        }
    }

    private static void submitPending() {
        submitPendingScheduled = false;
        if (purgeNeeded) {
            downloadPool.purge();
            purgeNeeded = false;
        }

        List<Download> downloads = List.copyOf(pendingDownloads.values());
        pendingDownloads.clear();
        for (Download download : downloads) {
            List<BandType> types = List.copyOf(download.subscribers.keySet());
            Interval interval = download.key.interval;
            download.requestedTypes.addAll(types);
            activeDownloads.add(download);
            try {
                Callable<List<BandData>> request =
                        BandReaderHapi.dataRequest(types, interval.start(), interval.end());
                download.future = Task.submit(downloadPool, request,
                        data -> acceptData(download, data),
                        t -> downloadFailed(download, t));
            } catch (RuntimeException e) {
                downloadFailed(download, e);
            }
        }
    }

    private static void acceptData(Download download, List<BandData> data) {
        if (!activeDownloads.remove(download))
            return;

        HashMap<BandType, BandData> byType = new HashMap<>();
        data.forEach(bandData -> byType.put(bandData.bandType(), bandData));
        download.subscribers.forEach((type, band) -> band.downloadSucceeded(byType.get(type)));
    }

    private static void downloadFailed(Download download, Throwable t) {
        if (!activeDownloads.remove(download))
            return;

        if (!(t instanceof CancellationException)) {
            Interval interval = download.key.interval;
            String context = download.title + " [" + TimeUtils.formatShort(interval.start())
                    + " - " + TimeUtils.formatShort(interval.end()) + "]";
            Log.error(context, t);
        }
        download.subscribers.values().forEach(band ->
                band.requestFailed(download.key.interval));
    }

}
