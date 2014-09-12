package org.helioviewer.jhv.plugins.swek.download;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    private final SWEKSourceManager sourceManager;

    /** The input stream from where the raw downloaded events are coming. */
    private InputStream downloadInputStream;

    /** The stream from where the parsed events are coming */
    private SWEKEventStream eventStream;

    /** Worker start download date */
    private final Date downloadStartDate;

    /** Worker end download date */
    private final Date downloadEndDate;

    /**
     * The list containing the download worker listeners of this download worker
     */
    private final List<DownloadWorkerListener> listeners;

    /** Is the fire force stopped called */
    private boolean isFireForceStoppedCalled;

    /**
     * Default constructor.
     */
    public DownloadWorker() {
        isStopped = false;
        eventType = null;
        swekSource = null;
        eventRequestDate = new Date();
        downloadStartDate = new Date();
        downloadEndDate = new Date();
        sourceManager = SWEKSourceManager.getSingletonInstance();
        listeners = new ArrayList<DownloadWorkerListener>();
        isFireForceStoppedCalled = false;
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
        isStopped = false;
        this.swekSource = swekSource;
        this.eventType = eventType;
        eventRequestDate = date;
        downloadStartDate = new Date(getCurrentDate(eventRequestDate).getTime() - this.eventType.getRequestIntervalExtension());
        downloadEndDate = new Date(getNextDate(eventRequestDate).getTime() + this.eventType.getRequestIntervalExtension());
        listeners = new ArrayList<DownloadWorkerListener>();
        sourceManager = SWEKSourceManager.getSingletonInstance();
        isFireForceStoppedCalled = false;
    }

    /**
     * Adds a new listener to the download worker thread.
     * 
     * @param listener
     *            The listener to add.
     */
    public void addDownloadWorkerListener(DownloadWorkerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the listener from the download worker thread.
     * 
     * @param listener
     *            The listener to remove.
     */
    public void removeDownloadWorkerListener(DownloadWorkerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Stops the worker thread
     */
    public void stopWorker() {
        isStopped = true;
        downloader.stopDownload();
        parser.stopParser();
    }

    @Override
    public void run() {
        fireDownloadWorkerStarted();
        // create downloader
        downloader = createDownloader();
        // create parser
        parser = createParser();
        // download the data
        downloadData();
        // parse the data
        parseData();
        // distribute the data
        distributeData();
        fireDownloadWorkerFinished();
    }

    /**
     * Gets the start date of the download.
     * 
     * @return the start date of the download
     */
    public Date getDownloadStartDate() {
        return downloadStartDate;
    }

    /**
     * Gets the end date of the download.
     * 
     * @return the end date of the download
     */
    public Date getDownloadEndDate() {
        return downloadEndDate;
    }

    /**
     * Gets the source from which the worker downloads its data.
     * 
     * @return The source
     */
    public SWEKSource getSource() {
        return swekSource;
    }

    /**
     * Gets the event type this download worker is downloading.
     * 
     * @return The event type that is downloading
     */
    public SWEKEventType getEventType() {
        return eventType;
    }

    /**
     * Sends the events to the event container.
     */
    private void distributeData() {
        if (!isStopped) {
            if (eventStream != null) {
                while (eventStream.hasEvents()) {
                    // TODO offer event to the JHVEventContainer
                    eventStream.next();
                }
            }
        } else {
            fireDownloadWorkerForcedStopped();
        }
    }

    /**
     * Parses the source specific input stream to a jhv specific event type.
     */
    private void parseData() {
        if (!isStopped) {
            eventStream = parser.parseEventStream(downloadInputStream, eventType, swekSource);
        } else {
            if (parser != null) {
                parser.stopParser();
            }
            fireDownloadWorkerForcedStopped();
        }
    }

    /**
     * Downloads the data from the source.
     */
    private void downloadData() {
        if (!isStopped) {
            downloadInputStream = downloader.downloadData(eventType, downloadStartDate, downloadEndDate);
        } else {
            if (downloader != null) {
                downloader.stopDownload();
            }
            fireDownloadWorkerForcedStopped();
        }
    }

    /**
     * Creates a parser for the given SWEK source.
     * 
     * @return the parser for the source
     */
    private SWEKParser createParser() {
        if (!isStopped) {
            return sourceManager.getParser(swekSource);
        } else {
            fireDownloadWorkerForcedStopped();
            return null;
        }
    }

    /**
     * Requests the downloader from the source manager
     * 
     * @return The downloader or null if the download was stopped.
     */
    private SWEKDownloader createDownloader() {
        if (!isStopped) {
            return sourceManager.getDownloader(swekSource);
        } else {
            fireDownloadWorkerForcedStopped();
            return null;
        }
    }

    /**
     * Gets the date of give date with hour, minute, seconds, milliseconds to 0.
     * 
     * @param date
     *            the date to round
     * @return the rounded date
     */
    private Date getCurrentDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Gets the date rounded up to the following day. So day+1, hour, minute,
     * second, millisecond 0.
     * 
     * @param date
     *            The date to round up.
     * @return The rounded up date on the day
     */
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

    /**
     * Inform the download worker listeners the download worker was forced to
     * stop.
     */
    private void fireDownloadWorkerForcedStopped() {
        if (!isFireForceStoppedCalled) {
            for (DownloadWorkerListener l : listeners) {
                l.workerForcedToStop(this);
            }
            isFireForceStoppedCalled = true;
        }
    }

    /**
     * Inform the download worker listeners the download worker has finished.
     */
    private void fireDownloadWorkerFinished() {
        for (DownloadWorkerListener l : listeners) {
            l.workerFinished(this);
        }
    }

    /**
     * Inform the download worker listener the download worker has started.
     */
    private void fireDownloadWorkerStarted() {
        for (DownloadWorkerListener l : listeners) {
            l.workerStarted(this);
        }
    }
}
