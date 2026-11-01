package org.helioviewer.jhv.ui;

import org.helioviewer.jhv.io.ImageRequestSettings;

public final class SamplingScreen implements Screen {
    static final int SAMPLING = 6, CADENCE = 7, CADENCE_UNIT = 8, FRAMES = 9;
    private final Panel buffer = new Panel();
    private Runnable changed = () -> {};
    private static final String[] CADENCE_UNITS = {"sec", "min", "hours", "days", "get all"};
    private static final int[] CADENCE_SECONDS = {1, 60, 3600, 86400};

    private final ImageRequestSettings settings = ImageRequestSettings.instance();
    // What the sampling widgets show; kept across a switch, as the Swing spinners.
    private int cadence = 1;
    private int cadenceUnit = 0;
    private int frames = 97;
    private boolean frameCount;

    public SamplingScreen() {
        settings.addListener(new ImageRequestSettings.Listener() {
            @Override
            public void samplingChanged(ImageRequestSettings.Sampling sampling) {
                changed.run();
            }
        });
    }

    @Override
    public void listen(Runnable listener) {
        changed = listener;
    }

    @Override
    public Panel build() {
        return build(buffer.reset());
    }

    public Panel build(Panel p) {
        // Keep the user's units when they already represent the accepted sampling.
        if (!settings.sampling().equals(currentSampling())) {
            switch (settings.sampling()) {
                case ImageRequestSettings.TimeStep step -> {
                    frameCount = false;
                    cadenceUnit = 0;
                    for (int u = CADENCE_SECONDS.length - 1; u > 0; u--) {
                        if (step.seconds() % CADENCE_SECONDS[u] == 0) {
                            cadenceUnit = u;
                            break;
                        }
                    }
                    cadence = Math.clamp(step.seconds() / CADENCE_SECONDS[cadenceUnit], 1, 10000);
                }
                case ImageRequestSettings.All ignored -> {
                    frameCount = false;
                    cadenceUnit = CADENCE_UNITS.length - 1;
                }
                case ImageRequestSettings.FrameCount count -> {
                    frameCount = true;
                    frames = Math.clamp(count.frames(), 1, 1000);
                }
            }
        }
        return p.grid(3).identity(2).stretch(0)
                .row().radio(SAMPLING, "Time step", 0, frameCount ? 1 : 0).rowEnd()
                .numberSpinner(CADENCE, cadence, 1, 10000, 1).format(0, 1, "").width(6).live().enabled(!frameCount && cadenceUnit < CADENCE_SECONDS.length)
                .combo(CADENCE_UNIT, CADENCE_UNITS, cadenceUnit).enabled(!frameCount).endRow()
                .row().radio(SAMPLING, "Frame count", 1, frameCount ? 1 : 0).rowEnd()
                .numberSpinner(FRAMES, frames, 1, 1000, 1).format(0, 1, "").width(6).live().enabled(frameCount)
                .space(0, 0).endRow().gridEnd();
    }

    private ImageRequestSettings.Sampling currentSampling() {
        if (frameCount)
            return new ImageRequestSettings.FrameCount(frames);
        if (cadenceUnit == CADENCE_UNITS.length - 1)
            return new ImageRequestSettings.All();
        return new ImageRequestSettings.TimeStep(cadence * CADENCE_SECONDS[cadenceUnit]);
    }

    private void push() {
        settings.setSampling(currentSampling());
    }

    @Override
    public void number(int owner, int item, double value) {
        switch (item) {
            case CADENCE -> cadence = Math.clamp((int) value, 1, 10000);
            case FRAMES -> frames = Math.clamp((int) value, 1, 1000);
            default -> throw new IllegalArgumentException("Sampling number " + item);
        }
        push();
    }

    @Override
    public void choice(int owner, int item, int value) {
        switch (item) {
            case SAMPLING -> frameCount = Panel.choiceIndex(value, 2) != 0;
            case CADENCE_UNIT -> cadenceUnit = Panel.choiceIndex(value, CADENCE_UNITS.length);
            default -> throw new IllegalArgumentException("Sampling choice " + item);
        }
        push();
    }
}
