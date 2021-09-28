package org.helioviewer.jhv.base.interval;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.time.TimeUtils;

public class Interval implements Comparable<Interval> {

    public final long start;
    public final long end;
    private final int hash;

    public Interval(long _start, long _end) {
        if (_end < _start)
            throw new IllegalArgumentException("End cannot be earlier than start");
        start = _start;
        end = _end;
        hash = computeHash(start, end);
    }

    private static int computeHash(long a, long b) {
        int result = (int) (a ^ (a >>> 32));
        return 31 * result + (int) (b ^ (b >>> 32));
    }

    private boolean containsPointInclusive(long time) {
        return time >= start && time <= end;
    }

    @Nonnull
    public static List<Interval> splitInterval(Interval interval, int days) {
        List<Interval> intervals = new ArrayList<>();
        long startDate = interval.start;

        while (true) {
            long newStartDate = startDate + TimeUtils.DAY_IN_MILLIS * days;
            if (interval.containsPointInclusive(newStartDate)) {
                intervals.add(new Interval(startDate, newStartDate));
                startDate = newStartDate;
            } else {
                intervals.add(new Interval(startDate, interval.end));
                break;
            }
        }
        return intervals;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof Interval i)
            return start == i.start && end == i.end;
        return false;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "[" + start + ',' + end + ')';
    }

    @Override
    public int compareTo(@Nonnull Interval o) {
        if (start < o.start) {
            return -1;
        }
        if (start == o.start && end < o.end) {
            return -1;
        }
        if (start == o.start && end == o.end) {
            return 0;
        }
        return 1;
    }

}
