package org.helioviewer.jhv.ui;

import org.helioviewer.jhv.image.ImageProcessingSettings;

// The FITS clipping and scaling. The sliders run over indices: 10/γ, log2(1/β) and log10(α).
public final class FitsOptions implements Screen {

    private static final int CLIPPING = 26;
    private static final int CLIP_MIN = 27;
    private static final int CLIP_MAX = 28;
    private static final int SCALING = 29;
    private static final int GAMMA = 30;
    private static final int BETA = 31;
    private static final int ALPHA = 32;
    private static final int CLIP_COLUMNS = 5;

    private final Panel buffer = new Panel();
    private final ImageProcessingSettings state;

    public FitsOptions(ImageProcessingSettings _state) {
        state = _state;
    }

    public static boolean owns(int item) {
        return item >= CLIPPING && item <= ALPHA;
    }

    public static void form(Panel p, ImageProcessingSettings.FITSParameters f) {
        p.grid(3).identity(3).columnsFrom(1).stretch(1).right();
        boolean first = true;
        for (ImageProcessingSettings.ClippingMode mode : ImageProcessingSettings.ClippingMode.values()) {
            if (first)
                p.caption("Clipping:");
            else
                p.space(0, 0);
            first = false;
            p.row().swingMargin(0, 5, 0, 5).radio(CLIPPING, mode.toString(), mode.ordinal(), f.clippingMode().ordinal());
            if (mode == ImageProcessingSettings.ClippingMode.Range) {
                boolean readOnly = f.clippingMode() != ImageProcessingSettings.ClippingMode.Range;
                double limit = ImageProcessingSettings.CLIP_LIMIT;
                p.text(CLIP_MIN, String.format("%g", f.clippingMin()), CLIP_COLUMNS).numeric(f.clippingMin(), -limit, limit).activateOnPress().readOnly(readOnly)
                        .text(CLIP_MAX, String.format("%g", f.clippingMax()), CLIP_COLUMNS).numeric(f.clippingMax(), -limit, limit).activateOnPress().readOnly(readOnly);
            }
            p.rowEnd().space(0, 0).endRow();
        }
        int scaling = f.scalingMode().ordinal();
        long gamma = gammaIndex(f.gamma());
        long beta = betaIndex(f.beta());
        long alpha = alphaIndex(f.alpha());
        p.radio(SCALING, "γ", ImageProcessingSettings.ScalingMode.Gamma.ordinal(), scaling).tip("pixel^(1/γ)")
                .slider(GAMMA, gamma, gammaIndex(ImageProcessingSettings.GAMMA_MAX), gammaIndex(ImageProcessingSettings.GAMMA_MIN)).activateOnPress().step(1)
                .readout(String.valueOf(gamma / 10.), "4.0").endRow()
                .radio(SCALING, "β", ImageProcessingSettings.ScalingMode.Beta.ordinal(), scaling).tip("asinh(pixel / 2^β)")
                .slider(BETA, beta, betaIndex(ImageProcessingSettings.BETA_MAX), betaIndex(ImageProcessingSettings.BETA_MIN)).activateOnPress().step(1)
                .readout(String.valueOf(beta), "12").endRow()
                .radio(SCALING, "α", ImageProcessingSettings.ScalingMode.Alpha.ordinal(), scaling).tip("log1p(10^α⋅pixel) / log1p(10^α)")
                .slider(ALPHA, alpha, alphaIndex(ImageProcessingSettings.ALPHA_MIN), alphaIndex(ImageProcessingSettings.ALPHA_MAX)).activateOnPress().step(1)
                .readout(String.valueOf(alpha), "5").endRow()
                .gridEnd();
    }

    private static long gammaIndex(double gamma) {
        return Math.round(10 / gamma);
    }

    private static long betaIndex(double beta) {
        return Math.round(Math.log(1 / beta) / Math.log(2));
    }

    private static long alphaIndex(double alpha) {
        return Math.round(Math.log10(alpha));
    }

    // The press on a slider or clip field selects its mode; a slider changes its parameter only.
    public static void press(ImageProcessingSettings f, int item) {
        switch (item) {
            case CLIP_MIN, CLIP_MAX -> f.setClippingMode(ImageProcessingSettings.ClippingMode.Range);
            case GAMMA -> f.setScalingMode(ImageProcessingSettings.ScalingMode.Gamma);
            case BETA -> f.setScalingMode(ImageProcessingSettings.ScalingMode.Beta);
            case ALPHA -> f.setScalingMode(ImageProcessingSettings.ScalingMode.Alpha);
            default -> throw new IllegalArgumentException("FITS press " + item);
        }
    }

    public static void number(ImageProcessingSettings f, int item, double value) {
        switch (item) {
            case GAMMA -> f.setGamma(10 / Math.rint(value));
            case BETA -> f.setBeta(1. / (1 << (int) Math.round(value)));
            case ALPHA -> f.setAlpha(Math.pow(10, Math.rint(value)));
            default -> throw new IllegalArgumentException("FITS number " + item);
        }
    }

    public static void choice(ImageProcessingSettings f, int item, int value) {
        switch (item) {
            case CLIPPING -> f.setClippingMode(Panel.choice(ImageProcessingSettings.ClippingMode.values(), value));
            case SCALING -> f.setScalingMode(Panel.choice(ImageProcessingSettings.ScalingMode.values(), value));
            default -> throw new IllegalArgumentException("FITS choice " + item);
        }
    }

    // Unparsable values keep the current ones.
    public static void text(ImageProcessingSettings f, int item, String text) {
        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException ignore) {
            return;
        }
        switch (item) {
            case CLIP_MIN -> f.setClippingMin(value);
            case CLIP_MAX -> f.setClippingMax(value);
            default -> throw new IllegalArgumentException("FITS text " + item);
        }
    }

    @Override
    public Panel build() {
        form(buffer.reset(), state.fitsParameters());
        return buffer;
    }

    @Override
    public void listen(Runnable listener) {
        state.addFITSListener(listener::run);
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        if (command == Panel.Command.PRESS)
            press(state, item);
    }

    @Override
    public void number(int owner, int item, double value) {
        number(state, item, value);
    }

    @Override
    public void choice(int owner, int item, int value) {
        choice(state, item, value);
    }

    @Override
    public void setText(int owner, int item, String text) {
        text(state, item, text);
    }

}
