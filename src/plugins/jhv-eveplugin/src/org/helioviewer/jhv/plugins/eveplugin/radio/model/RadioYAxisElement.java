/**
 *
 */
package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import java.awt.Color;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.plugins.eveplugin.base.Range;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;

/**
 * Radio specific implementation of y-axis element. This implementation
 * overrides the getSelectedRange() and getAvailableRange() in order to use the
 * YValueModel to get the correct values.
 *
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class RadioYAxisElement extends YAxisElement {

    /** Instance of the y-value model manager. */

    /**
     * Default constructor.
     *
     */
    public RadioYAxisElement() {
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
     * @param minValue
     *            The minimum value of the y-axis element
     * @param maxValue
     *            The maximum value of the y-axis element
     * @param color
     *            The color of the y-axis element
     * @param plotIdentifier
     *            The plot identifier for this radio y-axis element
     */
    public RadioYAxisElement(Range selectedRange, Range availableRange, String label, double minValue, double maxValue, Color color, long activationTime) {
        super(selectedRange, availableRange, label, minValue, maxValue, color, false, activationTime);
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
    public void setScaledSelectedRange(Range newScaledSelectedRange) {
        double diffScaledAvailable = scaledAvailableRange.max - scaledAvailableRange.min;
        double diffAvail = availableRange.max - availableRange.min;

        double ratio = diffAvail / diffScaledAvailable;

        double diffScSelStartScAvaiStart = newScaledSelectedRange.min - scaledAvailableRange.min;
        double diffscSelEndScAvailStart = newScaledSelectedRange.max - scaledAvailableRange.min;

        double selectedEnd = availableRange.max - diffScSelStartScAvaiStart * ratio;
        double selectedStart = availableRange.max - diffscSelEndScAvailStart * ratio;

        selectedRange = new Range(selectedStart, selectedEnd);

        Log.debug("setScaledSelectedRange old: " + scaledSelectedRange);
        scaledSelectedRange = new Range(newScaledSelectedRange);
        Log.debug("setScaledSelectedRange new: " + scaledSelectedRange);
        fireSelectedRangeChanged();
    }
}
