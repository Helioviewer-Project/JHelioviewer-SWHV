package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.interval.Interval;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandType;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVEAPI;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Stephan Pagel
 * */
public class DownloadController {

    private static final DownloadController singletonInstance = new DownloadController();

    private final HashMap<Band, LinkedList<Interval<Date>>> downloadMap = new HashMap<Band, LinkedList<Interval<Date>>>();
    private final HashMap<Band, List<Future<?>>> futureJobs = new HashMap<Band, List<Future<?>>>();

    private final LineDataSelectorModel selectorModel;
    private final static ExecutorService downloadPool = Executors.newFixedThreadPool(5);

    private DownloadController() {
        selectorModel = LineDataSelectorModel.getSingletonInstance();
    }

    public static final DownloadController getSingletonInstance() {
        return singletonInstance;
    }

    public void updateBands(final BandType[] bandTypes, final Interval<Date> interval, final Interval<Date> priorityInterval) {
        for (int i = 0; i < bandTypes.length; ++i) {
            updateBand(new Band(bandTypes[i]), interval, priorityInterval);
        }
    }

    public void updateBands(final Band[] bands, final Interval<Date> interval, final Interval<Date> priorityInterval) {
        for (int i = 0; i < bands.length; i++) {
            updateBand(bands[i], interval, priorityInterval);
        }
    }

    public void updateBand(final BandType bandType, final Interval<Date> interval, final Interval<Date> priorityInterval) {
        updateBand(new Band(bandType), interval, priorityInterval);
    }

    public void updateBand(final Band band, final Interval<Date> queryInterval, final Interval<Date> priorityInterval) {
        if (band == null || queryInterval == null || queryInterval.getStart() == null || queryInterval.getEnd() == null) {
            return;
        }

        List<Interval<Date>> missingIntervalsNoExtend = EVECacheController.getSingletonInstance().getMissingDaysInInterval(band, queryInterval);
        if (!missingIntervalsNoExtend.isEmpty()) {
            Interval<Date> realQueryInterval = extendQueryInterval(queryInterval);

            // get all intervals within query interval where data is missing
            LinkedList<Interval<Date>> intervals = getIntervals(band, realQueryInterval);

            if (intervals == null) {
                // there is no interval where data is missing
                return;
            }

            if (intervals.size() == 0) {
                fireDownloadStarted(band, queryInterval);
                return;
            }

            // create download jobs and allocate priorities
            final DownloadThread[] jobs = new DownloadThread[intervals.size()];

            int i = 0;
            for (final Interval<Date> interval : intervals) {
                jobs[i] = new DownloadThread(band, interval);
                ++i;
            }

            // add download jobs
            addFutureJobs(addDownloads(jobs), band);

            // inform listeners
            fireDownloadStarted(band, queryInterval);
        }
    }

    private void addFutureJobs(List<Future<?>> newFutureJobs, Band band) {
        List<Future<?>> fj = new LinkedList<Future<?>>();
        if (futureJobs.containsKey(band)) {
            fj = futureJobs.get(band);
        }
        fj.addAll(newFutureJobs);
        futureJobs.put(band, fj);
    }

    private Interval<Date> extendQueryInterval(Interval<Date> queryInterval) {
        GregorianCalendar cs = new GregorianCalendar();
        cs.setTime(queryInterval.getStart());
        cs.add(Calendar.DAY_OF_MONTH, -7);
        GregorianCalendar ce = new GregorianCalendar();
        ce.setTime(queryInterval.getEnd());
        ce.add(Calendar.DAY_OF_MONTH, +7);
        return new Interval<Date>(cs.getTime(), ce.getTime());
    }

    private LinkedList<Interval<Date>> getIntervals(final Band band, final Interval<Date> queryInterval) {
        // get missing data intervals within given interval
        final List<Interval<Date>> missingIntervals = EVECacheController.getSingletonInstance().addRequest(band, queryInterval);
        if (missingIntervals.size() == 0) {
            return null;
        }

        // split intervals (if necessary) into smaller intervals
        final LinkedList<Interval<Date>> intervals = new LinkedList<Interval<Date>>();
        for (final Interval<Date> i : missingIntervals) {
            intervals.addAll(splitInterval(i));
        }

        return intervals;
    }

    public void stopDownloads(final Band band) {
        final LinkedList<Interval<Date>> list = downloadMap.get(band);
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
        fireDownloadFinished(band, null, 0);
    }

    public boolean isDownloadActive(final Band band) {
        final LinkedList<Interval<Date>> list = downloadMap.get(band);
        if (list == null) {
            return false;
        }
        return list.size() > 0;
    }

    private void fireDownloadStarted(final Band band, final Interval<Date> interval) {
        selectorModel.downloadStarted(band);
    }

