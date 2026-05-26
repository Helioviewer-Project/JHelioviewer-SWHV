package org.helioviewer.jhv.view.uri;

import java.util.ArrayList;

import org.helioviewer.jhv.display.DisplayController;

import org.json.JSONObject;

public final class FITSViewState {

    interface Listener {
        void fitsViewStateChanged();
    }

    enum ClippingMode {
        Auto, ZScale, Range
    }

    enum ScalingMode {
        Gamma, Beta, Alpha
    }

    static final double CLIP_LIMIT = 1e20;
    static final IntParameter Z_CONTRAST = new IntParameter(1, 100, 4);
    static final GammaParameter GAMMA = new GammaParameter(10, 40);
    static final BetaParameter BETA = new BetaParameter(1, 12);
    static final AlphaParameter ALPHA = new AlphaParameter(1, 5);

    interface IndexedParameter {
        int minIndex();

        int maxIndex();
    }

    record IntParameter(int minIndex, int maxIndex, int multiplier) implements IndexedParameter {
        int toIndex(int value) {
            return Math.clamp(value / multiplier, minIndex, maxIndex);
        }

        int fromIndex(int index) {
            return multiplier * Math.clamp(index, minIndex, maxIndex);
        }

        int clampValue(int value) {
            return Math.clamp(value, fromIndex(minIndex), fromIndex(maxIndex));
        }
    }

    record GammaParameter(int minIndex, int maxIndex) implements IndexedParameter {
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

    record BetaParameter(int minIndex, int maxIndex) implements IndexedParameter {
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

    record AlphaParameter(int minIndex, int maxIndex) implements IndexedParameter {
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

    record Data(
            ClippingMode clippingMode,
            int zContrast,
            double clippingMin,
            double clippingMax,
            ScalingMode scalingMode,
            double gamma,
            double beta,
            double alpha) {

        public int zContrastIndex() {
            return Z_CONTRAST.toIndex(zContrast);
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

    private static int zContrast = 4;
    private static double clippingMin = -500;
    private static double clippingMax = 500;
    private static ClippingMode clippingMode = ClippingMode.Auto;

    private static ScalingMode scalingMode = ScalingMode.Gamma;
    private static double gamma = 1. / 2.2;
    private static double beta = 1. / (1 << 6);
    private static double alpha = Math.pow(10, 3);
    private static final ArrayList<Listener> listeners = new ArrayList<>();

    private FITSViewState() {}

    private static void refresh() {
        URIView.clearURICache();
        DisplayController.render(1);
    }

    static Data data() {
        return new Data(clippingMode, zContrast, clippingMin, clippingMax, scalingMode, gamma, beta, alpha);
    }

    public static JSONObject toJson() {
        Data data = data();
        return new JSONObject()
                .put("clippingMode", data.clippingMode().name())
                .put("zContrast", data.zContrast())
                .put("clippingMin", data.clippingMin())
                .put("clippingMax", data.clippingMax())
                .put("scalingMode", data.scalingMode().name())
                .put("gamma", data.gamma())
                .put("beta", data.beta())
                .put("alpha", data.alpha());
    }

    public static void fromJson(JSONObject jo) {
        if (jo == null)
            return;

        Data old = data();
        zContrast = Z_CONTRAST.clampValue(jo.optInt("zContrast", zContrast));
        clippingMin = Math.clamp(jo.optDouble("clippingMin", clippingMin), -CLIP_LIMIT, CLIP_LIMIT);
        clippingMax = Math.clamp(jo.optDouble("clippingMax", clippingMax), -CLIP_LIMIT, CLIP_LIMIT);
        clippingMode = readEnum(ClippingMode.class, jo.optString("clippingMode", clippingMode.name()), clippingMode);
        scalingMode = readEnum(ScalingMode.class, jo.optString("scalingMode", scalingMode.name()), scalingMode);
        gamma = GAMMA.clampValue(jo.optDouble("gamma", gamma));
        beta = BETA.clampValue(jo.optDouble("beta", beta));
        alpha = ALPHA.clampValue(jo.optDouble("alpha", alpha));

        if (!old.equals(data())) {
            notifyListeners();
            refresh();
        }
    }

    static void setZContrastIndex(int value) {
        int newZContrast = Z_CONTRAST.fromIndex(value);
        if (updateZContrast(newZContrast) && clippingMode == ClippingMode.ZScale)
            refresh();
    }

    static void setClippingMin(double value) {
        double newClippingMin = Math.clamp(value, -CLIP_LIMIT, CLIP_LIMIT);
        if (updateClippingMin(newClippingMin) && clippingMode == ClippingMode.Range)
            refresh();
    }

    static void setClippingMax(double value) {
        double newClippingMax = Math.clamp(value, -CLIP_LIMIT, CLIP_LIMIT);
        if (updateClippingMax(newClippingMax) && clippingMode == ClippingMode.Range)
            refresh();
    }

    static void setClippingMode(ClippingMode newClippingMode) {
        if (clippingMode == newClippingMode)
            return;
        clippingMode = newClippingMode;
        notifyListeners();
        refresh();
    }

    static void setScalingMode(ScalingMode newScalingMode) {
        if (scalingMode == newScalingMode)
            return;
        scalingMode = newScalingMode;
        notifyListeners();
        refresh();
    }

    static void setGammaIndex(int value) {
        double newGamma = GAMMA.fromIndex(value);
        if (updateGamma(newGamma) && scalingMode == ScalingMode.Gamma)
            refresh();
    }

    static void setBetaIndex(int value) {
        double newBeta = BETA.fromIndex(value);
        if (updateBeta(newBeta) && scalingMode == ScalingMode.Beta)
            refresh();
    }

    static void setAlphaIndex(int value) {
        double newAlpha = ALPHA.fromIndex(value);
        if (updateAlpha(newAlpha) && scalingMode == ScalingMode.Alpha)
            refresh();
    }

    static void addListener(Listener listener) {
        listeners.add(listener);
    }

    static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        listeners.forEach(Listener::fitsViewStateChanged);
    }

    private static boolean updateZContrast(int newZContrast) {
        if (zContrast == newZContrast)
            return false;
        zContrast = newZContrast;
        notifyListeners();
        return true;
    }

    private static boolean updateClippingMin(double newClippingMin) {
        if (clippingMin == newClippingMin)
            return false;
        clippingMin = newClippingMin;
        notifyListeners();
        return true;
    }

    private static boolean updateClippingMax(double newClippingMax) {
        if (clippingMax == newClippingMax)
            return false;
        clippingMax = newClippingMax;
        notifyListeners();
        return true;
    }

    private static boolean updateGamma(double newGamma) {
        if (gamma == newGamma)
            return false;
        gamma = newGamma;
        notifyListeners();
        return true;
    }

    private static boolean updateBeta(double newBeta) {
        if (beta == newBeta)
            return false;
        beta = newBeta;
        notifyListeners();
        return true;
    }

    private static boolean updateAlpha(double newAlpha) {
        if (alpha == newAlpha)
            return false;
        alpha = newAlpha;
        notifyListeners();
        return true;
    }

    private static <E extends Enum<E>> E readEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

}
