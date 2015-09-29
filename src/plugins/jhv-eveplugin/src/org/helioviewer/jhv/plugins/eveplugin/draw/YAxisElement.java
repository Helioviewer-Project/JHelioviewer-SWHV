package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.Color;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.plugins.eveplugin.base.Range;

/**
 * This class describes an Y-axis.
 *
 * @author Bram.Bourgoignie@oma.be
 */

public class YAxisElement extends AbstractValueSpace {
    public enum YAxisLocation {
        LEFT, RIGHT;
    }

    /** The current selected range */
    private Range selectedRange;
    /** The current available range */
    private Range availableRange;
    /** The label of the y-axis */
    private String label;
    /** The scaled selected range */
    private Range scaledSelectedRange;
    /** The scaled available range */
    private Range scaledAvailableRange;

    /** The minimum value of the y-axis */
    private double minValue;
    /** The maximum value o the y-axis */
    private double maxValue;
    /**  */

    private Color color;

    private boolean isLogScale;

    private YAxisLocation location;

    /**
     * Creates a Y-axis element with a selected value range, an available value
     * range, a label, a minimum value, a maximum value and a color.
     *
     * @param selectedRange
     *            The current selected value range
     * @param availableRange
     *            The current available value range
     * @param label
     *            The label of the y axis element
     * @param minValue
     *            The minimum value of this y-axis element
     * @param maxValue
     *            The maximum value of this y-axis element
     * @param color
     *            The color of this this y-axis element
     */
    public YAxisElement(Range selectedRange, Range availableRange, String label, double minValue, double maxValue, Color color, boolean isLogScale, long activationTime) {
        this.selectedRange = selectedRange;
        this.availableRange = availableRange;
        scaledSelectedRange = new Range(0, 1);
        scaledAvailableRange = new Range(0, 1);
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.color = color;
        this.isLogScale = isLogScale;
        checkMinMax();
    }

    /**
     * Creates a default Y-axis element with a selected range (0,0), available
     * range (0,0), empty label, minimum and maximum value of 0.0 and a black
     * color.
     *
     */
    public YAxisElement() {
        selectedRange = new Range();
        availableRange = new Range();
        label = "";
        minValue = 0.0;
        maxValue = 0.0;
        color = Color.BLACK;
        isLogScale = true;
        scaledSelectedRange = new Range(0, 1);
        scaledAvailableRange = new Range(0, 1);
    }

    private void checkMinMax() {
        if (minValue == 0.0 && maxValue == 0.0) {
            minValue = -1.0;
            maxValue = 1.0;
        } else if (minValue == maxValue) {
            minValue = minValue - minValue / 10;
            maxValue = maxValue + maxValue / 10;
        }
    }

    /**
     * Gives the selected range.
     *
     * @return The selected range
     */
    public Range getSelectedRange() {
        return selectedRange;
    }

    /**
     * Sets the selected range.
     *
     * @param selectedRange
     *            The selected range
     */
    public void setSelectedRange(Range selectedRange) {
        Log.debug("Set selected range old selected range: [" + Math.log10(this.selectedRange.min) + ", " + Math.log10(this.selectedRange.max) + "]; new selected range: [" + Math.log10(selectedRange.min) + ", " + Math.log10(selectedRange.max) + "]");
        this.selectedRange = selectedRange;
        adaptScaledSelectedRange();
    }

    private void adaptScaledSelectedRange() {
        double diffAvailable = availableRange.max - availableRange.min;
        double diffScaledAvailable = scaledAvailableRange.max - scaledAvailableRange.min;

        double ratio = diffScaledAvailable / diffAvailable;

        double diffSelStartAvaiStart = selectedRange.min - availableRange.min;
        double diffSelEndAvailStart = selectedRange.max - availableRange.min;

        double scaledSelectedStart = scaledAvailableRange.min + ratio * diffSelStartAvaiStart;
        double scaledSelectedEnd = scaledAvailableRange.min + ratio * diffSelEndAvailStart;

        scaledSelectedRange = new Range(scaledSelectedStart, scaledSelectedEnd);
    }

    /**
     * Gets the available range.
     *
     * @return The available range
     */
    public Range getAvailableRange() {
        return availableRange;
    }

