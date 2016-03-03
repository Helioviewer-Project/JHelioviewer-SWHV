package org.helioviewer.jhv.plugins.eveplugin.draw;

import org.helioviewer.jhv.base.Range;

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

    private YAxisLocation location;
    private YAxisElementScale scale;
    protected static final double ZOOMSTEP_PERCENTAGE = 0.02;

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
        this.label = label;
        setIsLogScale(isLogScale);
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
        setIsLogScale(true);
    }

    /**
     * Gives the selected range.
     *
     * @return The selected range
     */
    @Override
    public Range getSelectedRange() {
        return selectedRange;
    }

    /**
     * Sets the selected range.
     *
     * @param selectedRange
     *            The selected range
     */
    @Override
    public void setSelectedRange(Range selectedRange) {
        this.selectedRange = selectedRange;
        fireSelectedRangeChanged();
    }

    /**
     * Gets the available range.
     *
     * @return The available range
     */
    @Override
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
                availableRange.setMax(selectedRange.max);
                availableRange.setMin(selectedRange.min);
                checkSelectedRange();
            }
        } else {
            availableRange.setMax(newAvailableRange.max);
            availableRange.setMin(newAvailableRange.min);
            selectedRange = new Range(newAvailableRange);
        }
    }

    private void checkSelectedRange() {
        if (selectedRange.min < availableRange.min || selectedRange.max > availableRange.max || selectedRange.min > selectedRange.max) {
            selectedRange = new Range(availableRange);
        }
    }

    /**
     * Gets the label.
     *
     * @return The label
     */

    public String getLabel() {
        return scale.getLabel();
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

    public double getScaledMinValue() {
        return scale(selectedRange.min);
    }

    public double getScaledMaxValue() {
        return scale(selectedRange.max);
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
        setIsLogScale(isLogScale);
    }

    public void setIsLogScale(boolean isLogScale) {
        if (isLogScale) {
            scale = new YAxisElementLogScale(label);
        } else {
            scale = new YAxisElementIdentityScale(label);
        }
    }

    public YAxisLocation getLocation() {
        return location;
    }

    public void setLocation(YAxisLocation location) {
        this.location = location;
    }

    protected void fireSelectedRangeChanged() {
        for (ValueSpaceListener vsl : listeners) {
            vsl.valueSpaceChanged(availableRange, selectedRange);
        }
    }

    public void reset() {
        availableRange = new Range();
        selectedRange = new Range();
    }

    @Override
    public void shiftDownPixels(double distanceY, int height) {
        double scaledMin = scale(selectedRange.min);
        double scaledMax = scale(selectedRange.max);

        double ratioValue = (scaledMax - scaledMin) / height;
        double shift = distanceY * ratioValue;
        double startValue = scaledMin + shift;
        double endValue = scaledMax + shift;
        if (startValue < Math.log10(Float.MIN_VALUE)) {
            double oldStart = startValue;
            startValue = Math.log10(Float.MIN_VALUE);
            endValue = startValue + (endValue - oldStart);
        } else if (endValue > Math.log10(Float.MAX_VALUE)) {
            double oldEnd = endValue;
            endValue = Math.log10(Float.MAX_VALUE);
            startValue = endValue - (oldEnd - startValue);
        }
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
        double newScaledMin = (1 + delta) * scaledMin - delta * scaled;
        double newScaledMax = (1 + delta) * scaledMax - delta * scaled;
        newScaledMin = Math.max(Math.log10(Float.MIN_VALUE), newScaledMin);
        newScaledMax = Math.min(Math.log10(Float.MAX_VALUE), newScaledMax);
        // newScaledMin = Math.max(scale(availableRange.min), newScaledMin);
        // newScaledMax = Math.min(scale(availableRange.max), newScaledMax);
        if (newScaledMax - newScaledMin > 0.04) {
            selectedRange.min = invScale(newScaledMin);
            selectedRange.max = invScale(newScaledMax);
            fireSelectedRangeChanged();
        }
    }

    @Override
    public double scale(double maxValue) {
        return scale.scale(maxValue);
    }

    @Override
    public double invScale(double maxValue) {
        return scale.invScale(maxValue);
    }

    private static interface YAxisElementScale {
        public abstract double scale(double val);

        public abstract double invScale(double val);

        public abstract String getLabel();
    }

    private static class YAxisElementLogScale implements YAxisElementScale {

        private final String label;

        public YAxisElementLogScale(String _label) {
            label = "log(" + _label.replace("^2", "\u00B2") + ")";
        }

        @Override
        public double scale(double val) {
            return Math.log10(val);
        }

        @Override
        public double invScale(double val) {
            return Math.pow(10, val);
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    private static class YAxisElementIdentityScale implements YAxisElementScale {

        private final String label;

        public YAxisElementIdentityScale(String _label) {
            label = _label.replace("^2", "\u00B2");
        }

        @Override
        public double scale(double val) {
            return val;
        }

        @Override
        public double invScale(double val) {
            return val;
        }

        @Override
        public String getLabel() {
            return label;
        }

    }

}
