package org.helioviewer.jhv.plugins.eveplugin.draw;

/**
 * This class describes an Y-axis.
 *
 * @author Bram.Bourgoignie@oma.be
 */

public class YAxis {

    public enum YAxisLocation {
        LEFT, RIGHT;
    }

    public double start;
    public double end;

    private YAxisScale scale;
    protected static final double ZOOMSTEP_PERCENTAGE = 0.02;

    private final static float UNSCALED_MIN_BOUND = Float.MIN_VALUE;
    private final static float UNSCALED_MAX_BOUND = Float.MAX_VALUE;

    public YAxis(double _start, double _end, String label, boolean isLogScale) {
        start = _start;
        end = _end;
        if (isLogScale) {
            scale = new YAxisLogScale(label);
        } else {
            scale = new YAxisIdentityScale(label);
        }
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
        if (startValue < scale(UNSCALED_MIN_BOUND)) {
            double oldStart = startValue;
            startValue = scale(UNSCALED_MIN_BOUND);
            endValue = startValue + (endValue - oldStart);
        } else if (endValue > scale(UNSCALED_MAX_BOUND)) {
            double oldEnd = endValue;
            endValue = scale(UNSCALED_MAX_BOUND);
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

        newScaledMin = Math.max(scale(UNSCALED_MIN_BOUND), newScaledMin);
        newScaledMax = Math.min(scale(UNSCALED_MAX_BOUND), newScaledMax);

        start = invScale(newScaledMin);
        end = invScale(newScaledMax);
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
