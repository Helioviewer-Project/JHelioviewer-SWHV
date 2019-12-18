package org.helioviewer.jhv.timelines.band;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.threads.JHVExecutor;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONObject;

public class BandDataProvider {

    private static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 21;
    private static final ExecutorService executorService = JHVExecutor.createJHVWorkersExecutorService("EVE", 12);

    private static final HashMap<Band, List<BandDownloadTask>> downloadMap = new HashMap<>();
    private static final HashMap<Band, List<Future<?>>> futureJobs = new HashMap<>();

    public static void loadBandTypes() {
        executorService.execute(new BandTypeTask());
    }

    public static void loadBand(JSONObject jo) {
        executorService.execute(new BandLoadTask(jo));
    }

    static void updateBand(Band band, long start, long end) {
        List<Interval> missingIntervalsNoExtend = band.getMissingDaysInInterval(start, end);
        if (!missingIntervalsNoExtend.isEmpty()) {
            // extend
            start -= 7 * TimeUtils.DAY_IN_MILLIS;
            end += 7 * TimeUtils.DAY_IN_MILLIS;

            ArrayList<Interval> intervals = getIntervals(band, start, end);
            if (!intervals.isEmpty())
                addDownloads(band, intervals);
        }
    }

    private static ArrayList<Interval> getIntervals(Band band, long start, long end) {
        ArrayList<Interval> intervals = new ArrayList<>();
        band.addRequest(start, end).forEach(interval -> intervals.addAll(Interval.splitInterval(interval, DOWNLOADER_MAX_DAYS_PER_BLOCK)));
        return intervals;
    }

    private static void addDownloads(Band band, List<Interval> intervals) {
        int size = intervals.size();
        List<BandDownloadTask> dl = downloadMap.computeIfAbsent(band, k -> new ArrayList<>(size));
        List<Future<?>> fl = futureJobs.computeIfAbsent(band, k -> new ArrayList<>(size));
        for (Interval interval : intervals) {
            BandDownloadTask worker = new BandDownloadTask(band, interval.start, interval.end);
            dl.add(worker);
            fl.add(executorService.submit(worker));
        }
    }

    static void downloadFinished(BandDownloadTask worker) {
        Band band = worker.band;
        List<BandDownloadTask> list = downloadMap.get(band);
        if (list != null) {
            list.remove(worker);
            if (list.isEmpty())
                downloadMap.remove(band);
        }
        Timelines.getLayers().downloadFinished(band);
    }

    static void stopDownloads(Band band) {
        List<BandDownloadTask> list = downloadMap.get(band);
        if (list == null)
            return;
        if (list.isEmpty())
            downloadMap.remove(band);

        futureJobs.get(band).forEach(job -> job.cancel(true));
        futureJobs.remove(band);
        Timelines.getLayers().downloadFinished(band);
    }

    static boolean isDownloadActive(Band band) {
        List<BandDownloadTask> list = downloadMap.get(band);
        return list != null && !list.isEmpty();
    }

}
