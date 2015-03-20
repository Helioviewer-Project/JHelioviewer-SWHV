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
    private SwingWorker<ImageDownloadWorkerResult, Void> imageDownloadWorker;

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
                Thread.currentThread().setName("RadioDownloader1--EVE");
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

                            while (startDate.before(endDate) || startDate.equals(endDate)) {
                                ImageInfoView v = APIRequestManager.requestAndOpenRemoteFile(false, null, createDateString(startDate), createDateString(startDate), "ROB-Humain", "CALLISTO", "CALLISTO", "RADIOGRAM", false);
                                if (v != null) {
                                    long imageID = getNextID();
                                    DownloadedJPXData newJPXData = new DownloadedJPXData(v, imageID, startDate, endDate, identifier, downloadID);
                                    jpxList.add(newJPXData);
                                    // cache.add(newJPXData);
                                } else {
                                    Log.error("Received null view in request and open for date " + startDate + " and " + endDate);
                                    noDataInterval.add(new Interval<Date>(startDate, calculateOneDayFurtherAsDate(startDate)));

                                }
                                startDate = calculateOneDayFurtherAsDate(startDate);
                            }

                        }
                    } else {
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
                            fireIntervalTooBig(result.getRequestInterval().getStart(), result.getRequestInterval().getEnd(), result.getDownloadID(), identifier);
                        } else {
                            if (!result.getImageInfoViews().isEmpty()) {
                                for (DownloadedJPXData dJPXD : result.getImageInfoViews()) {
                                    cache.add(dJPXD);
                                }
                                fireNewJPXDataAvailable(result.getImageInfoViews(), identifier, result.getRequestInterval().getStart(), result.getRequestInterval().getEnd(), result.getDownloadID());
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

    public void requestAndOpenIntervals(List<Interval<Date>> intervals, final Long downloadId, final String plotIdentifier, final double ratioX, final double ratioY) {
        for (final Interval<Date> interval : intervals) {
            while (imageDownloadWorker != null && !imageDownloadWorker.isDone()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            imageDownloadWorker = new SwingWorker<ImageDownloadWorkerResult, Void>() {

                @Override
                protected ImageDownloadWorkerResult doInBackground() {
                    Thread.currentThread().setName("RadioDownloader2--EVE");
                    try {
                        Date startDate = interval.getStart();
                        Date requestedStartDate = new Date(startDate.getTime());
                        Date endDate = interval.getEnd();
                        List<Interval<Date>> noDataList = new ArrayList<Interval<Date>>();
                        List<DownloadedJPXData> jpxList = new ArrayList<DownloadedJPXData>();
                        if (endDate != null && startDate != null) {
                            endDate.setTime(endDate.getTime());
                            // case there were not more than three days
                            while (startDate.before(endDate) || startDate.equals(endDate)) {
                                boolean inRequestCache = true;
                                synchronized (requestDateCache) {
                                    if (!requestDateCache.contains(startDate)) {
                                        inRequestCache = false;
                                        requestDateCache.add(startDate);
                                    }
                                }
                                if (!(inRequestCache || cache.containsDate(startDate, plotIdentifier))) {
                                    ImageInfoView v = null;

                                    v = APIRequestManager.requestAndOpenRemoteFile(false, null, createDateString(startDate), createDateString(startDate), "ROB-Humain", "CALLISTO", "CALLISTO", "RADIOGRAM", false);
                                    if (v != null) {
                                        Long imageID = getNextID();
                                        DownloadedJPXData newJPXData = new DownloadedJPXData(v, imageID, startDate, endDate, plotIdentifier, downloadId);
                                        jpxList.add(newJPXData);
                                        cache.add(newJPXData);
                                    } else {
                                        if (cache.addNoDataInterval(new Interval<Date>(startDate, calculateOneDayFurtherAsDate(startDate)), plotIdentifier)) {
                                            noDataList.add(new Interval<Date>(startDate, calculateOneDayFurtherAsDate(startDate)));
                                        }
                                    }
                                }
                                synchronized (requestDateCache) {
                                    requestDateCache.remove(startDate);
                                }
                                startDate = calculateOneDayFurtherAsDate(startDate);
                            }
                        }
                        return new ImageDownloadWorkerResult(jpxList, noDataList, false, new Interval<Date>(requestedStartDate, endDate), downloadId);
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
                            if (!result.getImageInfoViews().isEmpty()) {
                                fireAdditionalJPXDataAvailable(result.getImageInfoViews(), plotIdentifier, result.getRequestInterval().getStart(), result.getRequestInterval().getEnd(), downloadId, ratioX, ratioY);
                            }

                            fireNoData(result.getNoDataIntervals(), plotIdentifier, downloadId);
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
        }

    }

    private void fireAdditionalJPXDataAvailable(List<DownloadedJPXData> jpxList, String plotIdentifier, Date startDate, Date endDate, Long downloadID, double ratioX, double ratioY) {
        for (RadioDownloaderListener l : listeners) {
            l.newAdditionalDataDownloaded(jpxList, downloadID, plotIdentifier, ratioX, ratioY);
        }
    }

    private void fireNewJPXDataAvailable(List<DownloadedJPXData> jpxList, String plotIdentifier, Date startDate, Date endDate, Long downloadID) {
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
