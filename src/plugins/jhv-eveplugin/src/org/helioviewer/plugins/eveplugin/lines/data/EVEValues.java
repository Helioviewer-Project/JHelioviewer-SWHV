package org.helioviewer.plugins.eveplugin.lines.data;

import java.util.Arrays;
import java.util.Date;

import org.helioviewer.base.math.Interval;

public class EVEValues {

    private int index = 0;
    private final int MINUTES_PER_DAY = 1440;

    public long[] dates = new long[MINUTES_PER_DAY];
    public double[] values = new double[MINUTES_PER_DAY];

    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;

    public void addValues(final long[] indates, final double[] invalues) {
        if (index + indates.length >= dates.length) {
            dates = Arrays.copyOf(dates, index + indates.length + MINUTES_PER_DAY);
            values = Arrays.copyOf(values, index + indates.length + MINUTES_PER_DAY);
        }

        for (int i = 0; i < indates.length; i++) {
            double value = invalues[i];
            if (!Double.isNaN(value)) {
                values[index] = value;
                dates[index] = indates[i];
                index++;

                minValue = value < minValue ? value : minValue;
                maxValue = value > maxValue ? value : maxValue;
            }
        }
    }

    public int getNumberOfValues() {
        return index;
    }

    public double getMinimumValue() {
        return minValue;
    }

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
