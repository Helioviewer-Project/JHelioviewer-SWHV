package org.helioviewer.jhv.base.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;

public class RequestCache {

    private ArrayList<Interval> cache = new ArrayList<>();

    public List<Interval> adaptRequestCache(long start, long end) {
        ArrayList<Interval> missingIntervals = new ArrayList<>();
        if (cache.isEmpty()) {
            Interval interval = new Interval(start, end);
            missingIntervals.add(interval);
            cache.add(interval);
        } else {
            missingIntervals = getMissingIntervals(start, end);
            updateRequestCache(start, end);
        }
        return missingIntervals;
    }

    private void updateRequestCache(long start, long end) {
        cache.add(new Interval(start, end));
        cache = merge(cache);
    }

    public void removeRequestedInterval(long start, long end) {
        cache = remove(cache, new Interval(start, end));
    }

    public ArrayList<Interval> getAllRequestIntervals() {
        return cache;
    }

    public ArrayList<Interval> getMissingIntervals(long start, long end) {
        ArrayList<Interval> localCache = getInvertedCache(cache);
        localCache = remove(localCache, new Interval(Long.MIN_VALUE, start));
        localCache = remove(localCache, new Interval(end, Long.MAX_VALUE));
        return localCache;
    }

    private static ArrayList<Interval> getInvertedCache(ArrayList<Interval> toInvert) {
        ArrayList<Interval> newCache = new ArrayList<>();
        int len = toInvert.size();
        if (len == 0) {
            newCache.add(new Interval(Long.MIN_VALUE, Long.MAX_VALUE));
            return newCache;
        }

        Interval interval = toInvert.get(0);
        if (Long.MIN_VALUE != interval.start) {
            newCache.add(new Interval(Long.MIN_VALUE, interval.start));
        }

        long currend = interval.end;
        int i = 1;
        while (i < len) {
            interval = toInvert.get(i);
            newCache.add(new Interval(currend, interval.start));
            currend = interval.end;
            i++;
        }

        if (currend != Long.MAX_VALUE) {
            newCache.add(new Interval(currend, Long.MAX_VALUE));
        }
        return newCache;
    }

    private static ArrayList<Interval> merge(ArrayList<Interval> intervals) {
        if (intervals.size() <= 1) // cannot be null
            return intervals;

        Collections.sort(intervals);

        ArrayList<Interval> result = new ArrayList<>();
        Interval prev = intervals.get(0);
        for (int i = 1; i < intervals.size(); i++) {
            Interval curr = intervals.get(i);
            if (prev.end >= curr.start) {
                prev = new Interval(prev.start, Math.max(prev.end, curr.end));
            } else {
                result.add(prev);
                prev = curr;
            }
        }
        result.add(prev);
        return result;
    }

    private static ArrayList<Interval> remove(ArrayList<Interval> cache, Interval ri) {
        ArrayList<Interval> icache = getInvertedCache(cache);
        icache.add(ri);
        icache = merge(icache);
        return getInvertedCache(icache);
    }

}
