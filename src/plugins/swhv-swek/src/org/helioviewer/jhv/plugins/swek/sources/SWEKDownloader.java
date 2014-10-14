package org.helioviewer.jhv.plugins.swek.sources;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKParam;

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
     * @param params
     *            list of parameters to be used in the downloader
     * @param page
     *            the page that should be downloaded
     * @return an input stream giving access to the data
     */
    public abstract InputStream downloadData(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params, int page);

}
