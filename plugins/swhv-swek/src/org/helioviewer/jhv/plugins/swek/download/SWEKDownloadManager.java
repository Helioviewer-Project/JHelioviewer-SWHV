package org.helioviewer.jhv.plugins.swek.download;

/**
 * Manages all the downloaders and downloads of the SWEK plugin.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKDownloadManager {
    /** Singleton instance of the swek download manager */
    private static SWEKDownloadManager instance;

    /**
     * private constructor
     */
    private SWEKDownloadManager() {

    }

    /**
     * Gets the singleton instance of the SWEK download manager.
     * 
     * @return the instance of the SWEK download manager
     */
    public SWEKDownloadManager getSingletonInstance() {
        if (instance == null) {
            instance = new SWEKDownloadManager();
        }
        return instance;
    }
}