    /**
     * Sets the available range.
     *
     * @param availableRange
     *            The available range
     */
    public void setAvailableRange(Range newAvailableRange) {
        Range dummyRange = new Range();
        if (availableRange.min != dummyRange.min && availableRange.max != dummyRange.max) {
            if (availableRange.min != newAvailableRange.min || availableRange.max != newAvailableRange.max) {
                availableRange = new Range(newAvailableRange);
                checkSelectedRange();
                adaptScaledAvailableRange();
            }
        } else {
            availableRange = new Range(newAvailableRange);
            selectedRange = new Range(newAvailableRange);
            scaledAvailableRange = new Range(0, 1);
            scaledSelectedRange = new Range(0, 1);
        }
    }

    private void checkSelectedRange() {
        if (selectedRange.min < availableRange.min || selectedRange.max > availableRange.max || selectedRange.min > selectedRange.max) {
            selectedRange = new Range(availableRange);
        }
    }

    private void adaptScaledAvailableRange() {
        double diffSelectedRange = selectedRange.max - selectedRange.min;
        double diffScaledSelectedRange = scaledSelectedRange.max - scaledSelectedRange.min;

        double ratio = diffScaledSelectedRange / diffSelectedRange;

        double diffSelStartAvailStart = selectedRange.min - availableRange.min;
        double diffSelEndAvailEnd = availableRange.max - selectedRange.max;

        double scaledAvailableStart = scaledSelectedRange.min - diffSelStartAvailStart * ratio;
        double scaledAvailableEnd = scaledSelectedRange.max + diffSelEndAvailEnd * ratio;

        scaledAvailableRange = new Range(scaledAvailableStart, scaledAvailableEnd);

    }

    /**
     * Gets the label.
     *
     * @return The label
     */

    public String getLabel() {
        String localLabel = label.replace("^2", "\u00B2");
        return localLabel;
    }

    public String getOriginalLabel() {
        return label;
    }

    /**
     * Sets the label.
     *
     * @param label
     *            The label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the minimum value.
     *
     * @return The minimum value
     */
    public double getMinValue() {
        return selectedRange.min;
    }

    /**
     * Sets the minimum value.
     *
     * @param minValue
     *            The minimum value
     */
    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    /**
     * Gets the maximum value.
     *
     * @return The maximum value
     */
    public double getMaxValue() {
        return selectedRange.max;
    }

    /**
     * Sets the maximum value.
     *
     * @param maxValue
     *            The maximum value
     */
    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Gets the color.
     *
     * @return The color.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the color.
     *
     * @param color
     *            The color.
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Sets the available range, selected range, label minimum value, maximum
     * value and color of the y-axis element.
     *
     * @param label
     *            The label
     */
    public void set(String label, boolean isLogScale) {
        this.label = label;
        this.isLogScale = isLogScale;
        checkMinMax();
    }

    public boolean isLogScale() {
        return isLogScale;
    }

    public void setIsLogScale(boolean isLogScale) {
        this.isLogScale = isLogScale;

    }

    public YAxisLocation getLocation() {
        return location;
    }

    public void setLocation(YAxisLocation location) {
        this.location = location;
    }

    @Override
    public Range getScaledSelectedRange() {
        return scaledSelectedRange;
    }

    @Override
    public Range getScaledAvailableRange() {
        return scaledAvailableRange;
    }

    @Override
    public void setScaledSelectedRange(Range newScaledSelectedRange) {
        double diffScaledAvailable = scaledAvailableRange.max - scaledAvailableRange.min;
        double diffAvail = availableRange.max - availableRange.min;

        double ratio = diffAvail / diffScaledAvailable;

        double diffScSelStartScAvaiStart = newScaledSelectedRange.min - scaledAvailableRange.min;
        double diffscSelEndScAvailStart = newScaledSelectedRange.max - scaledAvailableRange.min;

        double selectedStart = availableRange.min + diffScSelStartScAvaiStart * ratio;
        double selectedEnd = availableRange.min + diffscSelEndScAvailStart * ratio;

        selectedRange = new Range(selectedStart, selectedEnd);
        scaledSelectedRange = new Range(newScaledSelectedRange);

        fireSelectedRangeChanged();
    }

    private void fireSelectedRangeChanged() {
        for (ValueSpaceListener vsl : listeners) {
            vsl.valueSpaceChanged(availableRange, selectedRange);
        }
    }

    public void reset() {
        availableRange = new Range();
        selectedRange = new Range();
        scaledAvailableRange = new Range(0, 1);
        scaledSelectedRange = new Range(0, 1);
    }
}
