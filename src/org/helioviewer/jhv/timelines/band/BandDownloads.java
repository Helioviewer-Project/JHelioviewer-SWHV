package org.helioviewer.jhv.timelines.band;

import java.util.List;
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

import com.google.common.collect.ArrayListMultimap;

final class BandDownloads {

    private static final int DOWNLOAD_THREADS = 8;
    private static final ThreadPoolExecutor downloadPool = new ThreadPoolExecutor(
            DOWNLOAD_THREADS, DOWNLOAD_THREADS, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new AppThread.NamedThreadFactory("Timeline-Download"));
    private static final ArrayListMultimap<Band, Future<BandData>> workers = ArrayListMultimap.create();
    private static boolean purgeNeeded;

    private static void pruneFinished(Band band) {
        workers.get(band).removeIf(Future::isDone);
    }

    static boolean hasCatalog(Band band) {
        String baseUrl = band.getBandType().getBaseUrl();
        return !baseUrl.isEmpty() && BandReaderHapi.hasCatalog(baseUrl);
    }

    static void start(Band band, List<Interval> intervals) {
        if (purgeNeeded) {
            downloadPool.purge();
            purgeNeeded = false;
        }

        String baseUrl = band.getBandType().getBaseUrl();
        pruneFinished(band);
        for (Interval interval : intervals) {
            Future<BandData> download = Task.submit(downloadPool,
                    BandReaderHapi.dataRequest(baseUrl, interval.start(), interval.end()),
                    data -> acceptData(band, data),
                    t -> downloadFailed(band, interval, t));
            workers.put(band, download);
        }
        band.downloadStateChanged();
    }

    static void stop(Band band) {
        workers.get(band).forEach(worker -> worker.cancel(true));
        workers.removeAll(band);
        purgeNeeded = true;
    }

    static boolean isActive(Band band) {
        pruneFinished(band);
        return !workers.get(band).isEmpty();
    }

    private static void acceptData(Band band, BandData data) {
        band.downloadSucceeded(data);
    }

    private static void downloadFailed(Band band, Interval interval, Throwable t) {
        if (!(t instanceof CancellationException)) {
            String context = band.getBandType().getName() + " [" +
                    TimeUtils.formatShort(interval.start()) + " - " + TimeUtils.formatShort(interval.end()) + "]";
            Log.error(context, t);
        }
        band.requestFailed(interval);
    }

}
