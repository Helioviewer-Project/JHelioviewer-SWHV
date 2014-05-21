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

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.viewmodel.view.ImageInfoView;

public class RadioDownloader{
    // Make connection with server to request the jpx
    // Make structure indicating the start and stop for every jpx in the file
    // give the data for a certain zoomlevel and timerange
    private static RadioDownloader instance;
    private List<RadioDownloaderListener> listeners;
    private RadioImageCache cache;
    private Set<Date> requestDateCache;

    private final long MAXIMUM_DAYS = 172800000;

    private RadioDownloader() {
        this.listeners = new ArrayList<RadioDownloaderListener>();
        this.cache = RadioImageCache.getInstance();
        this.requestDateCache = new HashSet<Date>();
    }

    public static RadioDownloader getSingletonInstance() {
        if (instance == null) {
            instance = new RadioDownloader();
        }
        return instance;
    }

    public void requestAndOpenRemoteFile(String startDateString, String endDateString, String identifier) {
        Thread thread = new Thread(new Runnable() {
            private String startDataString;
            private String endDateString;
            private String identifier;

            public Runnable init(String startTime, String endTime, String identifier) {
                this.startDataString = startTime;
                this.endDateString = endTime;
                this.identifier = identifier;
                return this;
            }

            public void run() {
                try {
                    Log.debug("Request for date " + startDataString + " - " + endDateString);
                    long duration = calculateFrequencyDuration(startDataString, endDateString);
                    long downloadID = Math.round(1000000 * Math.random());
                    Date startDate = parseDate(startDataString);
                    Date endDate = parseDate(endDateString);
                    if (duration >= 0 && duration <= MAXIMUM_DAYS) {
                        Date requestedStartDate = new Date(startDate.getTime());
                        if (endDate != null && startDate != null) {
                            endDate.setTime(endDate.getTime());
                            // case there were not more than three days
                            List<DownloadedJPXData> jpxList = new ArrayList<DownloadedJPXData>();
                            while (startDate.before(endDate) || startDate.equals(endDate)) {
                                ImageInfoView v = APIRequestManager.requestAndOpenRemoteFile(false, null, createDateString(startDate), createDateString(startDate), "ROB-Humain", "CALLISTO", "CALLISTO", "RADIOGRAM", false);
                                if (v != null) {
                                    Long imageID = Math.round(1000000 * Math.random());
                                    DownloadedJPXData newJPXData = new DownloadedJPXData(v, imageID, startDate, endDate, identifier, downloadID);
                                    jpxList.add(newJPXData);
                                    cache.add(newJPXData);
                                } else {
                                    Log.error("Received null view for date " + startDate + " and " + endDate);
                                }
                                startDate = calculateOneDayFurtherAsDate(startDate);
                            }
                            fireNewJPXDataAvailable(jpxList, identifier, requestedStartDate, endDate, downloadID);
                        } else {

                        }
                    } else {
                        fireIntervalTooBig(startDate, endDate, downloadID, identifier);
                    }
                } catch (IOException e) {
                    Log.error("An error occured while opening the remote file!", e);
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                }
            }

            private void fireIntervalTooBig(Date startDate, Date endDate, long downloadID, String plotIdentifier) {
                for (RadioDownloaderListener l : listeners) {
                    l.intervalTooBig(startDate, endDate, downloadID, identifier);
                }

            }
        }.init(startDateString, endDateString, identifier), "LoadNewImage");

        thread.start();
    }

    public void requestAndOpenIntervals(List<Interval<Date>> intervals, Long downloadId, String plotIdentifier, double ratioX, double ratioY) {
        for (Interval<Date> interval : intervals) {
            Log.debug("Request for data for interval " + interval.getStart() + " - " + interval.getEnd());
            Thread thread = new Thread(new Runnable() {
                private String startDataString;
                private String endDateString;
                private String identifier;
                private Long downloadID;
                private double ratioX;
                private double ratioY;

                public Runnable init(String startTime, String endTime, String identifier, Long downloadID, double ratioX, double ratioY) {
                    this.startDataString = startTime;
                    this.endDateString = endTime;
                    this.identifier = identifier;
                    this.downloadID = downloadID;
                    this.ratioX = ratioX;
                    this.ratioY = ratioY;
                    return this;
                }

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
                                    ImageInfoView v = APIRequestManager.requestAndOpenRemoteFile(false, null, createDateString(startDate), createDateString(startDate), "ROB-Humain", "CALLISTO", "CALLISTO", "RADIOGRAM", false);
                                    if (v != null) {
                                        Long imageID = Math.round(1000000 * Math.random());
                                        DownloadedJPXData newJPXData = new DownloadedJPXData(v, imageID, startDate, endDate, identifier, downloadID);
                                        jpxList.add(newJPXData);
                                        cache.add(newJPXData);

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
                                fireAdditionalJPXDataAvailable(jpxList, identifier, requestedStartDate, endDate, downloadID, ratioX, ratioY);
                            }
                        } else {

                        }
                    } catch (IOException e) {
                        Log.error("An error occured while opening the remote file!", e);
                        Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                    }
                }

            }.init(createDateString(interval.getStart()), createDateString(interval.getEnd()), plotIdentifier, downloadId, ratioX, ratioY), "LoadNewImage");

            thread.start();
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

}
