package org.helioviewer.jhv.base.interval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Interval implements Comparable<Interval> {

    public final Date start;
    public final Date end;

    public Interval(Date _start, Date _end) {
        start = _start;
        end = _end;
        if (start == null || end == null) {
            Thread.dumpStack();
            System.exit(1);
        }
    }

    public boolean containsInclusive(Interval other) {
        return containsPointInclusive(other.start) && containsPointInclusive(other.end);
    }

    public boolean containsPointInclusive(Date time) {
        assert start.compareTo(end) <= 0;
        return time.compareTo(start) >= 0 && time.compareTo(end) <= 0;
    }

    public boolean overlapsInclusive(Interval other) {
        return containsPointInclusive(other.start) || containsPointInclusive(other.end) || other.containsPointInclusive(start) || other.containsPointInclusive(end);
    }

    /**
     * Returns the value that is as close as possible to the given value, but
     * still contained in the interval. If the given value is outside the
     * interval, the the interval's closest 'edge' is returned.
     */
    public Date squeeze(Date value) {
        if (containsPointInclusive(value)) {
            return value;
        } else if (value.compareTo(start) < 0) {
            return start;
        } else if (value.compareTo(end) > 0) {
            return end;
        }
        // this case should never occur
        assert false;
        return value;
    }

    @Override
    public String toString() {
        return "[" + start + "," + end + ")";
    }

    @Override
    public int compareTo(Interval other) {
        return start.compareTo(other.start);
    }

    public static ArrayList<Interval> splitInterval(final Interval interval, int days) {
        final ArrayList<Interval> intervals = new ArrayList<Interval>();

        final Calendar calendar = new GregorianCalendar();
        Date startDate = interval.start;

        while (true) {
            calendar.clear();
            calendar.setTime(startDate);
            calendar.add(Calendar.DAY_OF_MONTH, days);

            final Date newStartDate = calendar.getTime();

            if (interval.containsPointInclusive(newStartDate)) {
                intervals.add(new Interval(startDate, calendar.getTime()));
                startDate = newStartDate;
            } else {
                intervals.add(new Interval(startDate, interval.end));
                break;
            }
        }

        return intervals;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Interval) {
            Interval s = (Interval) other;
            return start.equals(s.start) && end.equals(s.end);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (start.toString() + " - " + end.toString()).hashCode();
    }

}
