package org.helioviewer.jhv.timelines.draw;

public final class YAxis {

    public static final float BLANK = -Float.MAX_VALUE;

    private double start;
    private double end;

    private final YAxisScale scale;

    private static final float DISCARD_LEVEL_LINEAR_LOW = -100;
    private static final float DISCARD_LEVEL_LOG_LOW = 1e-10f;
    private static final float DISCARD_LEVEL_HIGH = 1e7f; // solar wind temp, xray flux: 1e4;

    private static final double ZOOMSTEP_PERCENTAGE = 0.02;

    private final float min;
    private final float max;

    private final double scaledMinBound;
    private final double scaledMaxBound;
    private boolean highlighted = false;

    public YAxis(double _start, double _end, YAxisScale _scale) {
        start = _start;
        end = _end;
        scale = _scale;

        min = scale.getMin();
        max = scale.getMax();
        scaledMinBound = scale(min);
        scaledMaxBound = scale(max);
    }

    public double start() {
        return start;
    }

    public double end() {
        return end;
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

    public Mapper mapper(int y0, int height) {
        return new Mapper(scale, scale(start), scale(end), y0, height);
    }

    public Ticks ticks(Mapper mapper) {
        double scaledStart = mapper.scaledStart();
        double scaledEnd = mapper.scaledEnd();
        if (scaledStart > scaledEnd) {
            double temp = scaledStart;
            scaledStart = scaledEnd;
            scaledEnd = temp;
        }

        int decade = (int) Math.floor(Math.log10(scaledEnd - scaledStart));
        double step = Math.pow(10, decade);
        double first = Math.floor(scaledStart / step) * step;
        double last = Math.ceil(scaledEnd / step) * step;
        if ((last - first) / step < 5) {
            step /= 2;
        }
        return new Ticks(scaledStart, scaledEnd, first, last, step);
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
        newScaledMin = Math.max(scaledMinBound, newScaledMin);

        double newScaledMax = (1 + delta) * scaledMax - delta * scaled;
        newScaledMax = Math.min(scaledMaxBound, newScaledMax);

        start = invScale(newScaledMin);
        end = invScale(newScaledMax);
    }

    public boolean preferMax() {
        return scale.preferMax();
    }

    public float clip(float val) {
        return min <= val && val <= max ? val : BLANK;
    }

    public double scale(double val) {
        return scale.scale(val);
    }

    private double invScale(double val) {
        return scale.invScale(val);
    }

    public record Ticks(double start, double end, double first, double last, double step) {}

    public static YAxisScale generateScale(String scaleString, String label) {
        try {
            return Scale.valueOf(scaleString.toUpperCase()).generateScale(label);
        } catch (Exception e) {
            return new YAxisIdentityScale(label);
        }
    }

    private enum Scale {
        LINEAR, POSITIVELINEAR, LOGARITHMIC;

        YAxisScale generateScale(String label) {
            return switch (this) {
                case LINEAR -> new YAxisIdentityScale(label);
                case POSITIVELINEAR -> new YAxisPositiveIdentityScale(label);
                case LOGARITHMIC -> new YAxisLogScale(label);
            };
        }
    }

    public interface YAxisScale {

        boolean preferMax();

        float getMin();

        float getMax();

        double scale(double val);

        double invScale(double val);

        String getLabel();

    }

    public record Mapper(YAxisScale scale, double scaledStart, double scaledEnd, int y0, int height) {
        public double pixelToScaled(int pixel) {
            return (scaledEnd - scaledStart) * (-pixel + y0 + height) / height + scaledStart;
        }

        public int scaledToPixel(double value) {
            return (int) (-height * (value - scaledStart) / (scaledEnd - scaledStart) + y0 + height);
        }

        public int dataToPixel(double value) {
            return scaledToPixel(scale.scale(value));
        }
    }

    private static String fixupUnit(String unit) {
        return unit.replace("^2", "²").replace("^3", "³").replace("^-2", "⁻²").replace("^-3", "⁻³");
    }

    public record YAxisLogScale(String label) implements YAxisScale {

        public YAxisLogScale {
            label = "log₁₀(" + fixupUnit(label) + ')';
        }

        @Override
        public boolean preferMax() {
            return true;
        }

        @Override
        public float getMin() {
            return DISCARD_LEVEL_LOG_LOW;
        }

        @Override
        public float getMax() {
            return DISCARD_LEVEL_HIGH;
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

    public record YAxisIdentityScale(String label) implements YAxisScale {

        public YAxisIdentityScale {
            label = fixupUnit(label);
        }

        @Override
        public boolean preferMax() {
            return false;
        }

        @Override
        public float getMin() {
            return DISCARD_LEVEL_LINEAR_LOW;
        }

        @Override
        public float getMax() {
            return DISCARD_LEVEL_HIGH;
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

    public record YAxisPositiveIdentityScale(String label) implements YAxisScale {

        public YAxisPositiveIdentityScale {
            label = fixupUnit(label);
        }

        @Override
        public boolean preferMax() {
            return true;
        }

        @Override
        public float getMin() {
            return 0;
        }

        @Override
        public float getMax() {
            return DISCARD_LEVEL_HIGH;
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
