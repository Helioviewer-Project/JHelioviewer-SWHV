package org.helioviewer.jhv.base.interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestCache {

    private List<Interval> cache = new ArrayList<>();

    public List<Interval> adaptRequestCache(long start, long end) {
        List<Interval> missingIntervals = getMissingIntervals(start, end);
        if (!missingIntervals.isEmpty())
            updateRequestCache(start, end);
        return missingIntervals;
    }

    public void removeRequestedInterval(long start, long end) {
        cache = subtract(cache, new Interval(start, end));
    }

    public List<Interval> getAllRequestIntervals() {
        return cache;
    }

    public List<Interval> getMissingIntervals(long start, long end) {
        Interval requested = new Interval(start, end);
        List<Interval> missing = new ArrayList<>();

        long cursor = requested.start();
        for (Interval interval : cache) {
            if (interval.end() <= cursor)
                continue;
            if (interval.start() >= requested.end())
                break;

            if (cursor < interval.start())
                missing.add(new Interval(cursor, interval.start()));
            cursor = interval.end();
            if (cursor >= requested.end())
                break;
        }
        if (cursor < requested.end()) {
            missing.add(new Interval(cursor, requested.end()));
        }
        return missing;
    }

    private void updateRequestCache(long start, long end) {
        cache.add(new Interval(start, end));
        cache = merge(cache);
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
            if (prev.end() >= curr.start()) {
                prev = new Interval(prev.start(), Math.max(prev.end(), curr.end()));
            } else {
                result.add(prev);
                prev = curr;
            }
        }
        result.add(prev);
        return result;
    }

    private static List<Interval> subtract(List<Interval> intervals, Interval removed) {
        List<Interval> result = new ArrayList<>(intervals.size());
        for (Interval interval : intervals) {
            if (interval.end() <= removed.start() || interval.start() >= removed.end()) {
                result.add(interval);
                continue;
            }
            if (interval.start() < removed.start()) {
                result.add(new Interval(interval.start(), removed.start()));
            }
            if (removed.end() < interval.end()) {
                result.add(new Interval(removed.end(), interval.end()));
            }
        }
        return result;
    }

}
