package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.lines.model.EVEDrawController;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandType;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.threads.JHVThread;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class DownloadController implements TimingListener {

    private static final DownloadController singletonInstance = new DownloadController();

    private final HashMap<Band, ArrayList<Interval>> downloadMap = new HashMap<Band, ArrayList<Interval>>();
    private final HashMap<Band, List<Future<?>>> futureJobs = new HashMap<Band, List<Future<?>>>();

    private final LineDataSelectorModel selectorModel;
    public static final ExecutorService downloadPool = new ThreadPoolExecutor(5, 5, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new JHVThread.NamedThreadFactory("EVE Download"), new ThreadPoolExecutor.AbortPolicy());

    private final DrawController drawController = DrawController.getSingletonInstance();

    private DownloadController() {
        selectorModel = LineDataSelectorModel.getSingletonInstance();
        drawController.addTimingListener(this);
    }

    public static final DownloadController getSingletonInstance() {
        return singletonInstance;
    }

    public void updateBands(final Interval interval, final Interval priorityInterval) {
        Set<Band> bands = EVEDrawController.getSingletonInstance().getAllBands();
        for (Band b : bands) {
            updateBand(b, interval, priorityInterval);
        }
    }

    public void updateBand(final Band band, final Interval queryInterval, final Interval priorityInterval) {
        if (band == null || queryInterval == null) {
            return;
        }

        List<Interval> missingIntervalsNoExtend = EVECacheController.getSingletonInstance().getMissingDaysInInterval(band, queryInterval);
        if (!missingIntervalsNoExtend.isEmpty()) {
            Interval realQueryInterval = extendQueryInterval(queryInterval);

            // get all intervals within query interval where data is missing
            ArrayList<Interval> intervals = getIntervals(band, realQueryInterval);

            if (intervals == null) {
                // there is no interval where data is missing
                return;
            }

            if (intervals.size() == 0) {
                fireDownloadStarted(band);
                return;
            }

            // create download jobs and allocate priorities
            final DownloadThread[] jobs = new DownloadThread[intervals.size()];

            int i = 0;
            for (final Interval interval : intervals) {
                jobs[i] = new DownloadThread(band, interval);
                ++i;
            }

            // add download jobs
            addFutureJobs(addDownloads(jobs), band);

            // inform listeners
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

    private Interval extendQueryInterval(Interval queryInterval) {
        return new Interval(queryInterval.start - 7 * TimeUtils.DAY_IN_MILLIS, queryInterval.end + 7 * TimeUtils.DAY_IN_MILLIS);
    }

    private ArrayList<Interval> getIntervals(final Band band, final Interval queryInterval) {
        // get missing data intervals within given interval
        final List<Interval> missingIntervals = EVECacheController.getSingletonInstance().addRequest(band, queryInterval);
        if (missingIntervals.isEmpty()) {
            return null;
        }

        // split intervals (if necessary) into smaller intervals
        final ArrayList<Interval> intervals = new ArrayList<Interval>();
        for (final Interval i : missingIntervals) {
            intervals.addAll(Interval.splitInterval(i, EVESettings.DOWNLOADER_MAX_DAYS_PER_BLOCK));
        }

        return intervals;
    }

    public void stopDownloads(final Band band) {
        final ArrayList<Interval> list = downloadMap.get(band);
        if (list == null) {
            return;
        }
        if (list.size() == 0) {
            downloadMap.remove(band);
        }
        final List<Future<?>> fjs = futureJobs.get(band);
        for (Future<?> fj : fjs) {
            fj.cancel(true);
        }
        futureJobs.remove(band);
        fireDownloadFinished(band);
    }

    public boolean isDownloadActive(final Band band) {
        final ArrayList<Interval> list = downloadMap.get(band);
        if (list == null) {
            return false;
        }
        return list.size() > 0;
    }

    private void fireDownloadStarted(final Band band) {
        selectorModel.downloadStarted(band);
    }

    private void fireDownloadFinished(final Band band) {
        selectorModel.downloadFinished(band);
    }

    private List<Future<?>> addDownloads(final DownloadThread[] jobs) {
        List<Future<?>> futureJobs = new ArrayList<Future<?>>();
        for (int i = 0; i < jobs.length; ++i) {
            // add to download map
            final Band band = jobs[i].getBand();
            final Interval interval = jobs[i].getInterval();

            ArrayList<Interval> list = downloadMap.get(band);
            if (list == null) {
                list = new ArrayList<Interval>();
            }
            list.add(interval);

            downloadMap.put(band, list);
            futureJobs.add(downloadPool.submit(jobs[i]));
        }
        return futureJobs;
    }

    private void downloadFinished(final Band band, final Interval interval) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                int numberOfDownloads = 0;

                final ArrayList<Interval> list = downloadMap.get(band);
                if (list != null) {
                    list.remove(interval);
                    numberOfDownloads = list.size();

                    if (numberOfDownloads == 0) {
                        downloadMap.remove(band);
                    }
                }
                fireDownloadFinished(band);
            }
        });
    }

    private class DownloadThread implements Runnable {

        private final Interval interval;
        private final Band band;

        public DownloadThread(final Band band, final Interval interval) {
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
        public void run() {
            try {
                requestData();
            } finally {
                downloadFinished(band, interval);
            }
        }

        private void requestData() {
            URL url = null;

            try {
                url = buildRequestURL(interval, band.getBandType());
            } catch (final MalformedURLException e) {
                Log.error("Error Creating the EVE URL.", e);
            }

            if (url == null) {
                return;
            }

            // Log.debug("Requesting EVE Data: " + url);

            // this might take a while
            try {
                DownloadStream ds = new DownloadStream(url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());
                BufferedReader in = new BufferedReader(new InputStreamReader(ds.getInput(), "UTF-8"));
                JSONObject json = new JSONObject(new JSONTokener(in));

                double multiplier = 1.0;
                if (json.has("multiplier")) {
                    multiplier = json.getDouble("multiplier");
                }

                JSONArray data = json.getJSONArray("data");
                int length = data.length();
                if (length == 0) {
                    return;
                }

                // Log.warn(data.toString());
                float[] values = new float[length];
                long[] dates = new long[length];

                for (int i = 0; i < length; i++) {
                    JSONArray entry = data.getJSONArray(i);
                    dates[i] = entry.getLong(0) * 1000;
                    values[i] = (float) (entry.getDouble(1) * multiplier);
                }

                addDataToCache(band, values, dates);
            } catch (JSONException e) {
                Log.error("Error Parsing the EVE Response ", e);
            } catch (IOException e) {
                Log.error("Error Parsing the EVE Response ", e);
            }
        }

        private URL buildRequestURL(final Interval interval, final BandType type) throws MalformedURLException {
            String urlf = type.getBaseUrl() + "start_date=%s&end_date=%s&timeline=%s&data_format=json";
            String url = String.format(urlf, TimeUtils.dateFormat.format(interval.start), TimeUtils.dateFormat.format(interval.end), type.getName());
            return new URL(url);
        }

        private void addDataToCache(final Band band, final float[] values, final long[] dates) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    EVECacheController.getSingletonInstance().addToCache(band, values, dates);
                }
            });
        }
    }

    @Override
    public void availableIntervalChanged() {
        Interval availableInterval = drawController.getAvailableInterval();
        final Interval downloadInterval = new Interval(availableInterval.start, availableInterval.end - TimeUtils.DAY_IN_MILLIS);

        DownloadController.getSingletonInstance().updateBands(downloadInterval, drawController.getSelectedInterval());

    }

    @Override
    public void selectedIntervalChanged(boolean keepFullValueRange) {
        // TODO Auto-generated method stub

    }

}
