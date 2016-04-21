package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;

/**
 * Radio specific implementation of y-axis element. This implementation
 * overrides the getSelectedRange() and getAvailableRange() in order to use the
 * YValueModel to get the correct values.
 *
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class RadioYAxis extends YAxis {

    public RadioYAxis() {
        super();
    }

    /**
     * Creates a radio y-axis element based on the given selected range,
     * available range, label, minimum value, maximum value, a color and a plot
     * identifier.
     *
     * @param selectedRange
     *            The selected range of the radio y-axis element
     * @param availableRange
     *            The available range of the radio y-axis element
     * @param label
     *            The label corresponding with the radio y-axis element
     */
    public RadioYAxis(Range selectedRange, Range availableRange, String label, long activationTime) {
        super(selectedRange, availableRange, label, false, activationTime);
    }

    @Override
    public double getMinValue() {
        return selectedRange.max;
    }

    @Override
    public double getMaxValue() {
        return selectedRange.min;
    }

    @Override
    public void setSelectedRange(Range newScaledSelectedRange) {
        double diffScaledAvailable = availableRange.max - availableRange.min;
        double diffAvail = availableRange.max - availableRange.min;

        double ratio = diffAvail / diffScaledAvailable;

        double diffScSelStartScAvaiStart = newScaledSelectedRange.min - availableRange.min;
        double diffscSelEndScAvailStart = newScaledSelectedRange.max - availableRange.min;

        double selectedEnd = availableRange.max - diffScSelStartScAvaiStart * ratio;
        double selectedStart = availableRange.max - diffscSelEndScAvailStart * ratio;

        selectedRange = new Range(selectedStart, selectedEnd);

        selectedRange = new Range(newScaledSelectedRange);
        fireSelectedRangeChanged();
    }

    @Override
    public void shiftDownPixels(double distanceY, int height) {
        double scaledMin = scale(selectedRange.min);
        double scaledMax = scale(selectedRange.max);

        double ratioValue = (scaledMax - scaledMin) / height;
        double shift = distanceY * ratioValue;
        double startValue = scaledMin + shift;
        double endValue = scaledMax + shift;
        selectedRange.min = invScale(startValue);
        selectedRange.max = invScale(endValue);
        fireSelectedRangeChanged();
    }

    @Override
    public void zoomSelectedRange(double scrollValue, double relativeY, double height) {
        double scaledMin = scale(selectedRange.min);
        double scaledMax = scale(selectedRange.max);
        double scaled = scaledMin + (scaledMax - scaledMin) * (relativeY / height);
        double delta = scrollValue * ZOOMSTEP_PERCENTAGE;
        double newScaledMin = ((1 + delta) * scaledMin - delta * scaled);
        double newScaledMax = ((1 + delta) * scaledMax - delta * scaled);

        newScaledMin = Math.max(scale(availableRange.min), newScaledMin);
        newScaledMax = Math.min(scale(availableRange.max), newScaledMax);
        selectedRange.min = invScale(newScaledMin);
        selectedRange.max = invScale(newScaledMax);
        fireSelectedRangeChanged();
    }

}
