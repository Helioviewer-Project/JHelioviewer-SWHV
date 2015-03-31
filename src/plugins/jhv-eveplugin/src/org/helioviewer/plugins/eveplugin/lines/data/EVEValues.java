package org.helioviewer.plugins.eveplugin.lines.data;

import java.util.Arrays;
import java.util.Date;

import org.helioviewer.base.Pair;
import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.download.DownloadedData;

/**
 *
 * @author Stephan Pagel
 * */
public class EVEValues implements DownloadedData {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    public long[] dates = new long[0];
    public double[] values = new double[0];
    private int index = 0;
    private final int increment = 1440;

    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public void addValue(final long date, final double value) {
        if (index == dates.length) {
            dates = Arrays.copyOf(dates, dates.length + increment);
            values = Arrays.copyOf(values, values.length + increment);
        }

        values[index] = value;
        dates[index] = date;

        index++;

        if (Double.isNaN(value))
            return;

        minValue = value < minValue ? value : minValue;
        maxValue = value > maxValue ? value : maxValue;
    }

    public int getNumberOfValues() {
        return index;
    }

    @Override
    public double getMinimumValue() {
        return minValue;
    }

    @Override
    public double getMaximumValue() {
        return maxValue;
    }

    public Interval<Date> getInterval() {
        if (index == 0) {
            return new Interval<Date>(null, null);
        }

        return new Interval<Date>(new Date(dates[0]), new Date(dates[index - 1]));
    }
}
