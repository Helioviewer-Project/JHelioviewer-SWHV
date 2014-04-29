package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.event.TreeModelListener;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.math.IntervalContainer;
import org.helioviewer.base.math.IntervalStore;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKSettings;

/**
 * This is a Model for the HEKCache
 * 
 * In addition to the state of the HEKCache object, it stores more state
 * information, like e.g.
 * 
 * <li>The interval currently used to display data</li>
 * 
 * <li>A structure of events available for download</li>
 * 
 * <li>The currently selected Categories/Events/...</li>
 * 
 * <li>The current point in time that is used to retrieve events</li>
 * 
 * @author Malte Nuhn
 */
public class HEKCacheModel {

    private HEKCache cache;

    private Interval<Date> curInterval;

    private Vector<HEKCacheListener> cacheModelListeners = new Vector<HEKCacheListener>();

    public HEKCacheModel(HEKCache cache) {
        this.cache = cache;

        // is this a wise initialization?
        this.curInterval = new Interval<Date>(new Date(), new Date());
    }

    /**
     * To which cache does this model belong?
     * 
     * @return HEKCache
     */
    public HEKCache getCache() {
        return cache;
    }

    /**
     * Set the interval for which events should be downloaded or for which
     * events available should be displayed Since this is a method that changes
     * the state of the model, it is protected, and only accessible from the
     * controller
     * 
     * @param curInterval
     */
    protected void setCurInterval(Interval<Date> curInterval) {
        cache.lockWrite();
        try {
            this.curInterval = curInterval;
        } finally {
            cache.unlockWrite();
        }
    }

    /**
     * Get the current interval for which the events are/will be downloaded or
     * for which events available are displayed
     * 
     * @return
     */
    public Interval<Date> getCurInterval() {
        // TODO: Malte Nuhn - Implement the CacheModelLOCK
        return curInterval;
    }

    /**
     * This method checks whether the given path is available in the given
     * duration
     * 
     * @param path
     *            - Path to check
     * @param duration
     *            - Interval in which the path is asked to be valid
     */
    private boolean pathValidInInterval(HEKPath path, Interval<Date> duration, boolean overlapmode) {
        // TODO: What to do with partial tracks
        // if (this.downloadablePaths.contains(path))
        // return true;

        // recursively calculate duration of virutal paths
        if (path.isVirtual()) {

            Vector<HEKPath> children = getChildren(path, overlapmode);

            for (HEKPath child : children) {
                if (pathValidInInterval(child, duration, overlapmode)) {
                    return true;
                }
            }

            return false;

            // An event is only valid if it is stored in an interval overlapping
            // with the interval in question
        } else if (path.getObject() instanceof HEKEvent) {

            if (path.getParent().getObject() instanceof IntervalStore<?, ?>) {

                @SuppressWarnings("unchecked")
                IntervalStore<Date, ?> intervalCache = (IntervalStore<Date, ?>) path.getParent().getObject();
                Vector<Interval<Date>> intervals = (Vector<Interval<Date>>) intervalCache.getIntervals();

                for (Interval<Date> interval : intervals) {

                    if (interval.containsInclusive(duration))

                        return true;

                }

            } else {

                assert (false); // should not happen

            }

            return false;

        } else if (path.getObject() instanceof IntervalStore<?, ?>) {

            @SuppressWarnings("unchecked")
            IntervalStore<Date, ?> intervalStore = (IntervalStore<Date, ?>) path.getObject();
            Vector<Interval<Date>> intervals = (Vector<Interval<Date>>) intervalStore.getIntervals();

            for (Interval<Date> interval : intervals) {
                if (interval.containsInclusive(duration))
                    return true;
            }

        }

        return false;
    }

