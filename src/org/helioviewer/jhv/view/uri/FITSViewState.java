package org.helioviewer.jhv.view.uri;

import java.util.ArrayList;

import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.MathUtils;

public final class FITSViewState {

    public interface Listener {
        void fitsViewStateChanged();
    }

    public enum ClippingMode {
        Auto, ZScale, Range
    }

    public enum ScalingMode {
        Gamma, Beta, Alpha
    }

    public static final double CLIP_LIMIT = 1e12;
    public static final int GAMMA_SLIDER_MIN = 10;
    public static final int GAMMA_SLIDER_MAX = 40;
    public static final int BETA_SLIDER_MIN = 1;
    public static final int BETA_SLIDER_MAX = 12;
    public static final int ALPHA_SLIDER_MIN = 1;
    public static final int ALPHA_SLIDER_MAX = 5;
    public static final int Z_CONTRAST_SLIDER_MIN = 1;
    public static final int Z_CONTRAST_SLIDER_MAX = 100;

    public record Data(
            ClippingMode clippingMode,
            int zContrast,
            double clippingMin,
            double clippingMax,
            ScalingMode scalingMode,
            double gamma,
            double beta,
            double alpha) {

        public int zContrastIndex() {
            return clamp(zContrast / 4, Z_CONTRAST_SLIDER_MIN, Z_CONTRAST_SLIDER_MAX);
        }

        public int gammaIndex() {
            return clamp((int) (10. / gamma), GAMMA_SLIDER_MIN, GAMMA_SLIDER_MAX);
        }

        public double gammaDisplayValue() {
            return gammaIndex() / 10.;
        }

        public int betaIndex() {
            return clamp((int) (Math.log(1 / beta) / Math.log(2)), BETA_SLIDER_MIN, BETA_SLIDER_MAX);
        }

        public int alphaIndex() {
            return clamp((int) Math.log10(alpha), ALPHA_SLIDER_MIN, ALPHA_SLIDER_MAX);
        }

        public double mapScaled(float d, float range) {
            return switch (scalingMode) {
                case Gamma -> Math.pow(d, gamma);
                case Beta -> MathUtils.asinh(d * beta);
                case Alpha -> Math.log1p(d / range * alpha);
            };
        }

        public double scaleFactor(float min, float max) {
            return switch (scalingMode) {
                case Gamma -> 65535. / Math.pow(max - min, gamma);
                case Beta -> 65535. / MathUtils.asinh((max - min) * beta);
                case Alpha -> 65535. / Math.log1p(alpha);
            };
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

    private FITSViewState() {
    }

    public static void refresh() {
        URIView.clearURICache();
        MovieDisplay.render(1);
    }

    public static Data data() {
        return new Data(clippingMode, zContrast, clippingMin, clippingMax, scalingMode, gamma, beta, alpha);
    }

    public static void setZContrastIndex(int value) {
        setZContrastIndex(value, false);
    }

    public static void setZContrastIndex(int value, boolean adjusting) {
        int newZContrast = 4 * clamp(value, Z_CONTRAST_SLIDER_MIN, Z_CONTRAST_SLIDER_MAX);
        if (updateZContrast(newZContrast) && clippingMode == ClippingMode.ZScale && !adjusting)
            refresh();
    }

    public static void setClippingMin(double value) {
        double newClippingMin = clamp(value, -CLIP_LIMIT, CLIP_LIMIT);
        if (updateClippingMin(newClippingMin) && clippingMode == ClippingMode.Range)
            refresh();
    }

    public static void setClippingMax(double value) {
        double newClippingMax = clamp(value, -CLIP_LIMIT, CLIP_LIMIT);
        if (updateClippingMax(newClippingMax) && clippingMode == ClippingMode.Range)
            refresh();
    }

    public static void setClippingMode(ClippingMode newClippingMode) {
        if (clippingMode == newClippingMode)
            return;
        clippingMode = newClippingMode;
        notifyListeners();
        refresh();
    }

    public static void setScalingMode(ScalingMode newScalingMode) {
        if (scalingMode == newScalingMode)
            return;
        scalingMode = newScalingMode;
        notifyListeners();
        refresh();
    }

    public static void setGammaIndex(int value) {
        setGammaIndex(value, false);
    }

    public static void setGammaIndex(int value, boolean adjusting) {
        double newGamma = 10. / clamp(value, GAMMA_SLIDER_MIN, GAMMA_SLIDER_MAX);
        if (updateGamma(newGamma) && scalingMode == ScalingMode.Gamma && !adjusting)
            refresh();
    }

    public static void setBetaIndex(int value) {
        setBetaIndex(value, false);
    }

    public static void setBetaIndex(int value, boolean adjusting) {
        double newBeta = 1. / (1 << clamp(value, BETA_SLIDER_MIN, BETA_SLIDER_MAX));
        if (updateBeta(newBeta) && scalingMode == ScalingMode.Beta && !adjusting)
            refresh();
    }

    public static void setAlphaIndex(int value) {
        setAlphaIndex(value, false);
    }

    public static void setAlphaIndex(int value, boolean adjusting) {
        double newAlpha = Math.pow(10, clamp(value, ALPHA_SLIDER_MIN, ALPHA_SLIDER_MAX));
        if (updateAlpha(newAlpha) && scalingMode == ScalingMode.Alpha && !adjusting)
            refresh();
    }

    public static void addListener(Listener listener) {
        listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
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

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }

    private static double clamp(double value, double min, double max) {
        return Math.clamp(value, min, max);
    }
}
