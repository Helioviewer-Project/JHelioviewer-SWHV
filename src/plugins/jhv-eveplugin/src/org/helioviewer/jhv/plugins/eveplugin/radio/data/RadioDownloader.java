package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;

public class RadioDownloader {

    // Make connection with server to request the jpx
    // Make structure indicating the start and stop for every jpx in the file
    // give the data for a certain zoomlevel and timerange
    private static RadioDownloader instance;
    // private final List<RadioDownloaderListener> listeners;
    private final RadioImageCache cache;
    private final Set<Date> requestDateCache;
    private JHVWorker<ImageDownloadWorkerResult, Void> imageDownloadWorker;
    private final RadioDataManager radioDataManager;

    private static final long MAXIMUM_DAYS = 172800000;

    private static final String ROBserver = DataSources.ROBsettings.get("API.jp2images.path");

    private RadioDownloader() {
        // listeners = new ArrayList<RadioDownloaderListener>();
        cache = RadioImageCache.getInstance();
        requestDateCache = new HashSet<Date>();
        radioDataManager = RadioDataManager.getSingletonInstance();
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

    public void requestAndOpenRemoteFile(final Date startDateOuter, final Date endDateOuter) {
        radioDataManager.removeSpectrograms();

        JHVWorker<ImageDownloadWorkerResult, Void> imageDownloadWorker = new JHVWorker<ImageDownloadWorkerResult, Void>() {

            @Override
            protected ImageDownloadWorkerResult backgroundWork() {
                try {
                    List<Interval> noDataInterval = new ArrayList<Interval>();
                    List<DownloadedJPXData> jpxList = new ArrayList<DownloadedJPXData>();
                    boolean intervalTooBig = false;

                    Date startDate = startDateOuter;
                    Date endDate = endDateOuter;
                    long duration = endDate.getTime() - startDate.getTime();
                    Date requestedStartDate = new Date(startDate.getTime());
                    if (duration >= 0 && duration <= MAXIMUM_DAYS) {
                        // case there were not more than three days
                        while (startDate.compareTo(endDate) <= 0) {
                            JP2ViewCallisto v = (JP2ViewCallisto) APIRequestManager.requestAndOpenRemoteFile(ROBserver, null, TimeUtils.apiDateFormat.format(startDate), TimeUtils.apiDateFormat.format(startDate), "ROB-Humain", "CALLISTO", "CALLISTO", "RADIOGRAM", false);
                            if (v != null) {
                                long imageID = getNextID();
                                DownloadedJPXData newJPXData = new DownloadedJPXData(v, imageID, startDate, endDate);
                                jpxList.add(newJPXData);
                                // cache.add(newJPXData);
                            } else {
                                Log.error("Received null view in request and open for date " + startDate + " and " + endDate);
                                noDataInterval.add(new Interval(startDate, calculateOneDayFurtherAsDate(startDate)));
                            }
                            startDate = calculateOneDayFurtherAsDate(startDate);
                        }
                    } else {
                        intervalTooBig = true;
                    }
                    Interval requestInterval = new Interval(requestedStartDate, endDate);
                    return new ImageDownloadWorkerResult(jpxList, noDataInterval, intervalTooBig, requestInterval, new ArrayList<Date>());
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
                            radioDataManager.intervalTooBig(result.getRequestInterval().start, result.getRequestInterval().end);
                        } else {
                            if (!result.getViews().isEmpty()) {
                                for (DownloadedJPXData dJPXD : result.getViews()) {
                                    cache.add(dJPXD);
                                }
                                radioDataManager.newJPXFilesDownloaded(result.getViews(), result.getRequestInterval().start, result.getRequestInterval().end);
                            }
                            if (!result.getNoDataIntervals().isEmpty()) {
                                if (!result.isIntervalTooBig() && result.getViews().isEmpty()) {
                                    radioDataManager.noDataInDownloadInterval(result.getRequestInterval());
                                }
                                List<Interval> noDataList = new ArrayList<Interval>();
                                for (Interval noData : result.getNoDataIntervals()) {
                                    // if (cache.addNoDataInterval(noData,
                                    // identifier)) {
                                    cache.addNoDataInterval(noData);
                                    noDataList.add(noData);
                                    // }
                                }
                                radioDataManager.newNoData(noDataList);
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

        imageDownloadWorker.setThreadName("EVE--RadioDownloader1");
        EVESettings.getExecutorService().execute(imageDownloadWorker);
    }

    public void requestAndOpenIntervals(List<Interval> intervals, final double ratioX, final double ratioY) {
        final List<Date> toDownloadStartDates = new ArrayList<Date>();
        for (final Interval interval : intervals) {
            Date startDate = interval.start;
            Date endDate = interval.end;
            // case there were not more than three days
            while (startDate.compareTo(endDate) <= 0) {
                boolean inRequestCache = true;
                if (!requestDateCache.contains(startDate)) {
                    inRequestCache = false;
                    requestDateCache.add(startDate);
                }
                if (!(inRequestCache || cache.containsDate(startDate))) {
                    toDownloadStartDates.add(startDate);
                } else {
                    if (cache.containsDate(startDate)) {
                        requestDateCache.remove(startDate);
                    }
                }
                startDate = calculateOneDayFurtherAsDate(startDate);
            }
        }

        imageDownloadWorker = new JHVWorker<ImageDownloadWorkerResult, Void>() {

            private List<Date> datesToDownload;

            @Override
            protected ImageDownloadWorkerResult backgroundWork() {
                List<Interval> noDataList = new ArrayList<Interval>();
                List<DownloadedJPXData> jpxList = new ArrayList<DownloadedJPXData>();
                for (Date date : datesToDownload) {
                    JP2ViewCallisto v = null;
                    try {
                        v = (JP2ViewCallisto) APIRequestManager.requestAndOpenRemoteFile(ROBserver, null, TimeUtils.apiDateFormat.format(date), TimeUtils.apiDateFormat.format(date), "ROB-Humain", "CALLISTO", "CALLISTO", "RADIOGRAM", false);
                    } catch (IOException e) {
                        Log.error("An error occured while opening the remote file!", e);
                    }
                    if (v != null) {
                        long imageID = getNextID();
                        DownloadedJPXData newJPXData = new DownloadedJPXData(v, imageID, date, calculateOneDayFurtherAsDate(date));
                        jpxList.add(newJPXData);
                    } else {
                        noDataList.add(new Interval(date, calculateOneDayFurtherAsDate(date)));
                    }
                }
                return new ImageDownloadWorkerResult(jpxList, noDataList, false, null, datesToDownload);
            }

            @Override
            protected void done() {
                try {
                    ImageDownloadWorkerResult result = get();
                    if (result != null) {
                        if (!result.getViews().isEmpty()) {
                            for (DownloadedJPXData jpxData : result.getViews()) {
                                cache.add(jpxData);
                                ArrayList<DownloadedJPXData> temp = new ArrayList<DownloadedJPXData>();
                                temp.add(jpxData);
                                radioDataManager.newAdditionalDataDownloaded(temp, ratioX, ratioY);
                            }
                        }
                        List<Interval> noDataToFire = new ArrayList<Interval>();
                        for (Interval noDataInterval : result.getNoDataIntervals()) {
                            if (cache.addNoDataInterval(noDataInterval)) {
                                noDataToFire.add(noDataInterval);
                            }
                        }
                        radioDataManager.newNoData(noDataToFire);
                        for (Date date : result.getDatesToRemoveFromRequestCache()) {
                            requestDateCache.remove(date);
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

            public JHVWorker<ImageDownloadWorkerResult, Void> init(List<Date> toDownload) {
                datesToDownload = toDownload;
                return this;
            }
        }.init(toDownloadStartDates);

        imageDownloadWorker.setThreadName("EVE--RadioDownloader2");
        EVESettings.getExecutorService().execute(imageDownloadWorker);
    }

    private Date calculateOneDayFurtherAsDate(Date date) {
        return new Date(date.getTime() + 86400000);
    }

    /**
     * Contains the result of the ImageDownloadWorker.
     *
     * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
     *
     */
    private static class ImageDownloadWorkerResult {
        /** the list of downloaded jpx data */
        private final List<DownloadedJPXData> imageInfoViews;
        /** the no data intervals */
        private final List<Interval> noDataIntervals;
        /** the interval too big indicater */
        private final boolean intervalTooBig;
        /** The request interval */
        private final Interval requestInterval;
        /** The download id */
        private final List<Date> datesToRemoveFromRequestCache;

        /**
         * Creates an image downloader result for the given image info views, no
         * data intervals and interval too big.
         *
         * @param imageInfoViews
         *            list with downloaded image info views
         * @param noDataIntervals
         *            list with no data intervals
         * @param intervalTooBig
         *            indication the interval was too big
         * @param requestInterval
         *            the request interval
         * @param datesToDownload
         */
        public ImageDownloadWorkerResult(List<DownloadedJPXData> imageInfoViews, List<Interval> noDataIntervals, boolean intervalTooBig, Interval requestInterval, List<Date> datesToRemoveFromRequestCache) {
            this.imageInfoViews = imageInfoViews;
            this.intervalTooBig = intervalTooBig;
            this.noDataIntervals = noDataIntervals;
            this.requestInterval = requestInterval;

            this.datesToRemoveFromRequestCache = datesToRemoveFromRequestCache;
        }

        public List<Date> getDatesToRemoveFromRequestCache() {
            return datesToRemoveFromRequestCache;
        }

        /**
         * Gets the list of image info views
         *
         * @return the list of image info views
         */
        public List<DownloadedJPXData> getViews() {
            return imageInfoViews;
        }

        /**
         * Gets the list of no data intervals.
         *
         * @return the list with no data intervals
         */
        public List<Interval> getNoDataIntervals() {
            return noDataIntervals;
        }

        /**
         * Gets if the interval was too big.
         *
         * @return true if the interval was too big, false if not.
         */
        public boolean isIntervalTooBig() {
            return intervalTooBig;
        }

        /**
         * Gets the request interval.
         *
         * @return the request interval
         */
        public Interval getRequestInterval() {
            return requestInterval;
        }
    }

}