    /**
     * Helper method that removes those events from the given list of events,
     * that do are not valid during the given interval
     * 
     * @param toFilter
     *            - result candidates
     * @param timeRange
     *            - interval in which the final events have to be validA
     * 
     * @see org.helioviewer.jhv.plugins.overlay.hek.cache.HEKCacheModel#filter
     */
    private void filterChildren(Vector<HEKPath> toFilter, Interval<Date> timeRange, boolean overlapmode) {
        // make sure we do not display events that do not exist in the current
        // interval
        // make sure we do not display any events if we do not know the whole
        // interval
        Iterator<HEKPath> toFilterIterator = toFilter.iterator();
        while (toFilterIterator.hasNext()) {
            HEKPath key = toFilterIterator.next();
            if (!pathValidInInterval(key, timeRange, overlapmode)) {
                toFilterIterator.remove();
            }
        }
    }

    /**
     * Check the given iterator of Paths for children of the given parentPath
     * and add valid children to the given resultList if they contain the given
     * timeRange
     * 
     * @param resultList
     *            - list to which possible children should be added
     * @param parentPath
     *            - parent path
     * @param possibleChildrenIterator
     *            - iterator of paths that might contain children
     * @param timeRange
     *            - time range that children need to contain
     * @return
     */
    private void getChildren(Vector<HEKPath> resultList, HEKPath parentPath, Iterator<HEKPath> possibleChildrenIterator, Interval<Date> timeRange, boolean overlapmode) {

        // loop over the children in question
        while (possibleChildrenIterator.hasNext()) {

            HEKPath possibleChildrenPath = possibleChildrenIterator.next();

            // skip here, because we do not want false virtual paths (see below)
            if (possibleChildrenPath.getObject() instanceof IntervalStore) {
                @SuppressWarnings("unchecked")
                IntervalStore<Date, HEKEvent> is = (IntervalStore<Date, HEKEvent>) possibleChildrenPath.getObject();

                if (!is.contains(timeRange)) {
                    if (!overlapmode) {
                        continue;
                    } else {
                        if (is.getCoveringIntervals(timeRange).size() == 0) {
                            continue;
                        }
                    }
                }

            }

            // check whether the child in question is a subpath of the given
            // parent path
            if (possibleChildrenPath.isSubPathOf(parentPath)) {

                // if it is not a direct child
                if (!possibleChildrenPath.isSubPathOf(parentPath, 1, true)) {

                    // find the next child on the way to the final destination
                    HEKPath sub = possibleChildrenPath.truncate(parentPath.getDepth() + 1);

                    // and set it virtual if it does not exist in the cache
                    if (!cache.getTrackPaths().contains(sub)) {
                        sub.setVirtual(true); // this is a virtual path
                    }

                    // add it to result list if it does not already exist
                    if (!resultList.contains(sub)) {
                        resultList.add(sub);
                    }

                    // and if it is a direct child...
                } else {

                    // add that to the list if not already existing
                    if (!resultList.contains(possibleChildrenPath)) {
                        resultList.add(possibleChildrenPath);
                    }

                }

            }

        }

    }

    /**
     * This method returns all Events that are stored inside a specific path
     * <p>
     * This is needed, since the events are stored in a different way than the
     * individual virtual paths.
     * <p>
     * Note that the resulting events may belong to different intervals inside a
     * common track.
     * 
     * @param path
     * @return
     */
    private Vector<HEKPath> getChildrenEvents(HEKPath path, Interval<Date> timeRange, boolean overlapmode) {

        Vector<HEKPath> result = new Vector<HEKPath>();

        // this only works if the path is known to the cache
        if (cache.getTrack(path) != null) {

            // get the track used
            IntervalStore<Date, HEKEvent> intervalStore = cache.getTrack(path);
            IntervalContainer<Date, HEKEvent> container = intervalStore.getItem(timeRange);

            // get the overlaping container if we are in overlapmode
            if (container == null && overlapmode) {
                Vector<Interval<Date>> overlappingIntervals = intervalStore.getCoveringIntervals(timeRange);
                if (overlappingIntervals.size() > 0) {
                    container = intervalStore.getItem(overlappingIntervals.get(0));
                }
            }

            Vector<HEKEvent> currentEvents = container.getItems();

            // and finally loop over all events in this interval
            for (HEKEvent evt : currentEvents) {
                if (timeRange.overlapsInclusive(evt.getDuration())) {
                    HEKPath eventPath = new HEKPath(path, evt.toString(), evt);
                    eventPath.setVirtual(false);
                    result.add(eventPath);
                }
            }

        }

        return result;

    }

