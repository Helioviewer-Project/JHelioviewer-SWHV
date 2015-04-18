package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.Date;
import java.util.List;

import org.helioviewer.base.interval.Interval;

/**
 * Contains the result of the ImageDownloadWorker.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class ImageDownloadWorkerResult {
    /** the list of downloaded jpx data */
    private final List<DownloadedJPXData> imageInfoViews;
    /** the no data intervals */
    private final List<Interval<Date>> noDataIntervals;
    /** the interval too big indicater */
    private final boolean intervalTooBig;
    /** The request interval */
    private final Interval<Date> requestInterval;
    /** The download id */
    private final Long downloadID;
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
    public ImageDownloadWorkerResult(List<DownloadedJPXData> imageInfoViews, List<Interval<Date>> noDataIntervals, boolean intervalTooBig, Interval<Date> requestInterval, long downloadID, List<Date> datesToRemoveFromRequestCache) {
        this.imageInfoViews = imageInfoViews;
        this.intervalTooBig = intervalTooBig;
        this.noDataIntervals = noDataIntervals;
        this.requestInterval = requestInterval;
        this.downloadID = downloadID;
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
    public List<Interval<Date>> getNoDataIntervals() {
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
    public Interval<Date> getRequestInterval() {
        return requestInterval;
    }

    /**
     * Gets the downloadID.
     * 
     * @return the downloadID
     */
    public Long getDownloadID() {
        return downloadID;
    }
}
