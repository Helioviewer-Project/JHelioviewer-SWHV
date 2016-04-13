package org.helioviewer.jhv.base.interval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Interval implements Comparable<Interval> {

    public Date start;

    public Date end;

    private Interval(Interval other) {
        this.start = other.start;
        this.end = other.end;
    }

    public Interval(Date start, Date end) {
        this.start = start;
        this.end = end;
        if (start == null || end == null) {
            Thread.dumpStack();
            System.exit(1);
        }
    }

    public boolean contains(Interval other) {
        return this.containsPoint(other.start) && this.containsPoint(other.end);
    }

    public boolean containsFully(Interval other) {
        return this.containsPointFully(other.start) && this.containsPointFully(other.end);
    }

    public boolean containsInclusive(Interval other) {
        return this.containsPointInclusive(other.start) && this.containsPointInclusive(other.end);
    }

    public boolean containsPoint(Date time) {
        // start inclusive, end exclusive!
        assert start.compareTo(end) <= 0;
        return (time.compareTo(this.start) >= 0) && (time.compareTo(this.end) < 0);
        // return (time >= this.start) && (time < this.end);
    }

    public boolean containsPointFully(Date time) {
        assert start.compareTo(end) <= 0;
        return (time.compareTo(this.start) > 0) && (time.compareTo(this.end) < 0);
    }

    public boolean containsPointInclusive(Date time) {
        assert start.compareTo(end) <= 0;
        return (time.compareTo(this.start) >= 0) && (time.compareTo(this.end) <= 0);
    }

    public boolean overlaps(Interval other) {
        return (this.containsPoint(other.start) || this.containsPoint(other.end)) || (other.containsPoint(this.start) || other.containsPoint(this.end));
    }

    public boolean overlapsInclusive(Interval other) {
        return (this.containsPointInclusive(other.start) || this.containsPointInclusive(other.end)) || (other.containsPointInclusive(this.start) || other.containsPointInclusive(this.end));
    }

    /**
     * Returns the value that is as close as possible to the given value, but
     * still contained in the interval. If the given value is outside the
     * interval, the the interval's closest 'edge' is returned.
     */
    public Date squeeze(Date value) {
        if (this.containsPointInclusive(value)) {
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
        return this.start.compareTo(other.start);
    }

    /**
     * Returns the intersection of this and the given interval.
     * <p>
     * If both intervals do not overlap the current interval will be returned.
     * If the current interval is not valid, the given interval is returned.
     *
     * @param other
     *            the interval to get the intersection with current one
     * @return the intersected interval, or in special cases, the given or the
     *         current interval
     * */
    public Interval intersectInterval(Interval other) {
        Interval result = new Interval(this);
        if (!this.isValid()) {
            result = new Interval(other);
        } else if (this.overlaps(other)) {
            if (this.start.compareTo(other.start) < 0) {
                result.start = other.start;
            }
            if (this.end.compareTo(other.end) > 0) {
                result.end = other.end;
            }
        }
        return result;
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
            return (this.start.equals(s.start) && (this.end.equals(s.end)));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (this.start.toString() + " - " + this.end.toString()).hashCode();
    }

    private boolean isValid() {
        if (this.start.compareTo(this.end) > 0) {
            return false;
        }
        return true;
    }

}
