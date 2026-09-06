package org.helioviewer.jhv.time;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public record Interval(long start, long end) implements Comparable<Interval> {

    public Interval {
        if (end < start)
            throw new IllegalArgumentException("End cannot be earlier than start");
    }

    @Nonnull
    public static List<Interval> splitInterval(Interval interval, int days) {
        if (days <= 0)
            throw new RuntimeException("Attempt to call splitInterval() with negative or 0 days: " + days);

        List<Interval> intervals = new ArrayList<>();
        long chunkMillis = TimeUtils.DAY_IN_MILLIS * days;
        long cursor = interval.start;
        while (cursor < interval.end) {
            long next = Math.min(cursor + chunkMillis, interval.end);
            intervals.add(new Interval(cursor, next));
            cursor = next;
        }
        return intervals;
    }

    // Merges overlapping or touching intervals, preserving gaps. Sorts the input list when needed.
    // For zero or one interval, returns the input list itself.
    public static List<Interval> merge(List<Interval> intervals) {
        int size = intervals.size();
        if (size <= 1)
            return intervals;

        intervals.sort(null);

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

    @Override
    public int compareTo(@Nonnull Interval o) {
        int cmp = Long.compare(start, o.start);
        return cmp != 0 ? cmp : Long.compare(end, o.end);
    }

}
