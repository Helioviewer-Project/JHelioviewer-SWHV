package org.helioviewer.plugins.eveplugin.lines.data;

import java.util.ArrayList;
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

    ArrayList<Long> dates = new ArrayList<Long>();
    ArrayList<Double> values = new ArrayList<Double>();
    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public void addValue(final long date, final double value) {
        values.add(value);
        dates.add(date);

        if (Double.isNaN(value))
            return;

        minValue = value < minValue ? value : minValue;
        maxValue = value > maxValue ? value : maxValue;
    }

    public Pair<ArrayList<Long>, ArrayList<Double>> getValues() {
        return new Pair<ArrayList<Long>, ArrayList<Double>>(dates, values);
    }

    public int getNumberOfValues() {
        return values.size();
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
        if (values.size() == 0) {
            return new Interval<Date>(null, null);
        }

        return new Interval<Date>(new Date(dates.get(0)), new Date(dates.get(dates.size() - 1)));
    }
}