    private void fireDownloadFinished(final Band band, final Interval<Date> interval, final int activeBandDownloads) {
        selectorModel.downloadFinished(band);
    }

    private LinkedList<Interval<Date>> splitInterval(final Interval<Date> interval) {
        final LinkedList<Interval<Date>> intervals = new LinkedList<Interval<Date>>();

        if (interval.getStart() == null || interval.getEnd() == null) {
            intervals.add(interval);
            return intervals;
        }

        final Calendar calendar = new GregorianCalendar();
        Date startDate = interval.getStart();

        while (true) {
            calendar.clear();
            calendar.setTime(startDate);
            calendar.add(Calendar.DAY_OF_MONTH, EVESettings.DOWNLOADER_MAX_DAYS_PER_BLOCK);

            final Date newStartDate = calendar.getTime();

            if (interval.containsPointInclusive(newStartDate)) {
                calendar.add(Calendar.SECOND, -1);
                intervals.add(new Interval<Date>(startDate, calendar.getTime()));
                startDate = newStartDate;
            } else {
                intervals.add(new Interval<Date>(startDate, interval.getEnd()));
                break;
            }
        }

        return intervals;
    }

    private List<Future<?>> addDownloads(final DownloadThread[] jobs) {
        List<Future<?>> futureJobs = new LinkedList<Future<?>>();
        for (int i = 0; i < jobs.length; ++i) {
            // add to download map
            final Band band = jobs[i].getBand();
            final Interval<Date> interval = jobs[i].getInterval();

            LinkedList<Interval<Date>> list = downloadMap.get(band);
            if (list == null) {
                list = new LinkedList<Interval<Date>>();
            }
            list.add(interval);

            downloadMap.put(band, list);
            futureJobs.add(downloadPool.submit(jobs[i]));
        }
        return futureJobs;
    }

    private void downloadFinished(final Band band, final Interval<Date> interval) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                int numberOfDownloads = 0;

                final LinkedList<Interval<Date>> list = downloadMap.get(band);
                if (list != null) {
                    list.remove(interval);
                    numberOfDownloads = list.size();

                    if (numberOfDownloads == 0) {
                        downloadMap.remove(band);
                    }
                }
                fireDownloadFinished(band, interval, numberOfDownloads);
            }
        });
    }

    private class DownloadThread implements Runnable {

        private final Interval<Date> interval;
        private final Band band;

        public DownloadThread(final Band band, final Interval<Date> interval) {
            this.interval = interval;
            this.band = band;
        }

        public Interval<Date> getInterval() {
            return interval;
        }

        public Band getBand() {
            return band;
        }

        @Override
        public void run() {
            try {
                if (interval != null && interval.getStart() != null && interval.getEnd() != null && band != null) {
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
                final StringBuilder sb = new StringBuilder();
                String str;

                while ((str = in.readLine()) != null) {
                    sb.append(str);
                }
                in.close();

                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            addDataToCache(new JSONObject(sb.toString()), band);
                        } catch (JSONException e) {
                            Log.error("Error Parsing the EVE Response.", e);
                        }
                    }
                });
            } catch (final IOException e1) {
                Log.error("Error Parsing the EVE Response.", e1);
            }
        }

        private URL buildRequestURL(final Interval<Date> interval, final BandType type) throws MalformedURLException {
            final SimpleDateFormat eveAPIDateFormat = new SimpleDateFormat(EVEAPI.API_DATE_FORMAT);

            return new URL(type.getBaseUrl() + EVEAPI.API_URL_PARAMETER_STARTDATE + eveAPIDateFormat.format(interval.getStart()) + "&" + EVEAPI.API_URL_PARAMETER_ENDDATE + eveAPIDateFormat.format(interval.getEnd()) + "&" + EVEAPI.API_URL_PARAMETER_TYPE + type.getName() + "&" + EVEAPI.API_URL_PARAMETER_FORMAT + EVEAPI.API_URL_PARAMETER_FORMAT_VALUES.JSON);
        }

        private void addDataToCache(final JSONObject json, final Band band) {
            try {
                double multiplier = 1.0;
                if (json.has("multiplier")) {
                    multiplier = json.getDouble("multiplier");
                }
                final JSONArray data = json.getJSONArray("data");

                int length = data.length();
                if (length == 0) {
                    return;
                }

                // Log.warn(data.toString());
                final double[] values = new double[length];
                final long[] dates = new long[length];

                for (int i = 0; i < length; i++) {
                    final JSONArray entry = data.getJSONArray(i);
                    final long millis = ((long) entry.getDouble(0)) * 1000;
                    values[i] = entry.getDouble(1) * multiplier;
                    dates[i] = millis;
                }

                EVECacheController.getSingletonInstance().addToCache(band, values, dates);
            } catch (JSONException e) {
                Log.error("", e);
            }
        }
    }

}
