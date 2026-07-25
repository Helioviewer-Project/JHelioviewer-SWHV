package org.helioviewer.jhv.timelines.band;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.Interval;

import com.google.common.collect.ArrayListMultimap;

final class BandDownloads {

    private static final ArrayListMultimap<Band, Future<BandData>> workers = ArrayListMultimap.create();

    private static void pruneFinished(Band band) {
        workers.get(band).removeIf(Future::isDone);
    }

    static boolean isAvailable(Band band) {
        String baseUrl = band.getBandType().getBaseUrl();
        return !baseUrl.isEmpty() && BandReaderHapi.hasCatalog(baseUrl);
    }

    static void start(Band band, List<Interval> intervals) {
        String baseUrl = band.getBandType().getBaseUrl();
        pruneFinished(band);
        for (Interval interval : intervals) {
            Future<BandData> download = Task.submit(baseUrl,
                    BandReaderHapi.dataRequest(baseUrl, interval.start(), interval.end()),
                    data -> acceptData(band, data),
                    (logContext, t) -> downloadFailed(band, interval, t));
            workers.put(band, download);
        }
        band.downloadStateChanged();
    }

    static void stop(Band band) {
        workers.get(band).forEach(worker -> worker.cancel(true));
        workers.removeAll(band);
    }

    static boolean isActive(Band band) {
        pruneFinished(band);
        return !workers.get(band).isEmpty();
    }

    private static void acceptData(Band band, BandData data) {
        band.downloadSucceeded(data);
    }

    private static void downloadFailed(Band band, Interval interval, Throwable t) {
        if (!(t instanceof CancellationException))
            Log.error(t);
        band.requestFailed(interval);
    }

}
