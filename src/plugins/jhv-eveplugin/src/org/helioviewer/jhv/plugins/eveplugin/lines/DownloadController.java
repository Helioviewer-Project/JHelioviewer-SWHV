package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DownloadController {

    private static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 21;

    private static final DownloadController singletonInstance = new DownloadController();

    private static final HashMap<Band, ArrayList<Interval>> downloadMap = new HashMap<Band, ArrayList<Interval>>();
    private static final HashMap<Band, List<Future<?>>> futureJobs = new HashMap<Band, List<Future<?>>>();

    public static DownloadController getSingletonInstance() {
        return singletonInstance;
    }

    public void updateBand(Band band, long start, long end) {
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

    private void addFutureJobs(List<Future<?>> newFutureJobs, Band band) {
        List<Future<?>> fj = new ArrayList<Future<?>>();
        if (futureJobs.containsKey(band)) {
            fj = futureJobs.get(band);
        }
        fj.addAll(newFutureJobs);
        futureJobs.put(band, fj);
    }

    private ArrayList<Interval> getIntervals(Band band, long start, long end) {
        List<Interval> missingIntervals = band.addRequest(start, end);
        if (missingIntervals.isEmpty()) {
            return null;
        }

        ArrayList<Interval> intervals = new ArrayList<Interval>();
        for (Interval i : missingIntervals) {
            intervals.addAll(Interval.splitInterval(i, DOWNLOADER_MAX_DAYS_PER_BLOCK));
        }

        return intervals;
    }

    public void stopDownloads(Band band) {
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

    public boolean isDownloadActive(Band band) {
        ArrayList<Interval> list = downloadMap.get(band);
        return list != null && !list.isEmpty();
    }

    private void fireDownloadStarted(Band band) {
        EVEPlugin.ldsm.downloadStarted(band);
    }

    private void fireDownloadFinished(Band band) {
        EVEPlugin.ldsm.downloadFinished(band);
    }

    private List<Future<?>> addDownloads(DownloadThread[] jobs) {
        List<Future<?>> futureJobs = new ArrayList<Future<?>>();
        for (DownloadThread job : jobs) {
            Band band = job.getBand();
            Interval interval = job.getInterval();

            ArrayList<Interval> list = downloadMap.get(band);
            if (list == null) {
                list = new ArrayList<Interval>();
            }
            list.add(interval);

            downloadMap.put(band, list);
            futureJobs.add(EVEPlugin.executorService.submit(job));
        }
        return futureJobs;
    }

    private void downloadFinished(Band band, Interval interval) {
        ArrayList<Interval> list = downloadMap.get(band);
        if (list != null) {
            list.remove(interval);
            if (list.isEmpty()) {
                downloadMap.remove(band);
            }
        }
        fireDownloadFinished(band);
    }

    private class DownloadThread extends JHVWorker<Pair<float[], long[]>, Void> {

        private final Interval interval;
        private final Band band;

        public DownloadThread(Band band, Interval interval) {
            this.interval = interval;
            this.band = band;
        }

        public Interval getInterval() {
            return interval;
        }

        public Band getBand() {
            return band;
        }

        @Override
        protected Pair<float[], long[]> backgroundWork() {
            URL url;
            try {
                url = buildRequestURL(interval, band.getBandType());
            } catch (MalformedURLException e) {
                Log.error("Error creating EVE URL: ", e);
                return null;
            }

            try {
                JSONObject json = JSONUtils.getJSONStream(new DownloadStream(url).getInput());

                double multiplier = 1.0;
                if (json.has("multiplier")) {
                    multiplier = json.getDouble("multiplier");
                }

                JSONArray data = json.getJSONArray("data");
                int length = data.length();
                if (length == 0) {
                    return null;
                }

                float[] values = new float[length];
                long[] dates = new long[length];
                for (int i = 0; i < length; i++) {
                    JSONArray entry = data.getJSONArray(i);
                    dates[i] = entry.getLong(0) * 1000;
                    values[i] = (float) (entry.getDouble(1) * multiplier);
                }

                return new Pair<float[], long[]>(values, dates);
            } catch (JSONException | IOException e) {
                Log.error("Error Parsing the EVE Response ", e);
            }
            return null;
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                try {
                    Pair<float[], long[]> p = get();
                    if (p != null)
                        band.addToCache(p.a, p.b);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            downloadFinished(band, interval);
        }

        private URL buildRequestURL(Interval interval, BandType type) throws MalformedURLException {
            String urlf = type.getBaseURL() + "start_date=%s&end_date=%s&timeline=%s&data_format=json";
            String url = String.format(urlf, TimeUtils.dateFormat.format(interval.start), TimeUtils.dateFormat.format(interval.end), type.getName());
            return new URL(url);
        }

    }

}
