package org.helioviewer.jhv.time;

import java.util.ArrayList;
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
        cache = Interval.merge(cache);
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
