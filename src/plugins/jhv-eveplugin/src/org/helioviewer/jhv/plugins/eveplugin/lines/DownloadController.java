package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineTableModel;

public class DownloadController {

    private static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 21;

    private static final HashMap<Band, ArrayList<Interval>> downloadMap = new HashMap<>();
    private static final HashMap<Band, List<Future<?>>> futureJobs = new HashMap<>();

    public static void updateBand(Band band, long start, long end) {
        List<Interval> missingIntervalsNoExtend = band.getMissingDaysInInterval(start, end);
        if (!missingIntervalsNoExtend.isEmpty()) {
            // extend
            start -= 7 * TimeUtils.DAY_IN_MILLIS;
            end += 7 * TimeUtils.DAY_IN_MILLIS;

            ArrayList<Interval> intervals = getIntervals(band, start, end);
            if (intervals == null) {
                return;
            }

            int n = intervals.size();
            if (n == 0) {
                fireDownloadStarted(band);
                return;
            }

            DownloadThread[] jobs = new DownloadThread[n];
            int i = 0;
            for (Interval interval : intervals) {
                jobs[i] = new DownloadThread(band, interval);
                ++i;
            }
            addFutureJobs(addDownloads(jobs), band);
            fireDownloadStarted(band);
        }
    }

    private static void addFutureJobs(List<Future<?>> newFutureJobs, Band band) {
        List<Future<?>> fj = new ArrayList<>();
        if (futureJobs.containsKey(band)) {
            fj = futureJobs.get(band);
        }
        fj.addAll(newFutureJobs);
        futureJobs.put(band, fj);
    }

    private static ArrayList<Interval> getIntervals(Band band, long start, long end) {
        List<Interval> missingIntervals = band.addRequest(start, end);
        if (missingIntervals.isEmpty()) {
            return null;
        }

        ArrayList<Interval> intervals = new ArrayList<>();
        for (Interval i : missingIntervals) {
            intervals.addAll(Interval.splitInterval(i, DOWNLOADER_MAX_DAYS_PER_BLOCK));
        }

        return intervals;
    }

    public static void stopDownloads(Band band) {
        ArrayList<Interval> list = downloadMap.get(band);
        if (list == null) {
            return;
        }
        if (list.isEmpty()) {
            downloadMap.remove(band);
        }
        List<Future<?>> fjs = futureJobs.get(band);
        for (Future<?> fj : fjs) {
            fj.cancel(true);
        }
        futureJobs.remove(band);
        fireDownloadFinished(band);
    }

    public static boolean isDownloadActive(Band band) {
        ArrayList<Interval> list = downloadMap.get(band);
        return list != null && !list.isEmpty();
    }

    private static void fireDownloadStarted(Band band) {
        TimelineTableModel.downloadStarted(band);
    }

    private static void fireDownloadFinished(Band band) {
        TimelineTableModel.downloadFinished(band);
    }

    private static List<Future<?>> addDownloads(DownloadThread[] jobs) {
        List<Future<?>> futureJobs = new ArrayList<>();
        for (DownloadThread job : jobs) {
            Band band = job.getBand();
            Interval interval = job.getInterval();

            ArrayList<Interval> list = downloadMap.get(band);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(interval);

            downloadMap.put(band, list);
            futureJobs.add(EVEPlugin.executorService.submit(job));
        }
        return futureJobs;
    }

    static void downloadFinished(Band band, Interval interval) {
        ArrayList<Interval> list = downloadMap.get(band);
        if (list != null) {
            list.remove(interval);
            if (list.isEmpty()) {
                downloadMap.remove(band);
            }
        }
        fireDownloadFinished(band);
    }

}
