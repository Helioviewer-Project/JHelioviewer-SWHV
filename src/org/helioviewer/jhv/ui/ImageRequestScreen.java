package org.helioviewer.jhv.ui;

import org.helioviewer.jhv.io.ImageRequestSettings;

public final class ImageRequestScreen implements Screen {

    private final Panel buffer = new Panel();
    private final TimeSelectorScreen time = new TimeSelectorScreen();
    private final SamplingScreen sampling = new SamplingScreen();
    private final ImageRequestSettings settings = ImageRequestSettings.instance();

    public ImageRequestScreen() {
        time.setTime(settings.start(), settings.end());
        time.addListener(settings::setInterval);
        settings.addListener(new ImageRequestSettings.Listener() {
            @Override
            public void intervalChanged(long start, long end) {
                time.setTime(start, end);
            }
        });
    }

    @Override
    public void listen(Runnable listener) {
        time.listen(listener);
        sampling.listen(listener);
    }

    @Override
    public Panel build() {
        build(buffer.reset());
        return buffer;
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        time.command(owner, item, command);
    }

    @Override
    public void number(int owner, int item, double value) {
        if (item == SamplingScreen.CADENCE || item == SamplingScreen.FRAMES) sampling.number(owner, item, value);
        else time.number(owner, item, value);
    }

    @Override
    public void choice(int owner, int item, int value) {
        if (item == SamplingScreen.SAMPLING || item == SamplingScreen.CADENCE_UNIT) sampling.choice(owner, item, value);
        else time.choice(owner, item, value);
    }

    @Override
    public void setText(int owner, int item, String text) {
        time.setText(owner, item, text);
    }

    public void build(Panel p) {
        time.build(p, sampling);
    }

}
