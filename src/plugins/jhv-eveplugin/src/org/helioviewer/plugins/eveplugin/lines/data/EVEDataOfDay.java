package org.helioviewer.plugins.eveplugin.lines.data;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.base.Range;

/**
 * Manages the {@link EVEValue}s for one specific day. This class allows quick
 * access to the values.
 * 
 * @author Stephan Pagel
 * */
public class EVEDataOfDay {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private final int MINUTES_PER_DAY = 1440;

    private final EVEValue[] values = new EVEValue[MINUTES_PER_DAY];
    private final Range valueRange = new Range();
    private int posMin = -1;
    private int posMax = -1;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor. The represented day has to be defined.
     * 
     * @param year
     *            Year describing the day associated with the {@link EVEValue}s.
     * @param month
     *            Month describing the day associated with the {@link EVEValue}
     *            s.
     * @param dayOfMonth
     *            Day of month describing the day associated with the
     *            {@link EVEValue}s.
     * */
    public EVEDataOfDay(final int year, final int month, final int dayOfMonth) {
        final GregorianCalendar calendar = new GregorianCalendar(year, month, dayOfMonth);

        for (int i = 0; i < values.length; i++) {
            values[i] = new EVEValue(calendar.getTime(), null);
            calendar.add(Calendar.MINUTE, 1);
        }
    }

    /**
     * Sets the given value and updates the minimum and maximum values.
     * 
     * @param value
     *            The new value.
     * */
    public void setValue(final EVEValue value) {
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(value.date);

        final int minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        values[minuteOfDay].value = value.value;

        if (minuteOfDay == posMin) {
            recomputeMinMax();
        } else if (valueRange.setMin(value.value)) {
            posMin = minuteOfDay;
        }

        if (minuteOfDay == posMax) {
            recomputeMinMax();
        } else if (valueRange.setMax(value.value)) {
            posMax = minuteOfDay;
        }
    }

    /**
     * Recomputes the minimum and maximum value of all available values and
     * updates the position where these values are located.
     * */
    private void recomputeMinMax() {
        valueRange.reset();
        posMin = -1;
        posMax = -1;

        for (int i = 0; i < values.length; ++i) {
            if (values[i].value != null) {
                final double value = values[i].value;

                if (valueRange.setMin(value)) {
                    posMin = i;
                }

                if (valueRange.setMax(value)) {
                    posMax = i;
                }
            }
        }
    }

    /**
     * Returns all values within the given interval.
     * 
     * @param interval
     *            Only values of corresponding time stamps within the given
     *            interval will be considered.
     * 
     * @return Values within the given interval.
     * */
    public EVEValue[] getValuesInInterval(final Interval<Date> interval) {
        if (values[MINUTES_PER_DAY - 1].date.compareTo(interval.getStart()) < 0 || values[0].date.compareTo(interval.getEnd()) > 0) {
            return new EVEValue[0];
        }

        int indexFrom = 0;
        int indexTo = MINUTES_PER_DAY - 1;

        final Interval<Date> available = new Interval<Date>(values[0].date, values[MINUTES_PER_DAY - 1].date);

        if (available.containsPointInclusive(interval.getStart())) {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(interval.getStart());
            indexFrom = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            indexFrom = Math.max(0, indexFrom - 3);
        }

        if (available.containsPointInclusive(interval.getEnd())) {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(interval.getEnd());
            indexTo = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            indexTo = Math.min(indexTo + 3, MINUTES_PER_DAY - 1);
        }

        return Arrays.copyOfRange(values, indexFrom, indexTo);
    }

    /**
     * Returns the current minimum and maximum value of all available values.
     * 
     * @return The minimum and maximum value of all available values.
     * */
    public Range getMinMax() {
        return new Range(valueRange.min, valueRange.max);
    }

    /**
     * Returns the current minimum and maximum value of all available values
     * within the given interval.
     * 
     * @param interval
     *            Only values of corresponding time stamps within the given
     *            interval will be considered for minimum and maximum value.
     * 
     * @return The minimum and maximum value of all available values within the
     *         given interval.
     * */
    public Range getMinMaxInInterval(final Interval<Date> interval) {
        if (values[MINUTES_PER_DAY - 1].date.compareTo(interval.getStart()) < 0 || values[0].date.compareTo(interval.getEnd()) > 0) {
            return new Range();
        }

        int indexFrom = 0;
        int indexTo = MINUTES_PER_DAY - 1;

        final Interval<Date> available = new Interval<Date>(values[0].date, values[MINUTES_PER_DAY - 1].date);

        if (available.containsPointInclusive(interval.getStart())) {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(interval.getStart());
            indexFrom = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        }

        if (available.containsPointInclusive(interval.getEnd())) {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(interval.getEnd());
            indexTo = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        }

        if (indexFrom == 0 && indexTo == MINUTES_PER_DAY - 1) {
            return new Range(valueRange.min, valueRange.max);
        }

        final Range range = new Range();
        for (int i = indexFrom; i <= indexTo; ++i) {
            range.setMinMax(values[i].value);
        }

        return range;
    }
}
