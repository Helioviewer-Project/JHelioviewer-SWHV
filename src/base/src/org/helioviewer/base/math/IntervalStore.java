package org.helioviewer.base.math;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

/**
 * Relationship: Interval <--> Items during that Interval
 * 
 */
public class IntervalStore<TimeFormat extends Comparable<TimeFormat>, ItemFormat extends IntervalComparison<TimeFormat>> {

    /**
     * The interval represents the Interval which returned the data
     */
    private HashMap<Interval<TimeFormat>, IntervalContainer<TimeFormat, ItemFormat>> data = new HashMap<Interval<TimeFormat>, IntervalContainer<TimeFormat, ItemFormat>>();

    /**
     * Empty Constructor
     */
    public IntervalStore() {

    }

    public IntervalStore(Interval<TimeFormat> interval) {
        data.put(interval, new IntervalContainer<TimeFormat, ItemFormat>());
    }

    /**
     * Constructor adding all of the array's intervals
     * 
     * @param intervals
     */

    // TODO: Malte Nuhn - For completeness implement constructor adding all of
    // the array's intervals

    /**
     * Copyconstructor, deepcopying the contained intervals, too
     * 
     * @param intervals
     */

    // TODO: Malte Nuhn - For completeness implement copyconstructor deepcopying
    // the contained intervals, too

    /**
     * Add/Merge the set of intervals given
     * 
     * @param data
     *            - intervals to be added/merged
     */
    public void add(HashMap<Interval<TimeFormat>, IntervalContainer<TimeFormat, ItemFormat>> data) {

        Iterator<Interval<TimeFormat>> iter = data.keySet().iterator();

        while (iter.hasNext()) {

            Interval<TimeFormat> curInterval = iter.next();
            this.add(curInterval, data.get(curInterval));

        }

    }

    /**
     * Add/Merge the interval given
     * 
     * @param newItems
     *            - intervals to be added/merged
     */
    public boolean add(Interval<TimeFormat> newInterval, IntervalContainer<TimeFormat, ItemFormat> newIntervalContainer) {
        boolean merged = false;
        int newItems = newIntervalContainer.getItems().size();

        // TODO WORK ON PARAMETERS OF THIS METHOD
        Vector<Interval<TimeFormat>> overlappingIntervals = this.getOverlappingIntervals(newInterval);

        // Melt new interval with all existing ones
        for (Interval<TimeFormat> overlappingInterval : overlappingIntervals) {
            newInterval = newInterval.expand(overlappingInterval);
            merged = true;

            // move items from old interval to the expanded one
            IntervalContainer<TimeFormat, ItemFormat> toAdd = this.data.get(overlappingInterval);
            newIntervalContainer.downloadableEvents += toAdd.getDownloadableEvents();

            for (ItemFormat item : toAdd.getItems()) {
                if (!newIntervalContainer.getItems().contains(item)) {
                    newIntervalContainer.getItems().add(item);
                } else {
                    // already contains
                }
            }

            // move over downloadable events

            this.data.remove(overlappingInterval);
        }

        // Log.info("Added "+ newItems + " new Items to container with " +
        // newIntervalContainer.downloadableEvents);
        newIntervalContainer.downloadableEvents -= newItems;

        // if we have no more downloadable events, the interval is not partial
        // anymore
        if (newItems > 0 && newIntervalContainer.downloadableEvents == 0) {
            newIntervalContainer.setPartial(false);
        }
        // Log.info("New Result of downloadable Paths is " +
        // newIntervalContainer.downloadableEvents);

        // TODO GETTERSETTER, COMMENT THAT DOWNLOADABLE ARE DECREASED
        // and finally store the new interval
        data.put(newInterval, newIntervalContainer);

        // loop to make sure that all events are registered to all possible
        // buckets
        Iterator<Interval<TimeFormat>> iter = this.data.keySet().iterator();

        while (iter.hasNext()) {

            Interval<TimeFormat> curInterval = iter.next();
            IntervalContainer<TimeFormat, ItemFormat> curContainer = this.data.get(curInterval);

            // this is currently not the interval we just created
            if (!curInterval.equals(newInterval)) {

                // loop over all items

                for (ItemFormat curItem : curContainer.getItems()) {

                    // if it overlaps the newInterval, add the item there, too
                    if (curItem.overlaps(newInterval) && !newIntervalContainer.getItems().contains(curItem)) {

                        newIntervalContainer.getItems().add(curItem);

                    }
                }

            }

        }

        return merged;

    }

