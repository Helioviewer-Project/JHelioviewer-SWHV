package org.helioviewer.jhv.view.uri;

import java.util.ArrayList;

import org.helioviewer.jhv.layers.MovieDisplay;

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
    static final int GAMMA_SLIDER_MIN = 10;
    static final int GAMMA_SLIDER_MAX = 40;
    static final int BETA_SLIDER_MIN = 1;
    static final int BETA_SLIDER_MAX = 12;
    static final int ALPHA_SLIDER_MIN = 1;
    static final int ALPHA_SLIDER_MAX = 5;
    static final int Z_CONTRAST_SLIDER_MIN = 1;
    static final int Z_CONTRAST_SLIDER_MAX = 100;

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
            return Math.clamp(zContrast / 4, Z_CONTRAST_SLIDER_MIN, Z_CONTRAST_SLIDER_MAX);
        }

        public int gammaIndex() {
            return Math.clamp((int) (10. / gamma), GAMMA_SLIDER_MIN, GAMMA_SLIDER_MAX);
        }

        public double gammaDisplayValue() {
            return gammaIndex() / 10.;
        }

        public int betaIndex() {
            return Math.clamp((int) (Math.log(1 / beta) / Math.log(2)), BETA_SLIDER_MIN, BETA_SLIDER_MAX);
        }

        public int alphaIndex() {
            return Math.clamp((int) Math.log10(alpha), ALPHA_SLIDER_MIN, ALPHA_SLIDER_MAX);
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
        MovieDisplay.render(1);
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
        zContrast = Math.clamp(jo.optInt("zContrast", zContrast), 4 * Z_CONTRAST_SLIDER_MIN, 4 * Z_CONTRAST_SLIDER_MAX);
        clippingMin = Math.clamp(jo.optDouble("clippingMin", clippingMin), -CLIP_LIMIT, CLIP_LIMIT);
        clippingMax = Math.clamp(jo.optDouble("clippingMax", clippingMax), -CLIP_LIMIT, CLIP_LIMIT);
        clippingMode = readEnum(ClippingMode.class, jo.optString("clippingMode", clippingMode.name()), clippingMode);
        scalingMode = readEnum(ScalingMode.class, jo.optString("scalingMode", scalingMode.name()), scalingMode);
        gamma = Math.clamp(jo.optDouble("gamma", gamma), gammaFromSlider(GAMMA_SLIDER_MAX), gammaFromSlider(GAMMA_SLIDER_MIN));
        beta = Math.clamp(jo.optDouble("beta", beta), betaFromSlider(BETA_SLIDER_MAX), betaFromSlider(BETA_SLIDER_MIN));
        alpha = Math.clamp(jo.optDouble("alpha", alpha), alphaFromSlider(ALPHA_SLIDER_MIN), alphaFromSlider(ALPHA_SLIDER_MAX));

        if (!old.equals(data())) {
            notifyListeners();
            refresh();
        }
    }

    static void setZContrastIndex(int value, boolean adjusting) {
        int newZContrast = 4 * Math.clamp(value, Z_CONTRAST_SLIDER_MIN, Z_CONTRAST_SLIDER_MAX);
        if (updateZContrast(newZContrast) && clippingMode == ClippingMode.ZScale && !adjusting)
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

    static void setGammaIndex(int value, boolean adjusting) {
        double newGamma = gammaFromSlider(Math.clamp(value, GAMMA_SLIDER_MIN, GAMMA_SLIDER_MAX));
        if (updateGamma(newGamma) && scalingMode == ScalingMode.Gamma && !adjusting)
            refresh();
    }

    private static double gammaFromSlider(int value) {
        return 10. / value;
    }

    static void setBetaIndex(int value, boolean adjusting) {
        double newBeta = betaFromSlider(Math.clamp(value, BETA_SLIDER_MIN, BETA_SLIDER_MAX));
        if (updateBeta(newBeta) && scalingMode == ScalingMode.Beta && !adjusting)
            refresh();
    }

    private static double betaFromSlider(int value) {
        return 1. / (1 << value);
    }

    static void setAlphaIndex(int value, boolean adjusting) {
        double newAlpha = alphaFromSlider(Math.clamp(value, ALPHA_SLIDER_MIN, ALPHA_SLIDER_MAX));
        if (updateAlpha(newAlpha) && scalingMode == ScalingMode.Alpha && !adjusting)
            refresh();
    }

    private static double alphaFromSlider(int value) {
        return Math.pow(10, value);
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
