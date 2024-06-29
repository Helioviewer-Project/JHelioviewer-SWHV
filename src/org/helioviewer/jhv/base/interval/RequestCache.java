package org.helioviewer.jhv.base.interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestCache {

    private List<Interval> cache = new ArrayList<>();

    public List<Interval> adaptRequestCache(long start, long end) {
        List<Interval> missingIntervals = new ArrayList<>();
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

    public void removeRequestedInterval(long start, long end) {
        cache = remove(cache, new Interval(start, end));
    }

    public List<Interval> getAllRequestIntervals() {
        return cache;
    }

    public List<Interval> getMissingIntervals(long start, long end) {
        List<Interval> localCache = getInvertedCache(cache);
        localCache = remove(localCache, new Interval(Long.MIN_VALUE, start));
        localCache = remove(localCache, new Interval(end, Long.MAX_VALUE));
        return localCache;
    }

    private void updateRequestCache(long start, long end) {
        cache.add(new Interval(start, end));
        cache = merge(cache);
    }

    private static List<Interval> getInvertedCache(List<Interval> toInvert) {
        List<Interval> newCache = new ArrayList<>();
        int len = toInvert.size();
        if (len == 0) {
            newCache.add(new Interval(Long.MIN_VALUE, Long.MAX_VALUE));
            return newCache;
        }

        Interval interval = toInvert.getFirst();
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

    private static List<Interval> merge(List<Interval> intervals) {
        int size = intervals.size(); // cannot be null
        if (size <= 1)
            return intervals;

        Collections.sort(intervals);

        List<Interval> result = new ArrayList<>();
        Interval prev = intervals.getFirst();
        for (int i = 1; i < size; i++) {
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

    private static List<Interval> remove(List<Interval> cache, Interval ri) {
        List<Interval> icache = getInvertedCache(cache);
        icache.add(ri);
        icache = merge(icache);
        return getInvertedCache(icache);
    }

}