    /**
     * Check whether any of the sets intervals overlaps the given interval
     * 
     * @param interval
     * @return list of overlapping intervals
     */
    public Vector<Interval<TimeFormat>> getOverlappingIntervals(Interval<TimeFormat> interval) {
        Vector<Interval<TimeFormat>> result = new Vector<Interval<TimeFormat>>();

        Iterator<Interval<TimeFormat>> iter = this.data.keySet().iterator();
        while (iter.hasNext()) {
            Interval<TimeFormat> key = iter.next();
            if (key.overlaps(interval) || key.equals(interval)) {
                result.add(key);
            }
        }

        return result;
    }

    /**
     * Check whether any of the sets intervals covers the given interval
     * 
     * @param interval
     * @return list of overlapping intervals
     */
    public Vector<Interval<TimeFormat>> getCoveringIntervals(Interval<TimeFormat> interval) {
        Vector<Interval<TimeFormat>> result = new Vector<Interval<TimeFormat>>();

        Iterator<Interval<TimeFormat>> iter = this.data.keySet().iterator();
        while (iter.hasNext()) {
            Interval<TimeFormat> key = iter.next();
            if (key.containsInclusive(interval) || key.equals(interval)) {
                result.add(key);
            }
        }

        return result;
    }

    /**
     * Return the set of intervals which is to be requested in order to have the
     * given interval cached, too
     * 
     * @param interval
     * @return intervals needed
     */
    public Vector<Interval<TimeFormat>> needed(Interval<TimeFormat> interval) {

        // linked list needed
        LinkedList<Interval<TimeFormat>> result = new LinkedList<Interval<TimeFormat>>();

        result.add(interval);

        // loop over the intervals in this cache
        Iterator<Interval<TimeFormat>> storeIntervalsIterator = this.data.keySet().iterator();
        while (storeIntervalsIterator.hasNext()) {
            Interval<TimeFormat> curStoreInterval = storeIntervalsIterator.next();
            IntervalContainer<TimeFormat, ItemFormat> curStoreContainer = this.data.get(curStoreInterval);

            // partially downloaded containers do not count
            if (curStoreContainer.isPartial()) {
                continue;
            }

            // loop over the intervals in the current result set

            // T O D O ? Iterator<SimpleInterval<TimeFormat>> checkIter =
            // checkInter.iterator();

            // iterate over the list -
            ListIterator<Interval<TimeFormat>> resultIntervalsIterator = result.listIterator();

            while (resultIntervalsIterator.hasNext()) {
                Interval<TimeFormat> curResultInterval = resultIntervalsIterator.next();

                // replace currently stored (overlapping) result interval by an
                // excluded one
                // we only remove those intervals that have already been loaded
                if (curStoreInterval.overlaps(curResultInterval) || curStoreInterval.equals(curResultInterval)) {
                    resultIntervalsIterator.remove(); // result.remove(checkInterval);
                    Vector<Interval<TimeFormat>> toAdd = curResultInterval.exclude(curStoreInterval);
                    for (Interval<TimeFormat> add : toAdd) {
                        resultIntervalsIterator.add(add);
                    }
                }

            }

        }

        // convert to vector
        Vector<Interval<TimeFormat>> resultVector = new Vector<Interval<TimeFormat>>();
        resultVector.addAll(result);
        return resultVector;
    }

    /**
     * Return the set of intervals which is to be requested in order to have the
     * given intervals(!) cached, too
     * 
     * The Items of the given Vector need to be pairwise non overlapping
     * 
     * @param requestIntervals
     * @return intervals needed
     */
    public Vector<Interval<TimeFormat>> needed(Vector<Interval<TimeFormat>> requestIntervals) {
        Vector<Interval<TimeFormat>> result = new Vector<Interval<TimeFormat>>();
        for (Interval<TimeFormat> requestInterval : requestIntervals) {
            Vector<Interval<TimeFormat>> neededIntervals = this.needed(requestInterval);
            result.addAll(neededIntervals);
        }
        return result;
    }

