package org.helioviewer.jhv.plugins.eveplugin.draw;

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
    protected Range selectedRange;
    /** The current available range */
    protected Range availableRange;
    /** The label of the y-axis */
    private String label;
    /** The scaled selected range */
    protected Range scaledSelectedRange;
    /** The scaled available range */
    protected Range scaledAvailableRange;

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
    public YAxisElement(Range selectedRange, Range availableRange, String label, boolean isLogScale, long activationTime) {
        this.selectedRange = selectedRange;
        this.availableRange = availableRange;
        scaledSelectedRange = new Range(0, 1);
        scaledAvailableRange = new Range(0, 1);
        this.label = label;
        this.isLogScale = isLogScale;
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
        isLogScale = true;
        scaledSelectedRange = new Range(0, 1);
        scaledAvailableRange = new Range(0, 1);
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
        adaptScaledSelectedRange();
    }

    private void adaptScaledSelectedRange() {
        double availableMax = availableRange.max;
        double availableMin = availableRange.min;
        double selectedMax = selectedRange.max;
        double selectedMin = selectedRange.min;
        if (isLogScale) {
            availableMax = Math.log10(availableMax);
            availableMin = Math.log10(availableMin);
            selectedMax = Math.log10(selectedMax);
            selectedMin = Math.log10(selectedMin);
        }
        double diffAvailable = availableMax - availableMin;
        double diffScaledAvailable = scaledAvailableRange.max - scaledAvailableRange.min;

        double ratio = diffScaledAvailable / diffAvailable;

        double diffSelStartAvaiStart = selectedMin - availableMin;
        double diffSelEndAvailStart = selectedMax - availableMin;

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
                availableRange.setMax(newAvailableRange.max);
                availableRange.setMin(newAvailableRange.min);
                checkSelectedRange();
                adaptScaledAvailableRange();
            }
        } else {
            availableRange.setMax(newAvailableRange.max);
            availableRange.setMin(newAvailableRange.min);
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

        double selectedMax = selectedRange.max;
        double selectedMin = selectedRange.min;
        double availableMax = availableRange.max;
        double availableMin = availableRange.min;
        if (isLogScale) {
            selectedMax = Math.log10(selectedMax);
            selectedMin = Math.log10(selectedMin);
            availableMin = Math.log10(availableMin);
            availableMax = Math.log10(availableMax);
        }

        double diffSelectedRange = selectedMax - selectedMin;
        double diffScaledSelectedRange = scaledSelectedRange.max - scaledSelectedRange.min;

        double ratio = diffScaledSelectedRange / diffSelectedRange;

        double diffSelStartAvailStart = selectedMin - availableMin;
        double diffSelEndAvailEnd = availableMax - selectedMax;

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
     * Gets the maximum value.
     *
     * @return The maximum value
     */
    public double getMaxValue() {
        return selectedRange.max;
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
        double minAvailable = availableRange.min;
        double maxAvailable = availableRange.max;
        if (isLogScale) {
            minAvailable = Math.log10(minAvailable);
            maxAvailable = Math.log10(maxAvailable);
        }
        double diffScaledAvailable = scaledAvailableRange.max - scaledAvailableRange.min;
        double diffAvail = maxAvailable - minAvailable;

        double ratio = diffAvail / diffScaledAvailable;

        double diffScSelStartScAvaiStart = newScaledSelectedRange.min - scaledAvailableRange.min;
        double diffscSelEndScAvailStart = newScaledSelectedRange.max - scaledAvailableRange.min;

        double selectedStart = minAvailable + diffScSelStartScAvaiStart * ratio;
        double selectedEnd = minAvailable + diffscSelEndScAvailStart * ratio;
        if (isLogScale) {
            selectedRange = new Range(Math.pow(10, selectedStart), Math.pow(10, selectedEnd));
        } else {
            selectedRange = new Range(selectedStart, selectedEnd);
        }
        scaledSelectedRange = new Range(newScaledSelectedRange);
        fireSelectedRangeChanged();
    }

    protected void fireSelectedRangeChanged() {
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
