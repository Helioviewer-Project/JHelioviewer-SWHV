package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.helioviewer.base.math.Interval;
import org.helioviewer.base.math.IntervalContainer;
import org.helioviewer.base.math.IntervalStore;

/**
 * Datastructure to store a different items.
 * 
 * These items can be categorized by using an appropriate {@link HEKPath} to
 * address then The items are also associated with one (or more) intervals,
 * prepresenting during which request they have been downloaded.
 * 
 * This makes it possible to only request missing intervals when querying a
 * timerange, which has is partially covered by requests done before.
 * 
 * @author nuhn
 * 
 */
public class HEKCache {

    private static final HEKCache singletonInstance = new HEKCache();

    private HashMap<HEKPath, IntervalStore<Date, HEKEvent>> tracks = new HashMap<HEKPath, IntervalStore<Date, HEKEvent>>();

    private HEKCacheModel model = new HEKCacheModel(this);
    private HEKCacheController controller = new HEKCacheController(this);

    private HEKCacheTreeModel treeModel = new HEKCacheTreeModel(this);
    private HEKCacheSelectionModel selectionModel = new HEKCacheSelectionModel(this);
    private HEKCacheLoadingModel loadingModel = new HEKCacheLoadingModel(this);
    private HEKCacheExpansionModel expansionModel = new HEKCacheExpansionModel(this);

    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    /**
     * The private constructor to support the singleton pattern.
     * */
    private HEKCache() {
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static HEKCache getSingletonInstance() {
        return singletonInstance;
    }

    public ReentrantReadWriteLock getLock() {
        return rwl;
    }

    public void lockRead() {
        this.getLock().readLock().lock();
    }

    public void unlockRead() {
        this.getLock().readLock().unlock();
    }

    public void lockWrite() {
        // Log.info(rwl);
        this.getLock().writeLock().lock();
    }

    public void unlockWrite() {
        // Log.info(rwl);
        this.getLock().writeLock().unlock();
    }

    /**
     * @return - the cache's model
     */
    public HEKCacheModel getModel() {
        return this.model;
    }

    /**
     * @return - the cache's controller
     */
    public HEKCacheController getController() {
        return this.controller;
    }

    /**
     * Return a single track using the given path
     * 
     * @param path
     *            - path that specifies the track to be requested
     * @return - the selected track
     */
    public IntervalStore<Date, HEKEvent> getTrack(HEKPath path) {
        return this.getTracks().get(path);
    }

    /**
     * Store a single track using the given path
     * 
     * @param path
     *            - path that specifies where the track is to be stored
     * @param store
     *            - intervalstore which is to be used
     * @return - the selected track
     */
    public void setTrack(HEKPath path, IntervalStore<Date, HEKEvent> store) {
        this.tracks.put(path, store);
    }

    /**
     * Create an empty intervalstore if the given track doesn't exist yet
     * 
     * @param track
     *            - path to be made available
     */
    public void addTrack(HEKPath track) {
        if (!this.containsTrack(track)) {
            this.tracks.put(track, new IntervalStore<Date, HEKEvent>());
        }
    }

    /**
     * Store events on a track with a specific interval(store)
     * 
     * @param trackPath
     *            - path to where the events are to be stored
     * @param newInterval
     *            - interval pointing to the intervalstore to be stored in
     * @param events
     *            - events to be stored
     */
    public void addToTrack(HEKPath trackPath, Interval<Date> newInterval, Vector<HEKEvent> events) {
        // Log.info("Adding to track " + trackPath + " interval " + newInterval
        // + " events " + events);
        addTrack(trackPath);
        IntervalStore<Date, HEKEvent> theTrack = getTrack(trackPath);
        // Log.info("Current Track is " + theTrack);
        IntervalContainer<Date, HEKEvent> newContainer = new IntervalContainer<Date, HEKEvent>(events);
        theTrack.add(newInterval, newContainer);
    }

    /**
     * Set all tracks
     * 
     * @param tracks
     */
    public void setTracks(HashMap<HEKPath, IntervalStore<Date, HEKEvent>> tracks) {
        this.tracks = tracks;
    }

    /**
     * Get all tracks
     * 
     * @return - tracks
     */
    public HashMap<HEKPath, IntervalStore<Date, HEKEvent>> getTracks() {
        return tracks;
    }

    /**
     * Return all intervals that are needed to have the requested interval
     * available
     */
    public HashMap<HEKPath, Vector<Interval<Date>>> needed(HashMap<HEKPath, Vector<Interval<Date>>> request) {
        HashMap<HEKPath, Vector<Interval<Date>>> result = new HashMap<HEKPath, Vector<Interval<Date>>>();

        Iterator<HEKPath> requestKeyIterator = request.keySet().iterator();
        while (requestKeyIterator.hasNext()) {
            HEKPath requestKey = requestKeyIterator.next();

            // TODO: Malte Nuhn - create method for this case
            if (!this.getTracks().containsKey(requestKey)) {
                this.tracks.put(requestKey, new IntervalStore<Date, HEKEvent>());
            }

            Vector<Interval<Date>> requestIntervals = request.get(requestKey);
            Vector<Interval<Date>> neededIntervals = this.getTracks().get(requestKey).needed(requestIntervals);

            if (neededIntervals.size() > 0) {
                result.put(requestKey, neededIntervals);
            }

        }

        return result;

    }

    /**
     * Add multiple events on different tracks, but using the same request
     * interval
     * 
     * @param toAdd
     *            - HashMap, where keys represent the target track, and values
     *            contain a vector of events to be added
     * @param curInterval
     *            - Interval pointing to the intervalstore where the data is to
     *            be stored
     */
    public void addMultiple(HashMap<HEKPath, Vector<HEKEvent>> toAdd, Interval<Date> curInterval) {

        Iterator<HEKPath> keyIterator = toAdd.keySet().iterator();

        while (keyIterator.hasNext()) {

            HEKPath key = keyIterator.next();
            // Log.info("Adding " + key + " > " + toAdd.get(key));
            addToTrack(key, curInterval, toAdd.get(key));

        }
    }

    /**
     * Check whether the cache contains the given track
     * 
     * @param path
     *            - track to be checked for
     * @return - true if the track exists
     */
    public boolean containsTrack(HEKPath path) {
        return this.tracks.containsKey(path);
    }

    /**
     * Return all pathes to the tracks stored
     * 
     * @return - a set of tracks available
     */
    public Set<HEKPath> getTrackPaths() {
        Set<HEKPath> result = this.getTracks().keySet();

        // fix the back references
        for (HEKPath path : result) {
            path.setObject(tracks.get(path));
        }

        return result;
    }

    /**
     * The cache is empty, if no tracks exist
     * 
     * @return - true if no tracks exist
     */
    public boolean isEmpty() {
        return this.getTracks().isEmpty();
    }

    /**
     * Overwritten toString() method. Returns e.g. "[path1, path2, ...]".
     */
    public String toString() {

        String result = "";

        Iterator<HEKPath> keyIterator = getTracks().keySet().iterator();
        while (keyIterator.hasNext()) {

            HEKPath key = keyIterator.next();

            result = result + key.toString() + ", ";

        }

        return result;

    }

    public HEKCacheTreeModel getTreeModel() {
        return treeModel;
    }

    public HEKCacheSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public HEKCacheLoadingModel getLoadingModel() {
        return loadingModel;
    }

    public HEKCacheExpansionModel getExpansionModel() {
        return expansionModel;
    }

}
