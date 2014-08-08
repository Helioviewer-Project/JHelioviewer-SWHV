package org.helioviewer.jhv.plugins.swek.download;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;
import org.helioviewer.jhv.plugins.swek.sources.SWEKParser;
import org.helioviewer.jhv.plugins.swek.sources.SWEKSourceManager;

/**
 * A download worker will download events for a type of event from a source.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class DownloadWorker implements Runnable {
    /** The event type to download */
    private final SWEKEventType eventType;

    /** The downloader used to download */
    private final SWEKSource swekSource;

    /** The date for which the event was requested */
    private final Date eventRequestDate;

    /** Should the download stop */
    private boolean isStopped;

    /** The downloader */
    private SWEKDownloader downloader;

    /** The parser */
    private SWEKParser parser;

    /** The source manager */
    private SWEKSourceManager sourceManager;

    private InputStream downloadInputStream;

    private SWEKEventStream eventStream;

    /**
     * Default constructor.
     */
    public DownloadWorker() {
        this.isStopped = false;
        this.eventType = null;
        this.swekSource = null;
        this.eventRequestDate = new Date();
        this.sourceManager = SWEKSourceManager.getSingletonInstance();
    }

    /**
     * Creates a worker thread to download the events of the given event type
     * from the given source for a given date.
     * 
     * @param eventType
     *            the type to download
     * @param swekSource
     *            the source from which to download
     * @param date
     *            the date for which to download the events
     * 
     */
    public DownloadWorker(SWEKEventType eventType, SWEKSource swekSource, Date date) {
        this.isStopped = false;
        this.swekSource = swekSource;
        this.eventType = eventType;
        this.eventRequestDate = date;
    }

    /**
     * Stops the worker thread
     */
    public void stopWorker() {
        this.isStopped = true;
        this.downloader.stopDownload();
        this.parser.stopParser();
    }

    @Override
    public void run() {
        // create downloader
        this.downloader = createDownloader();
        // create parser
        this.parser = createParser();
        // download the data
        downloadData();
        // parse the data
        parseData();
        // distribute the data
        distributeData();
    }

    /**
     * Sends the events to the event container.
     */
    private void distributeData() {
        if (this.eventStream != null) {
            while (this.eventStream.hasEvents()) {
                // TODO offer event to the JHVEventContainer
                this.eventStream.next();
            }
        }
    }

    /**
     * Parses the source specific input stream to a jhv specific event type.
     */
    private void parseData() {
        this.eventStream = this.parser.parseEventStream(this.downloadInputStream);
    }

    private void downloadData() {
        Date startDate = new Date(getCurrentDate(this.eventRequestDate).getTime() - this.eventType.getRequestIntervalExtension());
        Date endDate = new Date(getNextDate(this.eventRequestDate).getTime() + this.eventType.getRequestIntervalExtension());
        // TODO define start and end time of the interval specific for the
        this.downloadInputStream = this.downloader.downloadData(this.eventType, startDate, endDate);
    }

    private SWEKParser createParser() {
        if (!this.isStopped) {
            return this.sourceManager.getParser(this.swekSource);
        } else {
            return null;
        }
    }

    /**
     * Requests the downloader from the source manager
     * 
     * @return The downloader or null if the download was stopped.
     */
    private SWEKDownloader createDownloader() {
        if (!this.isStopped) {
            return this.sourceManager.getDownloader(this.swekSource);
        } else {
            return null;
        }
    }

    private Date getCurrentDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date getNextDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();

    }
}
