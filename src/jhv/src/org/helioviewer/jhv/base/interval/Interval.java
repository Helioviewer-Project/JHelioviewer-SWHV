package org.helioviewer.jhv.base.interval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Interval<TimeFormat extends Comparable<TimeFormat>> implements IntervalComparison<TimeFormat> {

    private TimeFormat start;

    private TimeFormat end;

    private Interval(Interval<TimeFormat> other) {
        this.start = other.start;
        this.end = other.end;
    }

    public Interval(TimeFormat start, TimeFormat end) {
        this.start = start;
        this.end = end;
    }

    public TimeFormat getStart() {
        return start;
    }

    public TimeFormat getEnd() {
        return end;
    }

    @Override
    public boolean contains(Interval<TimeFormat> other) {
        return this.containsPoint(other.start) && this.containsPoint(other.end);
    }

    @Override
    public boolean containsFully(Interval<TimeFormat> other) {
        return this.containsPointFully(other.start) && this.containsPointFully(other.end);
    }

    @Override
    public boolean containsInclusive(Interval<TimeFormat> other) {
        return this.containsPointInclusive(other.start) && this.containsPointInclusive(other.end);
    }

    @Override
    public boolean containsPoint(TimeFormat time) {
        // start inclusive, end exclusive!
        assert start.compareTo(end) <= 0;
        return (time.compareTo(this.start) >= 0) && (time.compareTo(this.end) < 0);
        // return (time >= this.start) && (time < this.end);
    }

    @Override
    public boolean containsPointFully(TimeFormat time) {
        assert start.compareTo(end) <= 0;
        return (time.compareTo(this.start) > 0) && (time.compareTo(this.end) < 0);
    }

    @Override
    public boolean containsPointInclusive(TimeFormat time) {
        assert start.compareTo(end) <= 0;
        return (time.compareTo(this.start) >= 0) && (time.compareTo(this.end) <= 0);
    }

    @Override
    public boolean overlaps(Interval<TimeFormat> other) {
        return (this.containsPoint(other.start) || this.containsPoint(other.end)) || (other.containsPoint(this.start) || other.containsPoint(this.end));
    }

    @Override
    public boolean overlapsInclusive(Interval<TimeFormat> other) {
        return (this.containsPointInclusive(other.start) || this.containsPointInclusive(other.end)) || (other.containsPointInclusive(this.start) || other.containsPointInclusive(this.end));
    }

    /**
     * Returns the value that is as close as possible to the given value, but
     * still contained in the interval. If the given value is outside the
     * interval, the the interval's closest 'edge' is returned.
     */
    public TimeFormat squeeze(TimeFormat value) {
        if (this.containsPointInclusive(value)) {
            return value;
        } else if (value.compareTo(this.getStart()) < 0) {
            return this.getStart();
        } else if (value.compareTo(this.getEnd()) > 0) {
            return this.getEnd();
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
    public int compareTo(Interval<TimeFormat> other) {
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
    public Interval<TimeFormat> intersectInterval(Interval<TimeFormat> other) {
        Interval<TimeFormat> result = new Interval<TimeFormat>(this);
        if (!this.isValid()) {
            result = new Interval<TimeFormat>(other);
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

    public static ArrayList<Interval<Date>> splitInterval(final Interval<Date> interval, int days) {
        final ArrayList<Interval<Date>> intervals = new ArrayList<Interval<Date>>();

        if (interval.getStart() == null || interval.getEnd() == null) {
            return intervals;
        }

        final Calendar calendar = new GregorianCalendar();
        Date startDate = interval.getStart();

        while (true) {
            calendar.clear();
            calendar.setTime(startDate);
            calendar.add(Calendar.DAY_OF_MONTH, days);

            final Date newStartDate = calendar.getTime();

            if (interval.containsPointInclusive(newStartDate)) {
                intervals.add(new Interval<Date>(startDate, calendar.getTime()));
                startDate = newStartDate;
            } else {
                intervals.add(new Interval<Date>(startDate, interval.getEnd()));
                break;
            }
        }

        return intervals;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Interval<?>) {
            Interval<?> s = (Interval<?>) other;
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
