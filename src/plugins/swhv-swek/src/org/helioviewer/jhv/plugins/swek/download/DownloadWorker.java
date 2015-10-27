package org.helioviewer.jhv.plugins.swek.download;

import java.awt.EventQueue;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKRelatedEvents;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;
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

    /** The supplier providing the events */
    private final SWEKSupplier supplier;

    /** Should the download stop */
    private boolean isStopped;

    /** The downloader */
    private SWEKDownloader downloader;

    /** The parser */
    private SWEKParser parser;

    /** The source manager */
    private final SWEKSourceManager sourceManager;

    /** Worker start download date */
    private final Date downloadStartDate;

    /** Worker end download date */
    private final Date downloadEndDate;

    /** instance of the JHV Event container */
    private final JHVEventContainer eventContainer;

    /**
     * The list containing the download worker listeners of this download worker
     */
    private final List<DownloadWorkerListener> listeners;

    /** Is the fire force stopped called */
    private boolean isFireForceStoppedCalled;

    /** List of parameters to use in the download. */
    private final List<SWEKParam> params;

    /** The related event rules */
    private final List<SWEKRelatedEvents> relatedEvents;

    /** The request interval */
    private final Interval<Date> requestInterval;

    /**
     * Creates a worker thread to download the events of the given event type,
     * from the given source for a given interval.
     *
     * @param eventType
     *            the type to download
     * @param swekSource
     *            the source from which to download
     * @param supplier
     *            the supplier providing the event
     * @param interval
     *            the interval for which to download
     * @param prams
     *            the parameters to use in the download
     */
    public DownloadWorker(SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier supplier, Interval<Date> interval, List<SWEKParam> params, List<SWEKRelatedEvents> relatedEventRules) {
        // Log.debug("Create dw " + this + " downloading interval " + interval);
        // Thread.dumpStack();
        isStopped = false;
        requestInterval = interval;
        this.swekSource = swekSource;
        this.eventType = eventType;
        downloadStartDate = new Date(interval.getStart().getTime() - this.eventType.getRequestIntervalExtension());
        downloadEndDate = new Date(interval.getEnd().getTime() + this.eventType.getRequestIntervalExtension());
        listeners = new ArrayList<DownloadWorkerListener>();
        sourceManager = SWEKSourceManager.getSingletonInstance();
        isFireForceStoppedCalled = false;
        eventContainer = JHVEventContainer.getSingletonInstance();
        this.params = params;
        this.supplier = supplier;
        relatedEvents = relatedEventRules;
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
        // Log.debug("Downloadworker stopped: " + this);
        // Thread.dumpStack();
        isStopped = true;
        if (downloader != null) {
            downloader.stopDownload();
        }
        if (parser != null) {
            parser.stopParser();
        }
    }

    @Override
    public void run() {
        if (!isStopped) {
            // Log.debug("Start download worker: " + this);
            fireDownloadWorkerStarted();
            // create downloader
            downloader = createDownloader();
            // create parser
            parser = createParser();
            //
            boolean moreDownloads = false;
            int page = 0;
            do {
                // download the data
                InputStream downloadInputStream = downloadData(page);
                // parse the data
                SWEKEventStream swekEventStream = parseData(downloadInputStream);
                if (swekEventStream != null) {
                    moreDownloads = swekEventStream.additionalDownloadNeeded();
                }
                // distribute the data
                distributeData(swekEventStream);
                page++;
            } while (moreDownloads && !isStopped);
            // inform JHVEventContainer data finished downloading
            if (!isStopped) {
                try {
                    EventQueue.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            eventContainer.finishedDownload(false);
                        }
                    });
                } catch (InvocationTargetException e) {
                    Log.error("Invoke and wait called from event queue", e);
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    Log.error("Invoke and wait interrupted", e);
                    e.printStackTrace();
                }

            }
            fireDownloadWorkerFinished();
            // Log.debug("Downloadworker finished: " + this);
        } else {
            // Log.debug("Download worker was stopped before it could be started");
            // Log.debug(this);
            fireDownloadWorkerForcedStopped();
            fireDownloadWorkerFinished();
        }
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
     * Gets the supplier providing the events
     *
     * @return the supplier
     */
    public SWEKSupplier getSupplier() {
        return supplier;
    }

    public Interval<Date> getRequestInterval() {
        return requestInterval;
    }

    /**
     * Sends the events to the event container.
     *
     * @param swekEventStream
     * @throws InterruptedException
     * @throws InvocationTargetException
     */
    private void distributeData(final SWEKEventStream eventStream) {
        if (!isStopped) {
            if (eventStream != null) {
                try {
                    EventQueue.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            while (eventStream.hasEvents() && !isStopped) {
                                eventContainer.addEvent(eventStream.next());
                            }
                            eventContainer.finishedDownload(true);
                        }

                    });
                } catch (InvocationTargetException e) {
                    Log.error("Invoke and wait called from event queue", e);
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    Log.error("Invoke and wait interrupted", e);
                    e.printStackTrace();
                }

            }
        } else {
            fireDownloadWorkerForcedStopped();
        }
    }

    /**
     * Parses the source specific input stream to a jhv specific event type.
     *
     * @return
     */
    private SWEKEventStream parseData(InputStream downloadInputStream) {
        if (!isStopped) {
            return parser.parseEventStream(downloadInputStream, eventType, swekSource, supplier, relatedEvents);
            // return eventStream.additionalDownloadNeeded();
        } else {
            if (parser != null) {
                parser.stopParser();
            }
            fireDownloadWorkerForcedStopped();
            return null;
        }
    }

    /**
     * Downloads the data from the source.
     *
     * @param page
     * @return
     */
    private InputStream downloadData(int page) {
        if (!isStopped) {
            return downloader.downloadData(eventType, downloadStartDate, downloadEndDate, params, page);
        } else {
            if (downloader != null) {
                downloader.stopDownload();
            }
            fireDownloadWorkerForcedStopped();
            return null;
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
     * Inform the download worker listeners the download worker was forced to
     * stop.
     */
    private void fireDownloadWorkerForcedStopped() {
        if (!isFireForceStoppedCalled) {
            try {
                EventQueue.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        for (DownloadWorkerListener l : listeners) {
                            l.workerForcedToStop(DownloadWorker.this);
                        }
                    }

                });
            } catch (InvocationTargetException e) {
                Log.error("Invoke and wait called from event queue", e);
                e.printStackTrace();
            } catch (InterruptedException e) {
                Log.error("Invoke and wait interrupted", e);
                e.printStackTrace();
            }
            isFireForceStoppedCalled = true;
        }
    }

    /**
     * Inform the download worker listeners the download worker has finished.
     */
    private void fireDownloadWorkerFinished() {
        try {
            EventQueue.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    for (DownloadWorkerListener l : listeners) {
                        l.workerFinished(DownloadWorker.this);
                    }
                }

            });
        } catch (InvocationTargetException e) {
            Log.error("Invoke and wait called from event queue", e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.error("Invoke and wait interrupted", e);
            e.printStackTrace();
        }

    }

    /**
     * Inform the download worker listener the download worker has started.
     */
    private void fireDownloadWorkerStarted() {
        try {
            EventQueue.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    for (DownloadWorkerListener l : listeners) {
                        l.workerStarted(DownloadWorker.this);
                    }
                }

            });
        } catch (InvocationTargetException e) {
            Log.error("Invoke and wait called from event queue", e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.error("Invoke and wait interrupted", e);
            e.printStackTrace();
        }
    }

    public JHVEventType getJHVEventType() {
        return new JHVSWEKEventType(eventType.getEventName(), swekSource.getSourceName(), supplier.getSupplierName());
    }

}