    /**
     * Return a String representation, e.g. "[[A,B),[C,D)]"
     */
    public String toString() {
        StringBuffer result = new StringBuffer("IntervalStore [ ");
        boolean added = false;

        // loop over the intervals in this cache
        Iterator<Interval<TimeFormat>> iter = this.data.keySet().iterator();
        while (iter.hasNext()) {
            Interval<TimeFormat> curInterval = iter.next();
            added = true;
            result.append(curInterval.toString());
            result.append(" : ");
            result.append((this.getItem(curInterval)));
            result.append(", ");
        }

        if (added)
            result.delete(result.length() - 1, result.length());

        result.append("]");

        return result.toString();
    }

    /**
     * Return the number of intervals in this set
     * 
     * @return number of intervals in the set
     */
    public int size() {
        return data.size();
    }

    /**
     * Check whether this set of intervals is equal to the given set of
     * intervals
     * 
     * @param other
     * @return true if both sets of intervals are equal
     */
    public boolean equals(IntervalStore<TimeFormat, ItemFormat> other) {
        if (this.data.size() != other.data.size())
            return false;
        for (int i = 0; i < this.data.size(); i++) {
            if (!this.data.get(i).equals(other.data.get(i)))
                return false;
        }
        return true;
    }

    /*
     * // REMOVE ALL DEGENERATED BUCKETS public void cleanup() {
     * Iterator<IntervalBucket<TimeFormat,ItemFormat>> iter =
     * intervals.iterator(); while (iter.hasNext()) { IntervalBucket<TimeFormat,
     * ItemFormat> i = iter.next(); if (i.getDuration().degenerated())
     * iter.remove(); } }
     */

    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    /**
     * interval addresses the requested interval, not the length of the event
     */
    public void addEvent(Interval<TimeFormat> interval, ItemFormat newEvent) {
        // System.out.println("Adding " + newEvent + " to " + interval);
        if (!this.data.containsKey(interval)) {
            this.data.put(interval, new IntervalContainer<TimeFormat, ItemFormat>());
        }

        IntervalContainer<TimeFormat, ItemFormat> curContainer = this.data.get(interval);

        // do not add the event if it is already in there
        if (newEvent != null && !curContainer.getItems().contains(newEvent)) {
            curContainer.getItems().add(newEvent);
        }

    }

    /**
     * Checks whether the given interval has already been requested and stored
     * 
     * @param request
     *            - EXACT interval to be asked for
     * @return true if the given interval has already been requested and stored
     */
    public boolean contains(Interval<TimeFormat> request) {
        return this.data.containsKey(request);
    }

    /**
     * Check whether the given item has been stored in the given request
     * interval
     * 
     * @param request
     *            - EXACT interval in which the event might have been stored
     * @param hek
     *            - event to be asked for
     * @return true if the given item has been stored in the given request
     *         interval
     */
    public boolean contains(Interval<TimeFormat> request, ItemFormat hek) {
        if (!this.data.containsKey(request))
            return false;
        return this.data.get(request).getItems().contains(hek);
    }

    /**
     * Check whether the given event is registered somewhere in this store
     * 
     * @param item
     *            - item to be checked
     * @return true if the given event is registered somewhere in this store
     */
    public boolean contains(ItemFormat item) {
        return findItem(item).size() > 0;
    }

    /**
     * Return all intervals in which the given event is registered
     * 
     * @param item
     *            - event to be asked for
     * @return - list of intervals
     */
    public Vector<Interval<TimeFormat>> findItem(ItemFormat item) {
        Vector<Interval<TimeFormat>> result = new Vector<Interval<TimeFormat>>();

        Iterator<Interval<TimeFormat>> iter = this.data.keySet().iterator();
        while (iter.hasNext()) {
            Interval<TimeFormat> interval = iter.next();

            if (this.data.get(interval).getItems().contains(item)) {
                result.add(interval);
            }

        }

        return result;

    }

    /**
     * Return the IntervalContainer for the given Interval - no overlap checks
     * are performed
     * 
     * @param interval
     * @return
     */
    public IntervalContainer<TimeFormat, ItemFormat> getItem(Interval<TimeFormat> interval) {
        return this.data.get(interval);
    }

    public Set<Interval<TimeFormat>> getKeys() {
        return this.data.keySet();
    }

    /**
     * This IntervalCache has a couple of Request Buckets!
     */
    public Vector<Interval<TimeFormat>> getIntervals() {
        Vector<Interval<TimeFormat>> result = new Vector<Interval<TimeFormat>>();
        Iterator<Interval<TimeFormat>> iter = this.data.keySet().iterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

    public void removeInterval(Interval<TimeFormat> interval) {
        this.data.remove(interval);
    }

}
