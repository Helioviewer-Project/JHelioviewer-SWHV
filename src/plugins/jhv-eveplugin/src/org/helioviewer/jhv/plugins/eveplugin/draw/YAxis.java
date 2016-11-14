package org.helioviewer.jhv.plugins.eveplugin.draw;

import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;

public class YAxis {

    public double start;
    public double end;

    private final YAxisScale scale;

    private static final double ZOOMSTEP_PERCENTAGE = 0.02;
    private static final float UNSCALED_MIN_BOUND = Float.MIN_VALUE;
    private static final float UNSCALED_MAX_BOUND = Float.MAX_VALUE;

    private final double scaledMinBound;
    private final double scaledMaxBound;
    private boolean highlighted = false;

    public YAxis(double _start, double _end, String label, boolean isLogScale) {
        start = _start;
        end = _end;
        scale = isLogScale ? new YAxisLogScale(label) : new YAxisPositiveIdentityScale(label);
        scaledMinBound = scale(UNSCALED_MIN_BOUND);
        scaledMaxBound = scale(UNSCALED_MAX_BOUND);
    }

    public void reset(double _start, double _end) {
        start = _start;
        end = _end;
    }

    public void setHighlighted(boolean _highlighted) {
        highlighted = _highlighted;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public double pixel2ScaledValue(int y0, int h, int p) {
        double smin = scale(start);
        double smax = scale(end);
        return (smax - smin) * (-p + y0 + h) / h + smin;
    }

    public int scaledvalue2pixel(int y0, int h, double value) {
        double smin = scale(start);
        double smax = scale(end);
        return (int) (-h * (value - smin) / (smax - smin) + y0 + h);
    }

    public int value2pixel(int y0, int h, double value) {
        return scaledvalue2pixel(y0, h, scale(value));
    }

    public String getLabel() {
        return scale.getLabel();
    }

    public void shiftDownPixels(double distanceY, int height) {
        double scaledMin = scale(start);
        double scaledMax = scale(end);

        double ratioValue = (scaledMax - scaledMin) / height;
        double shift = distanceY * ratioValue;
        double startValue = scaledMin + shift;
        double endValue = scaledMax + shift;
        if (startValue < scaledMinBound) {
            double oldStart = startValue;
            startValue = scaledMinBound;
            endValue = startValue + (endValue - oldStart);
        } else if (endValue > scaledMaxBound) {
            double oldEnd = endValue;
            endValue = scaledMaxBound;
            startValue = endValue - (oldEnd - startValue);
        }
        start = invScale(startValue);
        end = invScale(endValue);
    }

    public void zoomSelectedRange(double scrollValue, double relativeY, double height) {
        double scaledMin = scale(start);
        double scaledMax = scale(end);
        double scaled = scaledMin + (scaledMax - scaledMin) * (relativeY / height);
        double delta = scrollValue * ZOOMSTEP_PERCENTAGE;

        double newScaledMin = (1 + delta) * scaledMin - delta * scaled;
        double newScaledMax = (1 + delta) * scaledMax - delta * scaled;

        newScaledMin = Math.max(scaledMinBound, newScaledMin);
        newScaledMax = Math.min(scaledMaxBound, newScaledMax);

        start = invScale(newScaledMin);
        end = invScale(newScaledMax);
    }

    public double scale(double maxValue) {
        return scale.scale(maxValue);
    }

    private double invScale(double maxValue) {
        return scale.invScale(maxValue);
    }

    private interface YAxisScale {

        double scale(double val);

        double invScale(double val);

        String getLabel();

    }

    private static class YAxisLogScale implements YAxisScale {
        private final String label;

        public YAxisLogScale(String _label) {
            label = "log(" + _label.replace("^2", "\u00B2") + ")";
        }

        @Override
        public double scale(double val) {
            if (val < DrawConstants.DISCARD_LEVEL_LOW)
                return Math.log10(DrawConstants.DISCARD_LEVEL_LOW);
            if (val > DrawConstants.DISCARD_LEVEL_HIGH)
                return Math.log10(DrawConstants.DISCARD_LEVEL_HIGH);
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

    private static class YAxisPositiveIdentityScale implements YAxisScale {

        private final String label;

        public YAxisPositiveIdentityScale(String _label) {
            label = _label.replace("^2", "\u00B2");
        }

        @Override
        public double scale(double val) {
            if (val < 0)
                return 0;
            if (val > DrawConstants.DISCARD_LEVEL_HIGH)
                return DrawConstants.DISCARD_LEVEL_HIGH;
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