    /**
     * Remove all {@link IntervalContainer} that are marked as partial
     */
    public void clearPartial() {
        cache.lockWrite();

        try {
            for (IntervalStore<Date, HEKEvent> is : cache.getTracks().values()) {
                for (Interval<Date> currentInterval : is.getIntervals()) {
                    IntervalContainer<Date, HEKEvent> ic = is.getItem(currentInterval);
                    // remove if partial
                    if (ic.isPartial()) {
                        Log.info("Removing old Partial " + currentInterval);
                        is.removeInterval(currentInterval);
                    }
                }
            }
        } finally {
            cache.unlockWrite();
        }
    }

    /**
     * Return all possible children of the given path.
     * <p>
     * This queries
     * <li>Events stored in the cache</li>
     * <li>Event categories that are known to be downloadable</li>
     * 
     * @param path
     *            - path in which the children have to be stored in
     * @return
     */
    public Vector<HEKPath> getChildren(HEKPath path, boolean overlapmode) {

        Vector<HEKPath> result = new Vector<HEKPath>();
        cache.lockRead();
        try {
            // find all paths that are subdirectories of "path"
            getChildren(result, path, cache.getTrackPaths().iterator(), this.getCurInterval(), overlapmode);
            // do not show events in the tree
            // result.addAll(getChildrenEvents(path));
            Collections.sort(result, HEKPath.LASTPARTCOMPARATOR);

            if (this.getCurInterval().isValid()) {
                filterChildren(result, this.getCurInterval(), overlapmode);
            }

            // if the given path is the root, make sure that we add a valid
            // pointer to the HEK root instead
            if (path.getDepth() == 0) {
                HEKPath pathToHEK = (new HEKPath(new HEKPath(cache), "HEK"));
                if (!result.contains(pathToHEK)) {
                    result.add(pathToHEK);
                }
            }

        } finally {
            cache.unlockRead();
        }

        return result;
    }

    /**
     * Return all events that are (not necessarily direct) children of the given
     * path
     * 
     * @param path
     *            - path in which the children have to be stored in
     * @return
     */
    public Vector<HEKPath> getChildrenEventsRecursive(HEKPath path, Interval<Date> timeRange, boolean overlapmode) {
        Vector<HEKPath> childrenEvents = new Vector<HEKPath>();
        Vector<HEKPath> childrenPaths = new Vector<HEKPath>();

        cache.lockRead();

        try {
            // get all childrenPaths
            getChildren(childrenPaths, path, cache.getTrackPaths().iterator(), timeRange, overlapmode);
            // add all chhildren events from the given path
            childrenEvents.addAll(getChildrenEvents(path, timeRange, overlapmode));
            // recursively add these children again
            for (HEKPath child : childrenPaths) {
                childrenEvents.addAll(getChildrenEventsRecursive(child, timeRange, overlapmode));
            }
        } finally {
            cache.unlockRead();
        }

        return childrenEvents;

    }

    /**
     * Get all events stored in the cache, that last in the given interval
     * 
     * @param interval
     * @return - all events stored in the cache, that last in the given interval
     */
    public Vector<HEKEvent> getEventsIn(Interval<Date> interval) {

        cache.lockRead();

        try {
            Vector<HEKEvent> result = new Vector<HEKEvent>();

            Iterator<Entry<HEKPath, IntervalStore<Date, HEKEvent>>> iter = cache.getTracks().entrySet().iterator();

            while (iter.hasNext()) {
                IntervalStore<Date, HEKEvent> intervalCache = iter.next().getValue();
                for (Interval<Date> hek : intervalCache.getIntervals()) {
                    for (HEKEvent evts : intervalCache.getItem(hek).getItems()) {
                        Interval<Date> duration = evts.getDuration();
                        if (duration != null && duration.overlapsInclusive(interval)) {
                            result.add(evts);
                        }
                    }
                }
            }

            return result;

        } finally {
            cache.unlockRead();
        }

    }

