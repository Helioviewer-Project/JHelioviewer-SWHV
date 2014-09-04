package org.helioviewer.jhv.plugins.swek.sources;

import java.io.InputStream;
import java.util.Date;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;

public interface SWEKDownloader {

    /**
     * If called the downloader should try to stop the download process.
     */
    public abstract void stopDownload();

    /**
     * Downloads the data for a given event type over the interval defined by
     * the startDate and the endDate.
     * 
     * @param eventType
     *            the type that should be downloaded
     * @param startDate
     *            the start date of the interval over which the events need to
     *            be downloaded
     * @param endDate
     *            the end date of the interval over which the events need to be
     *            downloaded
     * 
     * @return an inputstream giving acces to the data
     */
    public abstract InputStream downloadData(SWEKEventType eventType, Date startDate, Date endDate);

}
