/**
 * 
 */
package org.helioviewer.plugins.eveplugin.radio.model;

import java.awt.Color;

import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;

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
    private final YValueModel yValueModel;

    /**
     * Default constructor.
     * 
     */
    public RadioYAxisElement() {
        super();
        yValueModel = YValueModel.getSingletonInstance();
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
        yValueModel = YValueModel.getSingletonInstance();
    }

    @Override
    public Range getSelectedRange() {
        return new Range(yValueModel.getSelectedYMax(), yValueModel.getSelectedYMin());
    }

    @Override
    public Range getAvailableRange() {
        return new Range(yValueModel.getAvailableYMax(), yValueModel.getAvailableYMin());
    }

    @Override
    public double getMinValue() {
        return yValueModel.getSelectedYMax();
    }

    @Override
    public double getMaxValue() {
        return yValueModel.getSelectedYMin();
    }
}
