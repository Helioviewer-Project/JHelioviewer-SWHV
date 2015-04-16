package org.helioviewer.plugins.eveplugin.lines.data;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.Pair;
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

    private final double[] values = new double[MINUTES_PER_DAY];
    private final long[] dates = new long[MINUTES_PER_DAY];

    private final Range valueRange = new Range();
    private int posMin = -1;
    private int posMax = -1;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public EVEDataOfDay(final int year, final int month, final int dayOfMonth) {
        GregorianCalendar calendar = new GregorianCalendar(year, month, dayOfMonth);

        Arrays.fill(values, Double.NaN);
        for (int i = 0; i < values.length; i++) {
            dates[i] = calendar.getTime().getTime();
            calendar.add(Calendar.MINUTE, 1);
        }
    }

    /**
     * Sets the given value and updates the minimum and maximum values.
     *
     * @param value
     *            The new value.
     * */
    public void setValue(final int minuteOfDay, final double value, final long date) {
        values[minuteOfDay] = value;

        if (minuteOfDay == posMin) {
            recomputeMinMax();
        } else if (valueRange.setMin(value)) {
            posMin = minuteOfDay;
        }

        if (minuteOfDay == posMax) {
            recomputeMinMax();
        } else if (valueRange.setMax(value)) {
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
            final double value = values[i];

            if (valueRange.setMin(value)) {
                posMin = i;
            }
            if (valueRange.setMax(value)) {
                posMax = i;
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
    public Pair<long[], double[]> getValuesInInterval(final Interval<Date> interval) {
        Date dateFirst = new Date(dates[0]);
        Date dateLast = new Date(dates[MINUTES_PER_DAY - 1]);

        if (dateLast.compareTo(interval.getStart()) < 0) {
            return new Pair(Arrays.copyOfRange(dates, MINUTES_PER_DAY - 3, MINUTES_PER_DAY - 1), Arrays.copyOfRange(values, MINUTES_PER_DAY - 3, MINUTES_PER_DAY - 1));
        }

        if (dateFirst.compareTo(interval.getEnd()) > 0) {
            return new Pair(Arrays.copyOfRange(dates, 0, 2), Arrays.copyOfRange(values, 0, 2));
        }

        int indexFrom = 0;
        int indexTo = MINUTES_PER_DAY - 1;
        GregorianCalendar calendar = new GregorianCalendar();
        final Interval<Date> available = new Interval<Date>(dateFirst, dateLast);

        if (available.containsPointInclusive(interval.getStart())) {
            calendar.setTime(interval.getStart());
            indexFrom = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            indexFrom = Math.max(0, indexFrom - 3);
        }

        if (available.containsPointInclusive(interval.getEnd())) {
            calendar.setTime(interval.getEnd());
            indexTo = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            indexTo = Math.min(indexTo + 3, MINUTES_PER_DAY - 1);
        }

        return new Pair(Arrays.copyOfRange(dates, indexFrom, indexTo), Arrays.copyOfRange(values, indexFrom, indexTo));
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
        Date dateFirst = new Date(dates[0]);
        Date dateLast = new Date(dates[MINUTES_PER_DAY - 1]);

        if (dateLast.compareTo(interval.getStart()) < 0 || dateFirst.compareTo(interval.getEnd()) > 0) {
            return new Range();
        }

        int indexFrom = 0;
        int indexTo = MINUTES_PER_DAY - 1;
        GregorianCalendar calendar = new GregorianCalendar();
        final Interval<Date> available = new Interval<Date>(dateFirst, dateLast);

        if (available.containsPointInclusive(interval.getStart())) {
            calendar.setTime(interval.getStart());
            indexFrom = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        }

        if (available.containsPointInclusive(interval.getEnd())) {
            calendar.setTime(interval.getEnd());
            indexTo = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        }

        if (indexFrom == 0 && indexTo == MINUTES_PER_DAY - 1) {
            return new Range(valueRange.min, valueRange.max);
        }

        final Range range = new Range();
        for (int i = indexFrom; i <= indexTo; ++i) {
            range.setMinMax(values[i]);
        }

        return range;
    }

    public void fillResult(EVEValues result) {
        result.addValues(dates, values);
    }

}
