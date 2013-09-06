package org.helioviewer.base.math;

/**
 * This Interface defines methods needed for camparison of intervals.
 * 
 * TODO Malte Nuhn - Why do JavaDocs write intervals always as '[A,B)'
 * (==half-open), if generic methods exist for all types of intervals?
 * 
 * @author Malte Nuhn
 * 
 * @param <TimeFormat>
 */
public interface IntervalComparison<TimeFormat extends Comparable<TimeFormat>> extends Comparable<Interval<TimeFormat>> {

    /**
     * Check whether both intervals overlap.
     * <p>
     * Edge-Cases:
     * <li>[A,B) [B,C) do not overlap</li>
     * <li>[A,B) [A,B) do not overlap</li>
     * <p>
     * 
     * @param other
     * @return true if intervals overlap
     */
    public boolean overlaps(Interval<TimeFormat> other);

    /**
     * Check whether both intervals overlap.
     * <p>
     * Edge-Cases:
     * <li>[A,B) [B,C) do overlap</li>
     * <li>[A,B) [A,B) do overlap</li>
     * <p>
     * 
     * @param other
     * @return true if intervals overlap
     */
    public boolean overlapsInclusive(Interval<TimeFormat> other);

    /**
     * Check whether the given interval is contained in current interval.
     * <p>
     * Edge-Cases:
     * <li>[A,B) does not contain [A,B)</li>
     * <li>[A,B) contains [A,B-eps)</li>
     * <li>[A,B) contains [A+eps,B-eps)</li>
     * <p>
     * 
     * @param other
     * @return true if the given interval is contained in the current interval
     */
    public boolean contains(Interval<TimeFormat> other);

    /**
     * Check whether the given interval is contained in current interval.
     * <p>
     * Edge-Cases:<br>
     * <li>[A,B) does not contain [A,B)</li>
     * <li>[A,B) does not contain [A,B-eps)</li>
     * <li>[A,B) contains [A+eps,B-eps)</li>
     * <p>
     * 
     * @param other
     * @return true if the given interval is contained in the current interval
     */
    public boolean containsFully(Interval<TimeFormat> other);

    /**
     * Check whether the given interval is contained in current interval.
     * <p>
     * Edge-Cases:
     * <li>[A,B) contains [A,B)</li>
     * <li>[A,B) contains [A,B-eps)</li>
     * <li>[A,B) contains [A+eps,B-eps)</li>
     * <p>
     * 
     * @param other
     * @return true if the given interval is contained in the current interval
     */
    public boolean containsInclusive(Interval<TimeFormat> other);

    /**
     * Check whether the given point is contained in current interval.
     * <p>
     * Edge-Cases:
     * <li>[A,B) contains A</li>
     * <li>[A,B) does not contain B</li>
     * <p>
     * 
     * @param other
     * @return true if the given interval is contained in the current interval
     */
    public boolean containsPoint(TimeFormat other);

    /**
     * Check whether the given point is contained in the current interval.
     * <p>
     * Edge-Cases:
     * <li>[A,B) does not contain A</li>
     * <li>[A,B) does not contain B</li>
     * <p>
     * 
     * @param other
     * @return true if the given point is contained in the current interval
     */
    public boolean containsPointFully(TimeFormat other);

    /**
     * Check whether the given point is contained in current interval.
     * <p>
     * Edge-Cases:
     * <li>[A,B) contains A</li>
     * <li>[A,B) contains B</li>
     * <p>
     * 
     * @param other
     * @return true if the given point is contained in the current interval
     */
    public boolean containsPointInclusive(TimeFormat other);

}
