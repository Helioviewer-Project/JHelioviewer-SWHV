package org.helioviewer.jhv.timelines.band;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.threads.JHVExecutor;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.json.JSONObject;

public class BandDataProvider {

    private static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 21;
    private static final ExecutorService executorService = JHVExecutor.createJHVWorkersExecutorService("EVE", 12);

    private static final HashMap<Band, List<BandDownloadTask>> downloadMap = new HashMap<>();

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

            ArrayList<Interval> intervals = new ArrayList<>();
            band.addRequest(start, end).forEach(interval -> intervals.addAll(Interval.splitInterval(interval, DOWNLOADER_MAX_DAYS_PER_BLOCK)));
            if (!intervals.isEmpty())
                addDownloads(band, intervals);
        }
    }

    private static void addDownloads(Band band, List<Interval> intervals) {
        List<BandDownloadTask> workerList = downloadMap.computeIfAbsent(band, k -> new ArrayList<>(intervals.size()));
        for (Interval interval : intervals) {
            BandDownloadTask worker = new BandDownloadTask(band, interval.start, interval.end);
            executorService.submit(worker);
            workerList.add(worker);
        }
    }

    static void stopDownloads(Band band) {
        downloadMap.get(band).forEach(worker -> worker.cancel(true));
        downloadMap.remove(band);
    }

    static boolean isDownloadActive(Band band) {
        for (BandDownloadTask worker : downloadMap.get(band)) {
            if (!worker.isDone())
                return true;
        }
        return false;
    }

    private static class BandDownloadTask extends JHVWorker<BandResponse, Void> {

        private final Band band;
        private final long startTime;
        private final long endTime;

        BandDownloadTask(Band _band, long _startTime, long _endTime) {
            band = _band;
            startTime = _startTime;
            endTime = _endTime;
            Timelines.getLayers().downloadStarted(band);
        }

        @Nullable
        @Override
        protected BandResponse backgroundWork() {
            try {
                BandType type = band.getBandType();
                String request = type.getBaseURL() + "start_date=" + TimeUtils.formatDate(startTime) + "&end_date=" + TimeUtils.formatDate(endTime) +
                        "&timeline=" + type.getName();
                return new BandResponse(JSONUtils.get(request));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                try {
                    BandResponse r = get();
                    if (r != null) {
                        if (!r.bandName.equals(band.getBandType().getName()))
                            throw new Exception("Expected " + band.getBandType().getName() + ", got " + r.bandName);
                        band.addToCache(r.values, r.dates);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Timelines.getLayers().downloadFinished(band);
        }

    }

}