    /**
     * Requests all events (loaded) that are currently active. It uses the
     * model's current position state and the selectionModel to query which
     * events are active.
     * <p>
     * Note: This method does not check whether those events are also visible
     * from earth.
     * <p>
     * The duration of events is artificially expanded by
     * {@link HEKSettings#MODEL_EXPAND_DURATION}
     * 
     * @see #setCurPosition(Date)
     * @see HEKSettings
     * @see HEKCacheSelectionModel
     * @return All currently active events
     */
    public Vector<HEKEvent> getActiveEvents(Date curPos) {

        // Log.info("Get Active Events " + curPos);

        Vector<HEKEvent> result = new Vector<HEKEvent>();

        cache.lockRead();
        try {

            if (curPos != null) {
                // setup the current view interval
                // which is [ NOW - EXPAND_DURATION, NOW + EXPAND_DURATION ]
                Date curPosStart = new Date(curPos.getTime() - HEKSettings.MODEL_EXPAND_DURATION); // need
                Date curPosEnd = new Date(curPos.getTime() + HEKSettings.MODEL_EXPAND_DURATION); // need
                Interval<Date> drawInterval = new Interval<Date>(curPosStart, curPosEnd);
                Vector<HEKEvent> activeEvents = getEventsIn(drawInterval);
                // works IN PLACE
                cache.getSelectionModel().filterSelectedEvents(activeEvents);
                result.addAll(activeEvents);
            }
        } finally {
            cache.unlockRead();
        }
        return result;
    }

    /**
     * Store the given Paths - available in the given interval - in the cache.
     * For each Path of the given Paths, the counter of downloadable events is
     * increased. Thus, if the API returns e.g. 10 event available for download
     * in a path "P", the list should contain this path 10 times.
     * 
     * @param availablePaths
     *            - list of available paths
     * @param curInterval
     *            - interval for which the given paths are valid
     */
    public void feedStructure(Vector<HEKPath> availablePaths, Interval<Date> curInterval) {

        cache.lockWrite();

        try {

            for (HEKPath trackPath : availablePaths) {

                if (!cache.containsTrack(trackPath)) {
                    // this is important, since the downloadable Paths are NOT
                    // virtual, because we actually need to be able to select
                    // them!
                    trackPath.setVirtual(false);
                    cache.addTrack(trackPath);
                } else {
                }

                // get the intervalstore
                IntervalStore<Date, HEKEvent> intervalStore = cache.getTrack(trackPath);
                IntervalContainer<Date, HEKEvent> intervalContainer = null;

                // get a covering interval
                Vector<Interval<Date>> coveringIntervals = intervalStore.getCoveringIntervals(curInterval);
                if (coveringIntervals.size() != 0) {
                    // overwrite curInterval
                    curInterval = coveringIntervals.get(0);
                    // this intervalcontainer is NOW partial, since we did not
                    // download the events
                    intervalContainer = intervalStore.getItem(curInterval);
                    if (!intervalContainer.isPartial()) {
                        // we already know everything about this interval - no
                        // new information
                        continue;
                    } else {
                    }
                }

                // if we still could not find any container for the given
                // interval, create one
                if (intervalContainer == null) {
                    intervalContainer = new IntervalContainer<Date, HEKEvent>();
                    Vector<Interval<Date>> overlapping = intervalStore.getOverlappingIntervals(curInterval);
                    for (Interval<Date> overlapInterval : overlapping) {
                        intervalStore.removeInterval(overlapInterval);
                    }
                    intervalContainer.setPartial(true);
                    intervalStore.add(curInterval, intervalContainer);
                }

                if (intervalContainer.isPartial()) {
                    intervalStore.getItem(curInterval).incDownloadableEvents();
                } else {
                    // ignore if the current container is not partial
                }

            }

        } finally {
            cache.unlockWrite();
        }

    }

