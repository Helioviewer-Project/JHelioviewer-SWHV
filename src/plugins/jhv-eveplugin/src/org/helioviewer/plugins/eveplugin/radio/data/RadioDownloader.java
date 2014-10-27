package org.helioviewer.plugins.eveplugin.radio.data;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.viewmodel.view.ImageInfoView;

public class RadioDownloader {
    // Make connection with server to request the jpx
    // Make structure indicating the start and stop for every jpx in the file
    // give the data for a certain zoomlevel and timerange
    private static RadioDownloader instance;
    private final List<RadioDownloaderListener> listeners;
    private final RadioImageCache cache;
    private final Set<Date> requestDateCache;

    private final long MAXIMUM_DAYS = 172800000;

    private RadioDownloader() {
        listeners = new ArrayList<RadioDownloaderListener>();
        cache = RadioImageCache.getInstance();
        requestDateCache = new HashSet<Date>();
    }

    public static RadioDownloader getSingletonInstance() {
        if (instance == null) {
            instance = new RadioDownloader();
        }
        return instance;
    }

    private static long nextID = -1L;

    private static long getNextID() {
        nextID++;
        return nextID;
    }

    public void requestAndOpenRemoteFile(final String startDateString, final String endDateString, final String identifier) {
        fireRemoveRadioSpectrogram(identifier);
        SwingWorker<ImageDownloadWorkerResult, Void> imageDownloadWorker = new SwingWorker<ImageDownloadWorkerResult, Void>() {

            @Override
            protected ImageDownloadWorkerResult doInBackground() {
                try {
                    List<Interval<Date>> noDataInterval = new ArrayList<Interval<Date>>();
                    List<DownloadedJPXData> jpxList = new ArrayList<DownloadedJPXData>();
                    boolean intervalTooBig = false;
                    long duration = calculateFrequencyDuration(startDateString, endDateString);
                    long downloadID = getNextID();
                    Date startDate = parseDate(startDateString);
                    Date endDate = parseDate(endDateString);
                    Date requestedStartDate = new Date(startDate.getTime());
                    if (duration >= 0 && duration <= MAXIMUM_DAYS) {
                        if (endDate != null && startDate != null) {
                            endDate.setTime(endDate.getTime());
                            // case there were not more than three days

                            // List<Interval<Date>> noDataList = new
                            // ArrayList<Interval<Date>>();
                            while (startDate.before(endDate) || startDate.equals(endDate)) {
                                ImageInfoView v = APIRequestManager.requestAndOpenRemoteFile(false, null, createDateString(startDate),
                                        createDateString(startDate), "ROB-Humain", "CALLISTO", "CALLISTO", "RADIOGRAM", false);
                                if (v != null) {
                                    long imageID = getNextID();
                                    DownloadedJPXData newJPXData = new DownloadedJPXData(v, imageID, startDate, endDate, identifier,
                                            downloadID);
                                    jpxList.add(newJPXData);
                                    // cache.add(newJPXData);
                                } else {
                                    Log.error("Received null view in request and open for date " + startDate + " and " + endDate);
                                    noDataInterval.add(new Interval<Date>(startDate, calculateOneDayFurtherAsDate(startDate)));
                                    /*
                                     * if (cache.addNoDataInterval(new
                                     * Interval<Date>(startDate,
                                     * calculateOneDayFurtherAsDate(startDate)),
                                     * identifier)) { noDataList.add(new
                                     * Interval<Date>(startDate,
                                     * calculateOneDayFurtherAsDate
                                     * (startDate))); }
                                     */
                                }
                                startDate = calculateOneDayFurtherAsDate(startDate);
                            }
                            /*
                             * fireNewJPXDataAvailable(jpxList, identifier,
                             * requestedStartDate, endDate, downloadID);
                             * Log.trace
                             * ("Data in no data in request and open : " +
                             * noDataList.size()); fireNoData(noDataList,
                             * identifier, downloadID);
                             */
                        }
                    } else {
                        // fireIntervalTooBig(startDate, endDate, downloadID,
                        // identifier);
                        intervalTooBig = true;
                    }
                    Interval<Date> requestInterval = new Interval<Date>(requestedStartDate, endDate);
                    return new ImageDownloadWorkerResult(jpxList, noDataInterval, intervalTooBig, requestInterval, downloadID);
                } catch (IOException e) {
                    Log.error("An error occured while opening the remote file!", e);
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    ImageDownloadWorkerResult result = get();
                    if (result != null) {
                        if (result.isIntervalTooBig()) {
                            fireIntervalTooBig(result.getRequestInterval().getStart(), result.getRequestInterval().getEnd(),
                                    result.getDownloadID(), identifier);
                        } else {
                            if (!result.getImageInfoViews().isEmpty()) {
                                for (DownloadedJPXData dJPXD : result.getImageInfoViews()) {
                                    cache.add(dJPXD);
                                }
                                fireNewJPXDataAvailable(result.getImageInfoViews(), identifier, result.getRequestInterval().getStart(),
                                        result.getRequestInterval().getEnd(), result.getDownloadID());
                            }
                            if (!result.getNoDataIntervals().isEmpty()) {
                                if (!result.isIntervalTooBig() && result.getImageInfoViews().isEmpty()) {
                                    fireNoDataInDownloadInterval(result.getRequestInterval(), result.getDownloadID(), identifier);
                                }
                                List<Interval<Date>> noDataList = new ArrayList<Interval<Date>>();
                                for (Interval<Date> noData : result.getNoDataIntervals()) {
                                    // if (cache.addNoDataInterval(noData,
                                    // identifier)) {
                                    cache.addNoDataInterval(noData, identifier);
                                    noDataList.add(noData);
                                    // }
                                }
                                fireNoData(noDataList, identifier, result.getDownloadID());
                            }
                        }

                    }
                } catch (InterruptedException e) {
                    Log.error("ImageDownloadWorker execution interrupted: " + e.getMessage());
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    Log.error("ImageDownloadWorker execution error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        imageDownloadWorker.execute();

        /*
         * Thread thread = new Thread(new Runnable() { private String
         * startDataString; private String endDateString; private String
         * identifier;
         * 
         * public Runnable init(String startTime, String endTime, String
         * identifier) { startDataString = startTime; endDateString = endTime;
         * this.identifier = identifier; return this; }
         * 
         * @Override public void run() { try { // Log.debug("Request for date "
         * + startDataString + " - " + // endDateString); long duration =
         * calculateFrequencyDuration(startDataString, endDateString); long
         * downloadID = Math.round(1000000 * Math.random()); Date startDate =
         * parseDate(startDataString); Date endDate = parseDate(endDateString);
         * if (duration >= 0 && duration <= MAXIMUM_DAYS) { Date
         * requestedStartDate = new Date(startDate.getTime()); if (endDate !=
         * null && startDate != null) { endDate.setTime(endDate.getTime()); //
         * case there were not more than three days List<DownloadedJPXData>
         * jpxList = new ArrayList<DownloadedJPXData>(); List<Interval<Date>>
         * noDataList = new ArrayList<Interval<Date>>(); while
         * (startDate.before(endDate) || startDate.equals(endDate)) {
         * ImageInfoView v = APIRequestManager.requestAndOpenRemoteFile(false,
         * null, createDateString(startDate), createDateString(startDate),
         * "ROB-Humain", "CALLISTO", "CALLISTO", "RADIOGRAM", false); if (v !=
         * null) { Long imageID = Math.round(1000000 * Math.random());
         * DownloadedJPXData newJPXData = new DownloadedJPXData(v, imageID,
         * startDate, endDate, identifier, downloadID); jpxList.add(newJPXData);
         * cache.add(newJPXData); } else {
         * Log.error("Received null view in request and open for date " +
         * startDate + " and " + endDate); if (cache.addNoDataInterval(new
         * Interval<Date>(startDate, calculateOneDayFurtherAsDate(startDate)),
         * identifier)) { noDataList.add(new Interval<Date>(startDate,
         * calculateOneDayFurtherAsDate(startDate))); } } startDate =
         * calculateOneDayFurtherAsDate(startDate); }
         * fireNewJPXDataAvailable(jpxList, identifier, requestedStartDate,
         * endDate, downloadID);
         * Log.trace("Data in no data in request and open : " +
         * noDataList.size()); fireNoData(noDataList, identifier, downloadID); }
         * } else { fireIntervalTooBig(startDate, endDate, downloadID,
         * identifier); } } catch (IOException e) {
         * Log.error("An error occured while opening the remote file!", e);
         * Message.err("An error occured while opening the remote file!",
         * e.getMessage(), false); } }
         * 
         * 
         * }.init(startDateString, endDateString, identifier), "LoadNewImage");
         * 
         * thread.start();
         */
    }

    protected void fireNoDataInDownloadInterval(Interval<Date> requestInterval, Long downloadID, String identifier) {
        for (RadioDownloaderListener l : listeners) {
            l.noDataInDownloadInterval(requestInterval, downloadID, identifier);
        }

    }

    private void fireIntervalTooBig(Date startDate, Date endDate, long downloadID, String plotIdentifier) {
        for (RadioDownloaderListener l : listeners) {
            l.intervalTooBig(startDate, endDate, downloadID, plotIdentifier);
        }
    }

    /**
     * Instructs the radio downloader listeners to remove the spectrograms from
     * the plot identified by the plot identifier
     * 
     * @param identifier
     *            The identifier of the plot from which the radio spectrograms
     *            should be removed.
     */
    private void fireRemoveRadioSpectrogram(String identifier) {
        for (RadioDownloaderListener l : listeners) {
            l.removeSpectrograms(identifier);
        }
    }

    public void requestAndOpenIntervals(List<Interval<Date>> intervals, Long downloadId, String plotIdentifier, double ratioX, double ratioY) {
        for (Interval<Date> interval : intervals) {
            // Log.debug("Request for data for interval " + interval.getStart()
            // + " - " + interval.getEnd());
            Thread thread = new Thread(
                    new Runnable() {
                        private String startDataString;
                        private String endDateString;
                        private String identifier;
                        private Long downloadID;
                        private double ratioX;
                        private double ratioY;
                        private List<Interval<Date>> noDataList;

                        public Runnable init(String startTime, String endTime, String identifier, Long downloadID, double ratioX,
                                double ratioY) {
                            startDataString = startTime;
                            endDateString = endTime;
                            this.identifier = identifier;
                            this.downloadID = downloadID;
                            this.ratioX = ratioX;
                            this.ratioY = ratioY;
                            noDataList = new ArrayList<Interval<Date>>();
                            return this;
                        }

                        @Override
                        public void run() {
                            try {
                                Date startDate = parseDate(startDataString);
                                Date requestedStartDate = new Date(startDate.getTime());
                                Date endDate = parseDate(endDateString);
                                if (endDate != null && startDate != null) {
                                    endDate.setTime(endDate.getTime());
                                    // case there were not more than three days
                                    List<DownloadedJPXData> jpxList = new ArrayList<DownloadedJPXData>();
                                    while (startDate.before(endDate) || startDate.equals(endDate)) {
                                        boolean inRequestCache = true;
                                        synchronized (requestDateCache) {
                                            if (!requestDateCache.contains(startDate)) {
                                                inRequestCache = false;
                                                Log.trace("Add date " + startDate + " to request cache");
                                                requestDateCache.add(startDate);
                                            }
                                        }
                                        if (!(inRequestCache || cache.containsDate(startDate, identifier))) {
                                            ImageInfoView v = APIRequestManager.requestAndOpenRemoteFile(false, null,
                                                    createDateString(startDate), createDateString(startDate), "ROB-Humain", "CALLISTO",
                                                    "CALLISTO", "RADIOGRAM", false);
                                            if (v != null) {
                                                Long imageID = getNextID();
                                                DownloadedJPXData newJPXData = new DownloadedJPXData(v, imageID, startDate, endDate,
                                                        identifier, downloadID);
                                                jpxList.add(newJPXData);
                                                cache.add(newJPXData);
                                            } else {
                                                Log.error("Received null view in request intervals for date " + startDate + " and "
                                                        + endDate);
                                                if (cache.addNoDataInterval(new Interval<Date>(startDate,
                                                        calculateOneDayFurtherAsDate(startDate)), identifier)) {
                                                    noDataList.add(new Interval<Date>(startDate, calculateOneDayFurtherAsDate(startDate)));
                                                }
                                            }
                                        } else {
                                            if (inRequestCache) {
                                                Log.trace("Date was already in the request cache. Do nothing.");
                                            } else {
                                                Log.trace("Date was already in the radio image cache. Do nothing.");
                                            }
                                        }
                                        synchronized (requestDateCache) {
                                            requestDateCache.remove(startDate);
                                            Log.trace("remove " + startDate + " from request cache");
                                        }
                                        startDate = calculateOneDayFurtherAsDate(startDate);
                                    }
                                    if (!jpxList.isEmpty()) {
                                        fireAdditionalJPXDataAvailable(jpxList, identifier, requestedStartDate, endDate, downloadID,
                                                ratioX, ratioY);
                                    }
                                    Log.trace("Size of noDataList in request intervals: " + noDataList.size());
                                    fireNoData(noDataList, identifier, downloadID);
                                }
                            } catch (IOException e) {
                                Log.error("An error occured while opening the remote file!", e);
                                Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                            }
                        }

                    }.init(createDateString(interval.getStart()), createDateString(interval.getEnd()), plotIdentifier, downloadId, ratioX,
                            ratioY),
                    "LoadNewImage" + getNextID());

            thread.start();
        }
    }

    private void fireAdditionalJPXDataAvailable(List<DownloadedJPXData> jpxList, String plotIdentifier, Date startDate, Date endDate,
            Long downloadID, double ratioX, double ratioY) {
        for (RadioDownloaderListener l : listeners) {
            l.newAdditionalDataDownloaded(jpxList, downloadID, plotIdentifier, ratioX, ratioY);
        }
    }

    private void fireNewJPXDataAvailable(List<DownloadedJPXData> jpxList, String plotIdentifier, Date startDate, Date endDate,
            Long downloadID) {
        for (RadioDownloaderListener l : listeners) {
            l.newJPXFilesDownloaded(jpxList, startDate, endDate, downloadID, plotIdentifier);
        }
    }

    public void addRadioDownloaderListener(RadioDownloaderListener l) {
        listeners.add(l);
    }

    public void removeDownloaderListener(RadioDownloaderListener l) {
        listeners.remove(l);
    }

    private long calculateFrequencyDuration(String startTime, String endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date start;
        try {
            start = sdf.parse(startTime);
            Date end = sdf.parse(endTime);
            return end.getTime() - start.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }

    }

    private Date calculateOneDayFurtherAsDate(Date date) {
        return new Date(date.getTime() + 86400000);
    }

    private Date parseDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String createDateString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private void fireNoData(List<Interval<Date>> noDataList, String identifier, long downloadID) {
        for (RadioDownloaderListener l : listeners) {
            l.newNoData(noDataList, identifier, downloadID);
        }
    }

}
