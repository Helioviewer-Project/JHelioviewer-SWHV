package org.helioviewer.plugins.eveplugin.lines.data;

import java.util.Date;
import java.util.LinkedList;

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

    private final LinkedList<EVEValue> values = new LinkedList<EVEValue>();
    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public void addValue(final EVEValue value) {
        values.add(value);

        if (Double.isNaN(value.value))
            return;

        minValue = value.value < minValue ? value.value : minValue;
        maxValue = value.value > maxValue ? value.value : maxValue;
    }

    public EVEValue[] getValues() {
        return values.toArray(new EVEValue[0]);
    }

    public int getNumberOfValues() {
        return values.size();
    }

    public double getMinimumValue() {
        return minValue;
    }

    public double getMaximumValue() {
        return maxValue;
    }

    public Interval<Date> getInterval() {
        if (values.size() == 0) {
            return new Interval<Date>(null, null);
        }

        return new Interval<Date>(new Date(values.getFirst().milli), new Date(values.getLast().milli));
    }

}
