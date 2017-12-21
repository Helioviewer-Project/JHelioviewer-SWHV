package org.helioviewer.jhv.plugins.eve.lines;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandDataProvider;

public class EVEDataProvider implements BandDataProvider {

    private static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 21;

    private static final HashMap<Band, List<Interval>> downloadMap = new HashMap<>();
    private static final HashMap<Band, List<Future<?>>> futureJobs = new HashMap<>();

    @Override
    public void loadBand(URI uri) {
        EVEPlugin.executorService.submit(new LoadThread(uri));
    }

    @Override
    public void updateBand(Band band, long start, long end) {
        List<Interval> missingIntervalsNoExtend = band.getMissingDaysInInterval(start, end);
        if (!missingIntervalsNoExtend.isEmpty()) {
            // extend
            start -= 7 * TimeUtils.DAY_IN_MILLIS;
            end += 7 * TimeUtils.DAY_IN_MILLIS;

            ArrayList<Interval> intervals = getIntervals(band, start, end);
            if (intervals.size() == 0)
                return;
            addDownloads(band, intervals);
        }
    }

    private static ArrayList<Interval> getIntervals(Band band, long start, long end) {
        List<Interval> missingIntervals = band.addRequest(start, end);
        ArrayList<Interval> intervals = new ArrayList<>();
        for (Interval i : missingIntervals) {
            intervals.addAll(Interval.splitInterval(i, DOWNLOADER_MAX_DAYS_PER_BLOCK));
        }
        return intervals;
    }

    private static void addDownloads(Band band, List<Interval> intervals) {
        int size = intervals.size();
        List<Interval> dl = downloadMap.computeIfAbsent(band, k -> new ArrayList<>(size));
        List<Future<?>> fl = futureJobs.computeIfAbsent(band, k -> new ArrayList<>(size));
        for (Interval interval : intervals) {
            dl.add(interval);
            fl.add(EVEPlugin.executorService.submit(new DownloadThread(band, interval)));
        }
    }

    static void downloadFinished(Band band, Interval interval) {
        List<Interval> list = downloadMap.get(band);
        if (list != null) {
            list.remove(interval);
            if (list.isEmpty())
                downloadMap.remove(band);
        }
        Timelines.getLayers().downloadFinished(band);
    }

    @Override
    public void stopDownloads(Band band) {
        List<Interval> list = downloadMap.get(band);
        if (list == null)
            return;
        if (list.isEmpty())
            downloadMap.remove(band);

        List<Future<?>> fjs = futureJobs.get(band);
        for (Future<?> fj : fjs) {
            fj.cancel(true);
        }
        futureJobs.remove(band);
        Timelines.getLayers().downloadFinished(band);
    }

    @Override
    public boolean isDownloadActive(Band band) {
        List<Interval> list = downloadMap.get(band);
        return list != null && !list.isEmpty();
    }

}
