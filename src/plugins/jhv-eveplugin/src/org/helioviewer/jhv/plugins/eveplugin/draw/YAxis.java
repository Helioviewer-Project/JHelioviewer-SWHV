package org.helioviewer.jhv.plugins.eveplugin.draw;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;

/**
 * This class describes an Y-axis.
 *
 * @author Bram.Bourgoignie@oma.be
 */

public class YAxis {

    public enum YAxisLocation {
        LEFT, RIGHT;
    }

    /** The current selected range */
    protected Range selectedRange;
    /** The label of the y-axis */
    private String label;

    private YAxisLocation location;
    private YAxisScale scale;
    protected static final double ZOOMSTEP_PERCENTAGE = 0.02;

    private final static float UNSCALED_MIN_BOUND = Float.MIN_VALUE;
    private final static float UNSCALED_MAX_BOUND = Float.MAX_VALUE;

    public YAxis(Range selectedRange, String label, boolean isLogScale) {
        this.selectedRange = selectedRange;
        this.label = label;
        setIsLogScale(isLogScale);
    }

    public YAxis() {
        selectedRange = new Range();
        label = "";
        setIsLogScale(true);
    }

    public int value2pixel(int y0, int h, double value) {
        return (int) (-h * (value - getScaledMinValue()) / (getScaledMaxValue() - getScaledMinValue()) + y0 + h);
    }

    public Range getSelectedRange() {
        return selectedRange;
    }

    public void setSelectedRange(Range selectedRange) {
        this.selectedRange = selectedRange;
        fireSelectedRangeChanged();
    }

    public String getLabel() {
        return scale.getLabel();
    }

    public String getOriginalLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getMinValue() {
        return selectedRange.min;
    }

    public double getMaxValue() {
        return selectedRange.max;
    }

    public double getScaledMinValue() {
        return scale(selectedRange.min);
    }

    public double getScaledMaxValue() {
        return scale(selectedRange.max);
    }

    public void set(String label, boolean isLogScale) {
        this.label = label;
        setIsLogScale(isLogScale);
    }

    public void setIsLogScale(boolean isLogScale) {
        if (isLogScale) {
            scale = new YAxisLogScale(label);
        } else {
            scale = new YAxisIdentityScale(label);
        }
    }

    public YAxisLocation getLocation() {
        return location;
    }

    public void setLocation(YAxisLocation location) {
        this.location = location;
    }

    protected void fireSelectedRangeChanged() {
        EVEPlugin.dc.rangeChanged();
    }

    public void reset() {
        selectedRange = new Range();
    }

    public void shiftDownPixels(double distanceY, int height) {
        double scaledMin = scale(selectedRange.min);
        double scaledMax = scale(selectedRange.max);

        double ratioValue = (scaledMax - scaledMin) / height;
        double shift = distanceY * ratioValue;
        double startValue = scaledMin + shift;
        double endValue = scaledMax + shift;
        if (startValue < scale(UNSCALED_MIN_BOUND)) {
            double oldStart = startValue;
            startValue = scale(UNSCALED_MIN_BOUND);
            endValue = startValue + (endValue - oldStart);
        } else if (endValue > scale(UNSCALED_MAX_BOUND)) {
            double oldEnd = endValue;
            endValue = scale(UNSCALED_MAX_BOUND);
            startValue = endValue - (oldEnd - startValue);
        }
        selectedRange.min = invScale(startValue);
        selectedRange.max = invScale(endValue);
        fireSelectedRangeChanged();
    }

    public void zoomSelectedRange(double scrollValue, double relativeY, double height) {
        double scaledMin = scale(selectedRange.min);
        double scaledMax = scale(selectedRange.max);
        double scaled = scaledMin + (scaledMax - scaledMin) * (relativeY / height);
        double delta = scrollValue * ZOOMSTEP_PERCENTAGE;
        double newScaledMin = (1 + delta) * scaledMin - delta * scaled;
        double newScaledMax = (1 + delta) * scaledMax - delta * scaled;
        newScaledMin = Math.max(scale(UNSCALED_MIN_BOUND), newScaledMin);
        newScaledMax = Math.min(scale(UNSCALED_MAX_BOUND), newScaledMax);

        if (newScaledMax - newScaledMin > 0.04) {
            selectedRange.min = invScale(newScaledMin);
            selectedRange.max = invScale(newScaledMax);
            fireSelectedRangeChanged();
        }
    }

    public double scale(double maxValue) {
        return scale.scale(maxValue);
    }

    public double invScale(double maxValue) {
        return scale.invScale(maxValue);
    }

    private static interface YAxisScale {
        public abstract double scale(double val);

        public abstract double invScale(double val);

        public abstract String getLabel();
    }

    private static class YAxisLogScale implements YAxisScale {

        private final String label;

        public YAxisLogScale(String _label) {
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

    private static class YAxisIdentityScale implements YAxisScale {

        private final String label;

        public YAxisIdentityScale(String _label) {
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
