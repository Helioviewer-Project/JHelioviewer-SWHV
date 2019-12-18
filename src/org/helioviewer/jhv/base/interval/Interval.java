package org.helioviewer.jhv.base.interval;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import org.helioviewer.jhv.time.TimeUtils;

public class Interval implements Comparable<Interval> {

    public final long start;
    public final long end;

    public Interval(long _start, long _end) {
        if (_end < _start)
            throw new IllegalArgumentException("End cannot be earlier than start");
        start = _start;
        end = _end;
    }

    private boolean containsPointInclusive(long time) {
        return time >= start && time <= end;
    }

    public static ArrayList<Interval> splitInterval(Interval interval, int days) {
        ArrayList<Interval> intervals = new ArrayList<>();
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
        if (!(o instanceof Interval))
            return false;
        Interval s = (Interval) o;
        return start == s.start && end == s.end;
    }

    @Override
    public int hashCode() {
        int result = (int) (start ^ (start >>> 32));
        return 31 * result + (int) (end ^ (end >>> 32));
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
