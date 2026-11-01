package org.helioviewer.jhv.ui;

import java.util.ArrayList;

import org.helioviewer.jhv.time.TimeListener;

public final class TimeSelectorScreen implements Screen {

    private final Panel buffer = new Panel();
    private final ArrayList<TimeListener.Selection> listeners = new ArrayList<>();
    private Runnable changed = () -> {};
    private final TimeSelection time = new TimeSelection(new TimeSelection.Ids(0, 1, 11, 12, 2, 4, 3, 10, 5), this::notifySelection);

    private void notifySelection() {
        listeners.forEach(listener -> listener.timeSelectionChanged(time.start(), time.end()));
        changed.run();
    }

    public void addListener(TimeListener.Selection listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void setTime(long start, long end) {
        time.set(start, end);
    }

    public long getStartTime() {
        return time.start();
    }

    public long getEndTime() {
        return time.end();
    }

    @Override
    public void listen(Runnable listener) {
        changed = listener;
    }

    @Override
    public Panel build() {
        Panel p = buffer.reset();
        time.build(p, true, 0);
        return p;
    }

    void build(Panel p, SamplingScreen sampling) {
        sampling.build(time.fields(p, 0));
        p.row().space(0, 0).rowEnd().space(0, 0).endRow().gridEnd().rowEnd();
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        time.command(item);
    }

    @Override
    public void number(int owner, int item, double value) {
        time.number(item, value);
    }

    @Override
    public void choice(int owner, int item, int value) {
        time.choice(item, value);
        changed.run();
    }

    @Override
    public void setText(int owner, int item, String text) {
        time.setText(item, text);
    }
}
