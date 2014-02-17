package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKSettings;

/**
 * This is the basic class to access the API. It does not deal with the
 * resulting data, but needs to make proper requests.
 * <p>
 * This is class is 'stupid', because it doesn't do any optimizations to reduce
 * the number of request to be done and doesn't use any parallelism.
 */
public class HEKStupidDownloader {

    /**
     * The ExecutorService running the actual downloadRequests
     */
    private ExecutorService threadExecutor = Executors.newFixedThreadPool(HEKSettings.DOWNLOADER_MAX_THREADS);

    /**
     * Store all requests so that they can be canceled later on
     */
    private Vector<HEKRequest> downloadRequests = new Vector<HEKRequest>();

    // the sole instance of this class
    private static final HEKStupidDownloader singletonInstance = new HEKStupidDownloader();

    /**
     * The private constructor to support the singleton pattern.
     * */
    private HEKStupidDownloader() {
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static HEKStupidDownloader getSingletonInstance() {
        return singletonInstance;
    }

    /**
     * Set the downloading state of the whole downloader
     * 
     * @param keepDownloading
     *            - new downloading state
     */
    public void cancelDownloads() {

        // set all "open" requests to canceled
        for (HEKRequest request : downloadRequests) {
            request.cancel();
        }

        // shut down the executor, so that no new requests are started
        threadExecutor.shutdownNow();

        // create a fresh and new executor
        threadExecutor = Executors.newFixedThreadPool(HEKSettings.DOWNLOADER_MAX_THREADS);

        // clear all downloadRequests
        downloadRequests = new Vector<HEKRequest>();

        // update the treeview
        HEKCache.getSingletonInstance().getController().fireEventsChanged(HEKCache.getSingletonInstance().getController().getRootPath());// cacheModel.getFirstVisiblePath(path));
    }

    /**
     * Request the actual events for the given (HEKPath,Intervals) tuples
     * <p>
     * 
     * @param cacheModel
     *            - cacheModel to fill
     * @param request
     *            - (HEKPath,Intervals) tuples
     */
    public void requestEvents(final HEKCacheController cacheController, HashMap<HEKPath, Vector<Interval<Date>>> request) {

        Iterator<HEKPath> keyIterator = request.keySet().iterator();
        while (keyIterator.hasNext()) {

            HEKPath key = keyIterator.next();

            cacheController.setState(key, HEKCacheLoadingModel.PATH_QUEUED);

            for (Interval<Date> curInterval : request.get(key)) {
                HEKRequestThread hekRequest = new HEKRequestThread(cacheController, key, curInterval);
                downloadRequests.add(hekRequest);
                threadExecutor.execute(hekRequest);
            }

        }

    }

    /**
     * Request the structure (which types of events are available) for the given
     * intervals
     * <p>
     * This is one of the methods that are 'stupid' at the moment
     * 
     * @param cacheModel
     *            - cacheModel to fill
     * @param needed
     *            - the intervals to request the structure for
     */
    public void requestStructure(HEKCacheController cacheController, Interval<Date> interval) {
        HEKRequestStructureThread hekRequest = new HEKRequestStructureThread(cacheController, interval);
        downloadRequests.add(hekRequest);
        threadExecutor.execute(hekRequest);
    }

}
