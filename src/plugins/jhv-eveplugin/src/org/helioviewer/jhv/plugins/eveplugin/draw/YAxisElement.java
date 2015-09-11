package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.Color;

import org.helioviewer.jhv.plugins.eveplugin.base.Range;

/**
 * This class describes an Y-axis.
 *
 * @author Bram.Bourgoignie@oma.be
 */

public class YAxisElement {
    public enum YAxisLocation {
        LEFT, RIGHT;
    }

    /** The current selected range */
    private Range selectedRange;
    /** The current available range */
    private Range availableRange;
    /** The label of the y-axis */
    private String label;
    /** The minimum value of the y-axis */
    private double minValue;
    /** The maximum value o the y-axis */
    private double maxValue;
    /**  */
    private Color color;

    private boolean isLogScale;

    private long activationTime;

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
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.color = color;
        this.isLogScale = isLogScale;
        checkMinMax();
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
        activationTime = System.currentTimeMillis();
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
        this.selectedRange = selectedRange;
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
    public void setAvailableRange(Range availableRange) {
        this.availableRange = availableRange;
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
        return minValue;
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
        return maxValue;
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
     * @param availableRange
     *            The available range
     * @param selectedRange
     *            The selected range
     * @param label
     *            The label
     * @param minValue
     *            The minimum value
     * @param maxValue
     *            The maximum value
     * @param color
     *            The color
     */
    public void set(Range availableRange, Range selectedRange, String label, double minValue, double maxValue, Color color, boolean isLogScale, long activationTime) {
        this.availableRange = availableRange;
        this.selectedRange = selectedRange;
        this.label = label;
        this.maxValue = maxValue;
        this.minValue = minValue;
        this.color = color;
        this.isLogScale = isLogScale;
        this.activationTime = activationTime;
        checkMinMax();
    }

    public boolean isLogScale() {
        return isLogScale;
    }

    public void setIsLogScale(boolean isLogScale) {
        this.isLogScale = isLogScale;

    }

    public long getActivationTime() {
        return activationTime;
    }

    public void setActivationTime(long activationTime) {
        this.activationTime = activationTime;
    }

    public YAxisLocation getLocation() {
        return location;
    }

    public void setLocation(YAxisLocation location) {
        this.location = location;
    }
}
