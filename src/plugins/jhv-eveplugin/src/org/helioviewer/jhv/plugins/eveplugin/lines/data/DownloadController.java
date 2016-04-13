package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
import org.helioviewer.jhv.plugins.eveplugin.lines.model.EVEDrawController;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandType;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVEAPI;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.threads.JHVThread;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class DownloadController {

    private static final DownloadController singletonInstance = new DownloadController();

    private final HashMap<Band, ArrayList<Interval>> downloadMap = new HashMap<Band, ArrayList<Interval>>();
    private final HashMap<Band, List<Future<?>>> futureJobs = new HashMap<Band, List<Future<?>>>();

    private final LineDataSelectorModel selectorModel;
    public static final ExecutorService downloadPool = new ThreadPoolExecutor(5, 5, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new JHVThread.NamedThreadFactory("EVE Download"), new ThreadPoolExecutor.AbortPolicy());

    private DownloadController() {
        selectorModel = LineDataSelectorModel.getSingletonInstance();
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
        GregorianCalendar cs = new GregorianCalendar();
        cs.setTime(queryInterval.start);
        cs.add(Calendar.DAY_OF_MONTH, -7);
        GregorianCalendar ce = new GregorianCalendar();
        ce.setTime(queryInterval.end);
        ce.add(Calendar.DAY_OF_MONTH, +7);
        return new Interval(cs.getTime(), ce.getTime());
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
                if (interval.start != null && interval.end != null) {
                    requestData();
                }
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
            final SimpleDateFormat eveAPIDateFormat = new SimpleDateFormat(EVEAPI.API_DATE_FORMAT);

            return new URL(type.getBaseUrl() + EVEAPI.API_URL_PARAMETER_STARTDATE + eveAPIDateFormat.format(interval.start) + "&" + EVEAPI.API_URL_PARAMETER_ENDDATE + eveAPIDateFormat.format(interval.end) + "&" + EVEAPI.API_URL_PARAMETER_TYPE + type.getName() + "&" + EVEAPI.API_URL_PARAMETER_FORMAT + EVEAPI.API_URL_PARAMETER_FORMAT_VALUES.JSON);
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

}
