package org.helioviewer.jhv.image;

import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.view.ClipSet;

import org.json.JSONObject;

public final class ImageProcessingSettings {

    public interface FITSListener {
        void fitsParametersChanged();
    }

    public enum ClippingMode {
        Percentile001("Percentile 0.001%", 0.00001),
        Percentile05("Percentile 0.5%", 0.005),
        Range("Range", 0);

        private final String label;
        private final double percentile;

        ClippingMode(String _label, double _percentile) {
            label = _label;
            percentile = _percentile;
        }

        public double percentile() {
            return percentile;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum ScalingMode {
        Gamma, Beta, Alpha
    }

    public static final double CLIP_LIMIT = 1e20;
    public static final GammaParameter GAMMA = new GammaParameter(10, 40);
    public static final BetaParameter BETA = new BetaParameter(1, 12);
    public static final AlphaParameter ALPHA = new AlphaParameter(1, 5);

    public interface IndexedParameter {
        int minIndex();

        int maxIndex();
    }

    public record GammaParameter(int minIndex, int maxIndex) implements IndexedParameter {
        int toIndex(double value) {
            return Math.clamp(Math.round(10. / value), minIndex, maxIndex);
        }

        double fromIndex(int index) {
            return 10. / Math.clamp(index, minIndex, maxIndex);
        }

        double displayValue(int index) {
            return index / 10.;
        }

        double clampValue(double value) {
            return Math.clamp(value, fromIndex(maxIndex), fromIndex(minIndex));
        }
    }

    public record BetaParameter(int minIndex, int maxIndex) implements IndexedParameter {
        int toIndex(double value) {
            return Math.clamp(Math.round(Math.log(1 / value) / Math.log(2)), minIndex, maxIndex);
        }

        double fromIndex(int index) {
            return 1. / (1 << Math.clamp(index, minIndex, maxIndex));
        }

        double clampValue(double value) {
            return Math.clamp(value, fromIndex(maxIndex), fromIndex(minIndex));
        }
    }

    public record AlphaParameter(int minIndex, int maxIndex) implements IndexedParameter {
        int toIndex(double value) {
            return Math.clamp(Math.round(Math.log10(value)), minIndex, maxIndex);
        }

        double fromIndex(int index) {
            return Math.pow(10, Math.clamp(index, minIndex, maxIndex));
        }

        double clampValue(double value) {
            return Math.clamp(value, fromIndex(minIndex), fromIndex(maxIndex));
        }
    }

    public record FITSParameters(
            ClippingMode clippingMode,
            double clippingMin,
            double clippingMax,
            ScalingMode scalingMode,
            double gamma,
            double beta,
            double alpha) {

        @Nullable
        public ClipSet.Range clipRange(@Nullable ClipSet clipSet) {
            return switch (clippingMode) {
                case Percentile001 -> clipSet == null ? null : clipSet.percentile001();
                case Percentile05 -> clipSet == null ? null : clipSet.percentile05();
                case Range -> new ClipSet.Range((float) clippingMin, (float) clippingMax);
            };
        }

        public int gammaIndex() {
            return GAMMA.toIndex(gamma);
        }

        public double gammaDisplayValue() {
            return GAMMA.displayValue(gammaIndex());
        }

        public int betaIndex() {
            return BETA.toIndex(beta);
        }

        public int alphaIndex() {
            return ALPHA.toIndex(alpha);
        }
    }

    private ImageFilter.Type filter = ImageFilter.Type.None;

    private double clippingMin = -500;
    private double clippingMax = 500;
    private ClippingMode clippingMode = ClippingMode.Percentile001;

    private ScalingMode scalingMode = ScalingMode.Gamma;
    private double gamma = 1. / 2.2;
    private double beta = 1. / (1 << 6);
    private double alpha = Math.pow(10, 3);
    private final ArrayList<FITSListener> listeners = new ArrayList<>();
    private volatile FITSParameters fitsParameters = createFITSParameters();
    private final Runnable onChange;

    public ImageFilter.Type getFilter() {
        return filter;
    }

    public void setFilter(ImageFilter.Type type) {
        if (filter == type)
            return;
        filter = type;
        onChange.run();
    }

    public FITSParameters fitsParameters() {
        return fitsParameters;
    }

    private FITSParameters createFITSParameters() {
        return new FITSParameters(clippingMode, clippingMin, clippingMax, scalingMode, gamma, beta, alpha);
    }

    public void serialize(JSONObject jo) {
        FITSParameters current = fitsParameters();
        jo.put("clippingMode", current.clippingMode().name());
        jo.put("clippingMin", current.clippingMin());
        jo.put("clippingMax", current.clippingMax());
        jo.put("scalingMode", current.scalingMode().name());
        jo.put("gamma", current.gamma());
        jo.put("beta", current.beta());
        jo.put("alpha", current.alpha());
    }

    public void fromJson(JSONObject jo) {
        if (jo == null)
            return;

        FITSParameters old = fitsParameters();
        clippingMin = Math.clamp(jo.optDouble("clippingMin", clippingMin), -CLIP_LIMIT, CLIP_LIMIT);
        clippingMax = Math.clamp(jo.optDouble("clippingMax", clippingMax), -CLIP_LIMIT, CLIP_LIMIT);
        clippingMode = readEnum(ClippingMode.class, jo.optString("clippingMode", clippingMode.name()), clippingMode);
        scalingMode = readEnum(ScalingMode.class, jo.optString("scalingMode", scalingMode.name()), scalingMode);
        gamma = GAMMA.clampValue(jo.optDouble("gamma", gamma));
        beta = BETA.clampValue(jo.optDouble("beta", beta));
        alpha = ALPHA.clampValue(jo.optDouble("alpha", alpha));

        if (!old.equals(createFITSParameters())) {
            notifyFITSListeners();
            onChange.run();
        }
    }

    public void setClippingMin(double value) {
        double newClippingMin = Math.clamp(value, -CLIP_LIMIT, CLIP_LIMIT);
        if (updateClippingMin(newClippingMin) && clippingMode == ClippingMode.Range)
            onChange.run();
    }

    public void setClippingMax(double value) {
        double newClippingMax = Math.clamp(value, -CLIP_LIMIT, CLIP_LIMIT);
        if (updateClippingMax(newClippingMax) && clippingMode == ClippingMode.Range)
            onChange.run();
    }

    public void setClippingMode(ClippingMode newClippingMode) {
        if (clippingMode == newClippingMode)
            return;
        clippingMode = newClippingMode;
        notifyFITSListeners();
        onChange.run();
    }

    public void setScalingMode(ScalingMode newScalingMode) {
        if (scalingMode == newScalingMode)
            return;
        scalingMode = newScalingMode;
        notifyFITSListeners();
        onChange.run();
    }

    public void setGammaIndex(int value) {
        double newGamma = GAMMA.fromIndex(value);
        if (updateGamma(newGamma) && scalingMode == ScalingMode.Gamma)
            onChange.run();
    }

    public void setBetaIndex(int value) {
        double newBeta = BETA.fromIndex(value);
        if (updateBeta(newBeta) && scalingMode == ScalingMode.Beta)
            onChange.run();
    }

    public void setAlphaIndex(int value) {
        double newAlpha = ALPHA.fromIndex(value);
        if (updateAlpha(newAlpha) && scalingMode == ScalingMode.Alpha)
            onChange.run();
    }

    public void addFITSListener(FITSListener listener) {
        listeners.add(listener);
    }

    private void notifyFITSListeners() {
        fitsParameters = createFITSParameters();
        listeners.forEach(FITSListener::fitsParametersChanged);
    }

    private boolean updateClippingMin(double newClippingMin) {
        if (clippingMin == newClippingMin)
            return false;
        clippingMin = newClippingMin;
        notifyFITSListeners();
        return true;
    }

    private boolean updateClippingMax(double newClippingMax) {
        if (clippingMax == newClippingMax)
            return false;
        clippingMax = newClippingMax;
        notifyFITSListeners();
        return true;
    }

    private boolean updateGamma(double newGamma) {
        if (gamma == newGamma)
            return false;
        gamma = newGamma;
        notifyFITSListeners();
        return true;
    }

    private boolean updateBeta(double newBeta) {
        if (beta == newBeta)
            return false;
        beta = newBeta;
        notifyFITSListeners();
        return true;
    }

    private boolean updateAlpha(double newAlpha) {
        if (alpha == newAlpha)
            return false;
        alpha = newAlpha;
        notifyFITSListeners();
        return true;
    }

    private static <E extends Enum<E>> E readEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public ImageProcessingSettings(Runnable _onChange) {
        onChange = _onChange;
    }
}
