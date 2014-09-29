package org.helioviewer.plugins.eveplugin.lines.data;

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
import java.util.concurrent.Semaphore;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.plugins.eveplugin.settings.BandType;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Stephan Pagel
 * */
public class DownloadController {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private enum DownloadPriority {
        LOW, HIGH
    }

    private static final DownloadController singletonInstance = new DownloadController();

    private final LinkedList<DownloadControllerListener> listeners = new LinkedList<DownloadControllerListener>();

    private final LinkedList<DownloadJob> highPriorityDownloadQueue = new LinkedList<DownloadJob>();
    private final LinkedList<DownloadJob> lowPriorityDownloadQueue = new LinkedList<DownloadJob>();
    private final HashMap<Band, LinkedList<Interval<Date>>> downloadMap = new HashMap<Band, LinkedList<Interval<Date>>>();

    private final Object lock = new Object();

    private final DownloadManager downloadManager = new DownloadManager();

    private final LineDataSelectorModel selectorModel;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    private DownloadController() {
        selectorModel = LineDataSelectorModel.getSingletonInstance();
        new Thread(downloadManager, "EVEDownloadManager").start();

    }

    public static final DownloadController getSingletonInstance() {
        return singletonInstance;
    }

    public void addListener(final DownloadControllerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(final DownloadControllerListener listener) {
        listeners.remove(listener);
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

        // get all intervals within query interval where data is missing
        LinkedList<Interval<Date>> intervals = getIntervals(band, queryInterval);

        if (intervals == null) {
            // there is no interval where data is missing
            return;
        }

        // try to find data in local database. If data is available in local
        // database it will be transfered to cache
        updateBandFromDatabase(band, intervals);

        // get all intervals within query interval where data is missing again
        intervals = getIntervals(band, queryInterval);

        if (intervals == null) {
            // there is no interval where data is missing
            return;
        }

        // check if intervals are in download queues already
        synchronized (lock) {
            final LinkedList<Interval<Date>> list = downloadMap.get(band);

            if (list != null) {
                for (int i = intervals.size() - 1; i >= 0; --i) {
                    if (list.contains(intervals.get(i))) {
                        intervals.remove(i);
                    }
                }
            }
        }

        if (intervals.size() == 0) {
            fireDownloadStarted(band, queryInterval);
            return;
        }

        // create download jobs and allocate priorities
        final DownloadJob[] jobs = new DownloadJob[intervals.size()];
        final DownloadPriority[] priorities = new DownloadPriority[intervals.size()];

        int i = 0;
        for (final Interval<Date> interval : intervals) {
            jobs[i] = new DownloadJob(band, interval);
            priorities[i] = getPriority(interval, priorityInterval);
            ++i;
        }

        // add download jobs
        addDownloads(jobs, priorities);

        // inform listeners
        fireDownloadStarted(band, queryInterval);
    }

    private LinkedList<Interval<Date>> getIntervals(final Band band, final Interval<Date> queryInterval) {
        // get missing data intervals within given interval
        final List<Interval<Date>> missingIntervals = EVECacheController.getSingletonInstance().getMissingDaysInInterval(band, queryInterval);

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

    private void updateBandFromDatabase(final Band band, final LinkedList<Interval<Date>> intervals) {
        final Calendar calendar = new GregorianCalendar();

        for (final Interval<Date> interval : intervals) {
            // one day has to be added otherwise the last day of the given
            // interval will be ignored
            calendar.clear();
            calendar.setTime(interval.getEnd());
            calendar.add(Calendar.DAY_OF_YEAR, 1);

            final EVEValue[] values = DatabaseController.getSingletonInstance().getDataInInterval(band, new Interval<Date>(interval.getStart(), calendar.getTime()));
            EVECacheController.getSingletonInstance().addToCache(values, band);
        }
    }

    public void stopAllDownloads() {
        Band[] bands = null;

        synchronized (lock) {
            bands = downloadMap.keySet().toArray(new Band[0]);
        }

        if (bands != null) {
            for (final Band band : bands) {
                stopDownloads(band);
            }
        }
    }

    public void stopDownloads(final Band band) {
        synchronized (lock) {
            final LinkedList<Interval<Date>> list = downloadMap.get(band);

            if (list == null) {
                return;
            }

            for (int i = highPriorityDownloadQueue.size() - 1; i >= 0; --i) {
                final DownloadJob job = highPriorityDownloadQueue.get(i);

                if (job.getBand().equals(band)) {
                    list.remove(job.getInterval());
                }

                highPriorityDownloadQueue.remove(i);
            }

            for (int i = lowPriorityDownloadQueue.size() - 1; i >= 0; --i) {
                final DownloadJob job = lowPriorityDownloadQueue.get(i);

                if (job.getBand().equals(band)) {
                    list.remove(job.getInterval());
                }

                lowPriorityDownloadQueue.remove(i);
            }

            if (list.size() == 0) {
                downloadMap.remove(band);
            }
        }

        fireDownloadFinished(band, null, 0);
    }

    public boolean isDownloadActive() {
        synchronized (lock) {
            return !downloadMap.isEmpty();
        }
    }

    public boolean isDownloadActive(final Band band) {
        synchronized (lock) {
            final LinkedList<Interval<Date>> list = downloadMap.get(band);

            if (list == null) {
                return false;
            }

            return list.size() > 0;
        }
    }

    private void fireDownloadStarted(final Band band, final Interval<Date> interval) {
        for (final DownloadControllerListener listener : listeners) {
            listener.downloadStarted(band, interval);
        }
        selectorModel.downloadStarted(band);
    }

    private void fireDownloadFinished(final Band band, final Interval<Date> interval, final int activeBandDownloads) {
        for (final DownloadControllerListener listener : listeners) {
            listener.downloadFinished(band, interval, activeBandDownloads);
        }
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

    private DownloadPriority getPriority(final Interval<Date> interval, final Interval<Date> priorityInterval) {
        if (priorityInterval == null || priorityInterval.getStart() == null || priorityInterval.getEnd() == null) {
            return DownloadPriority.LOW;
        }

        if (priorityInterval.overlapsInclusive(interval)) {
            return DownloadPriority.HIGH;
        }

        return DownloadPriority.LOW;
    }

    private void addDownloads(final DownloadJob[] jobs, final DownloadPriority[] priorities) {
        synchronized (lock) {
            for (int i = 0; i < jobs.length; ++i) {
                // add to download map
                final Band band = jobs[i].getBand();
                final Interval<Date> interval = jobs[i].getInterval();

                LinkedList<Interval<Date>> list = downloadMap.get(band);

                if (list == null)
                    list = new LinkedList<Interval<Date>>();

                list.add(interval);

                downloadMap.put(band, list);

                // add to download queue
                switch (priorities[i]) {
                case LOW:
                    lowPriorityDownloadQueue.add(jobs[i]);
                    break;
                case HIGH:
                    highPriorityDownloadQueue.add(jobs[i]);
                    break;
                }
            }
        }
    }

    private void downloadFinished(final Band band, final Interval<Date> interval) {
        int numberOfDownloads = 0;

        synchronized (lock) {
            final LinkedList<Interval<Date>> list = downloadMap.get(band);

            if (list != null) {
                list.remove(interval);
                numberOfDownloads = list.size();

                if (numberOfDownloads == 0) {
                    downloadMap.remove(band);
                }
            }
        }

        fireDownloadFinished(band, interval, numberOfDownloads);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Download Job
    // //////////////////////////////////////////////////////////////////////////////

    private class DownloadJob {

        private final Band band;
        private final Interval<Date> interval;

        public DownloadJob(final Band band, final Interval<Date> interval) {
            this.band = band;
            this.interval = interval;
        }

        public Band getBand() {
            return band;
        }

        public Interval<Date> getInterval() {
            return interval;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Download Manager
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * @author Stephan Pagel
     * */
    private class DownloadManager implements Runnable {

        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        private final Semaphore finishSemaphore = new Semaphore(EVESettings.DOWNLOADER_MAX_THREADS);

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        public void run() {
            while (true) {
                try {
                    finishSemaphore.acquire();
                    final DownloadJob job = getNextDownloadJob();

                    if (job != null) {
                        final DownloadThread thread = new DownloadThread(job, finishSemaphore);
                        new Thread(thread, "EVEDownload").start();
                    } else {
                        finishSemaphore.release();
                        Thread.sleep(100);
                    }
                } catch (final InterruptedException e) {
                }
            }
        }

        private DownloadJob getNextDownloadJob() {
            synchronized (lock) {
                // try to find a download job within the high priority list
                if (!highPriorityDownloadQueue.isEmpty()) {
                    return highPriorityDownloadQueue.removeFirst();
                }

                // if no high priority job is available try to find a download
                // job within the low priority list
                if (!lowPriorityDownloadQueue.isEmpty()) {
                    return lowPriorityDownloadQueue.removeFirst();
                }

                // there is no job available
                return null;
            }
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Download Thread
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * @author Stephan Pagel
     * */
    private class DownloadThread implements Runnable {

        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        private final Interval<Date> interval;
        private final Band band;
        private final Semaphore semaphore;

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        public DownloadThread(final DownloadJob job, final Semaphore semaphore) {
            this.interval = job.getInterval();
            this.band = job.getBand();
            this.semaphore = semaphore;
        }

        public void run() {
            try {
                if (interval != null && interval.getStart() != null && interval.getEnd() != null && band != null)
                    requestData();
            } finally {
                semaphore.release();
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

            if (url == null)
                return;

            // Log.debug("Requesting EVE Data: " + url);

            // this might take a while
            try {
                DownloadStream ds = new DownloadStream(url, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());

                BufferedReader in = new BufferedReader(new InputStreamReader(ds.getInput()));
                StringBuilder sb = new StringBuilder();
                String str;

                while ((str = in.readLine()) != null) {
                    sb.append(str);
                }

                in.close();

                // try {
                // BufferedWriter out = new BufferedWriter(new FileWriter("D:\\"
                // + band.getBandType().toString() + ".json"));
                // out.write(sb.toString());
                // out.close();
                // }
                // catch (IOException e) {
                // e.printStackTrace();
                // }

                addDataToCache(new JSONObject(sb.toString()), band);
            } catch (final IOException e1) {
                Log.error("Error Parsing the EVE Response.", e1);
            } catch (final JSONException e2) {
                Log.error("Error Parsing the EVE Response.", e2);
            }
        }

        private URL buildRequestURL(final Interval<Date> interval, final BandType type) throws MalformedURLException {
            final SimpleDateFormat eveAPIDateFormat = new SimpleDateFormat(EVEAPI.API_DATE_FORMAT);

            return new URL(type.getBaseUrl() + EVEAPI.API_URL_PARAMETER_STARTDATE + eveAPIDateFormat.format(interval.getStart()) + "&" + EVEAPI.API_URL_PARAMETER_ENDDATE + eveAPIDateFormat.format(interval.getEnd()) + "&" + EVEAPI.API_URL_PARAMETER_TYPE + type.getName() + "&" + EVEAPI.API_URL_PARAMETER_FORMAT + EVEAPI.API_URL_PARAMETER_FORMAT_VALUES.JSON);
        }

        private boolean test = true;

        private void addDataToCache(final JSONObject json, final Band band) {
            try {
                double multiplier = 1.0;
                if (json.has("multiplier")) {
                    multiplier = json.getDouble("multiplier");
                }
                final JSONArray data = json.getJSONArray("data");
                // Log.warn(data.toString());
                final EVEValue[] values = new EVEValue[data.length()];

                for (int i = 0; i < data.length(); i++) {
                    final JSONArray entry = data.getJSONArray(i);

                    // used time system in data is TAI -> compute to UTC
                    final long millis = ((long) entry.getDouble(0)) * 1000;// -
                                                                           // 378691234000L;
                    // final long millis = ((long) entry.getDouble(0)*1000);
                    values[i] = new EVEValue(new Date(millis), entry.getDouble(1) * multiplier);
                    if (test) {
                        test = false;
                        System.out.println(new Date(millis));
                    }
                }

                DatabaseController.getSingletonInstance().addToDatabase(band, values);
                EVECacheController.getSingletonInstance().addToCache(values, band);
            } catch (JSONException e) {
                Log.error("", e);
            }
        }
    }
}