    /**
     * Store the given Events - available in the given interval - in the cache.
     * TODO Do not explicitly pass the eventpaths, use
     * {@link HEKEvent#getPath()} instead
     * 
     * @param eventsToFeed
     *            - hashmap describing location and event
     * @param curInterval
     *            - interval for which the given paths are valid
     */
    protected Vector<HEKPath> feedEvents(HashMap<HEKPath, HEKEvent> eventsToFeed, Interval<Date> curInterval) {

        // parse all events
        HashMap<HEKPath, Vector<HEKEvent>> toAdd = new HashMap<HEKPath, Vector<HEKEvent>>();

        for (HEKPath eventPath : eventsToFeed.keySet()) {
            HEKEvent event = eventsToFeed.get(eventPath);
            HEKPath trackPath = eventPath.getParent();
            eventPath.setVirtual(false);

            // if we do not yet have that track, create it
            if (!toAdd.containsKey(trackPath)) {
                toAdd.put(trackPath, new Vector<HEKEvent>());
            }

            toAdd.get(trackPath).add(event);

        }

        // Log.info("feedEvents");

        // Finally add the tracks to the cache
        // toAdd: Keys = TARGET TRACKS, Values = VECTOR OF EVENTS

        cache.lockWrite();
        try {
            cache.addMultiple(toAdd, curInterval);
        } finally {
            cache.unlockWrite();
        }

        return new Vector<HEKPath>(toAdd.keySet());

    }

    public synchronized void addCacheListener(HEKCacheListener listener) {
        // cache.lockWrite();
        try {
            cacheModelListeners.add(listener);
        } finally {
            // cache.unlockWrite();
        }
    }

    public synchronized void removeHEKCacheListener(TreeModelListener listener) {
        // cache.lockWrite();
        try {
            cacheModelListeners.remove(listener);
        } finally {
            // cache.unlockWrite();
        }
    }

    protected synchronized void fireCacheStateChanged() {
        // cache.lockRead();
        try {
            for (HEKCacheListener h : cacheModelListeners) {
                h.cacheStateChanged();
            }
        } finally {
            // cache.unlockRead();
        }
    }

    protected synchronized void fireStructureChanged(HEKPath path) {
        // cache.lockRead();
        try {
            for (HEKCacheListener h : cacheModelListeners) {
                h.structureChanged(path);
            }
        } finally {
            // cache.unlockRead();
        }
    }

    protected synchronized void fireEventsChanged(HEKPath path) {
        // cache.lockRead();
        try {
            for (HEKCacheListener h : cacheModelListeners) {
                h.eventsChanged(path);
            }
        } finally {
            // cache.unlockRead();
        }
    }

    /**
     * The root of the tree is represented by this special path, pointing to the
     * cache itself
     */
    public HEKPath getRoot() {
        return new HEKPath(cache);
    }

    public int getDownloadableChildrenEventsRecursive(HEKPath path, Interval<Date> timeRange, boolean overlapmode) {
        cache.lockRead();
        try {
            int result = 0;
            Vector<HEKPath> childrenPaths = new Vector<HEKPath>();

            // get all childrenPaths
            getChildren(childrenPaths, path, cache.getTrackPaths().iterator(), timeRange, overlapmode);

            // this only works if the path is known to the cache
            if (cache.getTrack(path) != null) {
                IntervalStore<Date, HEKEvent> intervalStore = cache.getTrack(path);
                IntervalContainer<Date, HEKEvent> intervalContainer = intervalStore.getItem(timeRange);
                if (overlapmode) {
                    Vector<Interval<Date>> overlappingIntervals = intervalStore.getCoveringIntervals(timeRange);
                    if (overlappingIntervals.size() > 0) {
                        intervalContainer = intervalStore.getItem(overlappingIntervals.get(0));
                    }
                }

                result += intervalContainer.getDownloadableEvents();
            }

            // recursively add these children again
            for (HEKPath child : childrenPaths) {
                result += getDownloadableChildrenEventsRecursive(child, timeRange, overlapmode);
            }

            return result;
        } finally {
            cache.unlockRead();
        }
    }

}
