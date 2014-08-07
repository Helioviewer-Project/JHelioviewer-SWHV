package org.helioviewer.jhv.plugins.swek.download;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;
import org.helioviewer.jhv.plugins.swek.sources.SWEKSourceManager;

public class SWEKDownloadManager {
    /***/
    private final SWEKSourceManager sourceManager;

    /** Singleton instance of the SWE */
    private static SWEKDownloadManager instance;

    /**
     * private constructor of the SWEKDownloadManager
     */
    private SWEKDownloadManager() {
        this.sourceManager = SWEKSourceManager.getSingletonInstance();
    }

    /**
     * Gets the singleton instance of the SWEKDownloadManager
     * 
     * @return The singleton instance
     */
    public static SWEKDownloadManager getSingletonInstance() {
        if (instance == null) {
            instance = new SWEKDownloadManager();
        }
        return instance;
    }

    /**
     * Starts downloading for every source the requested event type. This will
     * start a thread to download the events.
     * 
     * @param eventType
     *            The event type to download
     */
    public void startDownloadEventType(SWEKEventType eventType) {
        for (SWEKSupplier s : eventType.getSuppliers()) {
            startDownloadEventType(eventType, s.getSource());
        }
    }

    /**
     * Starts downloading for one particular source the given event type.
     * 
     * @param eventType
     *            The event type to download
     * @param source
     *            The source from which to download the event type
     */
    public void startDownloadEventType(SWEKEventType eventType, SWEKSource source) {
        SWEKDownloader downloader = this.sourceManager.getDownloader(source);
        System.out.println("downloader : " + downloader);
    }

    /**
     * Stops downloading the event type for every source of the event type.
     * 
     * @param eventType
     *            the event type for which to stop downloads
     */
    public void stopDownloadingEventType(SWEKEventType eventType) {

    }

    /**
     * Stops downloading the event type for the given source.
     * 
     * @param eventType
     *            the event type for which to stop the downloads
     * @param source
     *            the source for which to stop the downloads
     */
    public void stopDownloadingEventType(SWEKEventType eventType, SWEKSource source) {

    }
}
