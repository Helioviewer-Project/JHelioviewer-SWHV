package org.helioviewer.base.interval;

import java.util.Iterator;
import java.util.Vector;

/**
 * A generic Interval class, used for any type of ranges. Different types of
 * functions are provided, allowing to use this class for open, closed and
 * half-closed intervals.
 * 
 * @author Malte Nuhn
 * @author Stephan Pagel
 * 
 */
public class Interval<TimeFormat extends Comparable<TimeFormat>> implements IntervalComparison<TimeFormat> {

    /**
     * Start of the interval
     */
    protected TimeFormat start;

    /**
     * End of the interval
     */
    protected TimeFormat end;

    /**
     * Copyconstructor. Does not deepcopy the start and end fields of the given
     * interval.
     * 
     * @param other
     *            - the interval that forms the blueprint of the interval to be
     *            created
     */
    public Interval(Interval<TimeFormat> other) {
        this.start = other.start;
        this.end = other.end;
    }

    /**
     * Construct a new Interval with the start and end time given. No checks are
     * performed on the given data.
     * 
     * @param start
     *            - Start of the interval
     * @param end
     *            - End of the interval
     */
    public Interval(TimeFormat start, TimeFormat end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Get the beginning of the interval.
     * 
     * @return the interval's start timestamp
     */
    public TimeFormat getStart() {
        return start;
    }

    /**
     * Get the end of the interval.
     * 
     * @return the interval's end timestamp
     */
    public TimeFormat getEnd() {
        return end;
    }

    /**
     * Set the beginning of the interval.
     * 
     * @param start
     */
    public void setStart(TimeFormat start) {
        this.start = start;
    }

    /**
     * Set the end of the interval.
     * 
     * @param end
     */
    public void setEnd(TimeFormat end) {
        this.end = end;
    }

    /**
     * @see org.helioviewer.base.interval.IntervalComparison#contains
     */
    public boolean contains(Interval<TimeFormat> other) {
        return this.containsPoint(other.start) && this.containsPoint(other.end);
    }

    /**
     * @see org.helioviewer.base.interval.IntervalComparison#containsFully
     */
    public boolean containsFully(Interval<TimeFormat> other) {
        return this.containsPointFully(other.start) && this.containsPointFully(other.end);
    }

    /**
     * @see org.helioviewer.base.interval.IntervalComparison#containsInclusive
     */
    public boolean containsInclusive(Interval<TimeFormat> other) {
        return this.containsPointInclusive(other.start) && this.containsPointInclusive(other.end);
    }

    /**
     * @see org.helioviewer.base.interval.IntervalComparison#containsPoint
     */
    public boolean containsPoint(TimeFormat time) {
        // start inclusive, end exclusive!
        assert start.compareTo(end) <= 0;
        return (time.compareTo(this.start) >= 0) && (time.compareTo(this.end) < 0);
        // return (time >= this.start) && (time < this.end);
    }

    /**
     * @see org.helioviewer.base.interval.IntervalComparison#containsPointFully
     */
    public boolean containsPointFully(TimeFormat time) {
        assert start.compareTo(end) <= 0;
        return (time.compareTo(this.start) > 0) && (time.compareTo(this.end) < 0);
    }

    /**
     * @see org.helioviewer.base.interval.IntervalComparison#containsPointInclusive
     */
    public boolean containsPointInclusive(TimeFormat time) {
        assert start.compareTo(end) <= 0;
        return (time.compareTo(this.start) >= 0) && (time.compareTo(this.end) <= 0);
    }

    /**
     * @see org.helioviewer.base.interval.IntervalComparison#overlaps
     */
    public boolean overlaps(Interval<TimeFormat> other) {
        return (this.containsPoint(other.start) || this.containsPoint(other.end)) || (other.containsPoint(this.start) || other.containsPoint(this.end));
    }

    /**
     * @see org.helioviewer.base.interval.IntervalComparison#overlapsInclusive
     */
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

    /**
     * Returns a String representation of the Interval, e.g. "[X,Y)"
     */
    public String toString() {
        return "[" + start + "," + end + ")";
    }

    /**
     * Overridden comepareTo method.
     * <p>
     * This method compares intervals by their start date, thus, sorting a list
     * of intervals using this compeareTo method results in a list, where those
     * intervals that start earliest are in the beginning of the list.
     * 
     */
    public int compareTo(Interval<TimeFormat> other) {
        return this.start.compareTo(other.start);
    }

    /**
     * Check whether the interval is degenerated, meaning that start and end
     * equal; or in other words, that the interval duration is zero.
     * <p>
     * No special treatment of null values is performed.
     * 
     * @return true if the interval is degenerated
     */
    public boolean isDegenerated() {
        return this.start.equals(this.end);
    }

    /**
     * Expand this interval as less as possible, but still making sure that the
     * expression
     * <p>
     * <code> this.expand(other).overlapsInclusive(other) == true </code>
     * <p>
     * is true. If both intervals do not overlap, no change occurs.
     * <p>
     * If the current interval is not valid, the given interval is returned
     * 
     * @param other
     *            - the interval to which the current interval should be
     *            extended to
     * @return the expanded interval
     */
    public Interval<TimeFormat> expand(Interval<TimeFormat> other) {
        Interval<TimeFormat> result = new Interval<TimeFormat>(this);
        // copy other if null
        if (!this.isValid()) {
            result = new Interval<TimeFormat>(other);
        } else if (this.overlapsInclusive(other)) {
            if (other.start.compareTo(this.start) < 0) {
                result.start = other.start;
                result.end = this.end;
            }
            if (other.end.compareTo(this.end) > 0) {
                result.start = this.start;
                result.end = other.end;
            }
        }
        return result;
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

    /**
     * Exclude the given interval from the current interval.
     * <p>
     * Since this can result in a non continuous range, multiple intervals may
     * be returned.
     * <p>
     * For each of the returned intervals (every possible value of "i"), the
     * following statements are true
     * <li>
     * <code>this.exclude(other).get(i).overlaps(other) == false</code></li>
     * <li>
     * <code>this.exclude(other).get(i).containsPoint(x) == true</code> for all
     * x with <code> this.containsPoint(x) && ! other.containsPoint(x) </code></li>
     * 
     * Note: Possibly degenerated Intervals are removed from the result set
     * 
     * @see Interval#isDegenerated()
     * 
     * @param other
     * @return result of the exclusion
     */
    public Vector<Interval<TimeFormat>> exclude(Interval<TimeFormat> other) {
        Vector<Interval<TimeFormat>> result = new Vector<Interval<TimeFormat>>();
        if (this.equals(other))
            return result;

        // this interval is contained in the interval to be excluded: return
        // empty list
        if (other.contains(this)) {
            return result;
        }
        // the interval to be excluded is included in this interval: this
        // results in two smaller intervals
        if (this.contains(other)) {
            result.add(new Interval<TimeFormat>(this.start, other.start));
            result.add(new Interval<TimeFormat>(other.end, this.end));
            removeDegenerated(result);
            return result;
        }
        // this interval only overlaps the interval to be excluded: make this
        // interval smaller
        if (this.overlaps(other)) {
            if (other.start.compareTo(this.start) < 0) {
                result.add(new Interval<TimeFormat>(other.end, this.end));
            } else {
                result.add(new Interval<TimeFormat>(this.start, other.start));
            }
            removeDegenerated(result);
            return result;
        }

        result.add(this);
        removeDegenerated(result);
        return result;
    }

    /**
     * Internal method to remove all degenerated Intervals from a noncontinuous
     * range of intervals.
     * 
     * @param toClean
     */
    private void removeDegenerated(Vector<Interval<TimeFormat>> toClean) {
        Iterator<Interval<TimeFormat>> intIterator = toClean.iterator();
        while (intIterator.hasNext()) {
            Interval<TimeFormat> curInterval = intIterator.next();
            if (curInterval.isDegenerated()) {
                intIterator.remove();
            }
        }
    }

    /**
     * Overridden equals method.
     * <p>
     * Two Intervals are equal, if start and end values are equal.
     */

    public boolean equals(Object other) {
        if (other instanceof Interval<?>) {
            Interval<?> s = (Interval<?>) other;
            return (this.start.equals(s.start) && (this.end.equals(s.end)));
        }
        return false;
    }

    /**
     * Overridden hashCode method.
     * <p>
     * This method makes sure that
     * <code> intervalA.hashCode() == intervalB.hashCode() </code> holds iff
     * <code> intervalA.equals(intervalB) </code>
     * <p>
     * 
     * @see org.helioviewer.base.interval.Interval#equals
     */

    public int hashCode() {
        return (this.start.toString() + " - " + this.end.toString()).hashCode();
    }

    /**
     * Check whether the current interval is valid, meaning that
     * <p>
     * <li>neither start nor end are null</li>
     * <li>that start is before or equal to end (using the compareTo method)</li>
     * 
     * @return true if the interval is valid
     */
    public boolean isValid() {
        if (this.start == null || this.end == null)
            return false;
        if (this.start.compareTo(this.end) > 0)
            return false;
        return true;
    }

}
