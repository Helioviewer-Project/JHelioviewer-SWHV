package org.helioviewer.jhv.layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.swing.Timer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.view.View;

public class Movie {

    public enum AdvanceMode {
        Loop, Stop, Swing, SwingDown
    }

    @Nullable
    private static JHVDate getNextTime(AdvanceMode mode, JHVDate time,
                                       Supplier<JHVDate> getFirst, Supplier<JHVDate> getLast,
                                       Function<JHVDate, JHVDate> getLower, Function<JHVDate, JHVDate> getHigher) {
        JHVDate next = mode == AdvanceMode.SwingDown ? getLower.apply(time) : getHigher.apply(time);
        switch (mode) {
            case Stop:
                if (next.milli == getLast.get().milli && next.milli == time.milli) {
                    return null;
                }
                break;
            case Swing:
                if (next.milli == getLast.get().milli && next.milli == time.milli) {
                    setAdvanceMode(AdvanceMode.SwingDown);
                    return getLower.apply(next);
                }
                break;
            case SwingDown:
                if (next.milli == getFirst.get().milli && next.milli == time.milli) {
                    setAdvanceMode(AdvanceMode.Swing);
                    return getHigher.apply(next);
                }
                break;
            default: // Loop
                if (next.milli == getLast.get().milli && next.milli == time.milli) {
                    return getFirst.get();
                }
        }
        return next;
    }

    static void setMaster(ImageLayer layer) {
        View view;
        if (layer == null || !(view = layer.getView()).isMultiFrame()) {
            pause();
            MoviePanel.unsetMovie();
        } else
            MoviePanel.setMovie(view);
        setTime(lastTimestamp); // ensure master layer time is used
        timespanChanged();
    }

    public static long getStartTime() {
        return movieStart;
    }

    public static long getEndTime() {
        return movieEnd;
    }

    private static long getMovieStart() {
        ImageLayer layer = Layers.getActiveImageLayer();
        return layer == null ? lastTimestamp.milli : layer.getStartTime();
    }

    private static long getMovieEnd() {
        ImageLayer layer = Layers.getActiveImageLayer();
        return layer == null ? lastTimestamp.milli : layer.getEndTime();
    }

    static void timespanChanged() {
        movieStart = getMovieStart();
        movieEnd = getMovieEnd();
        timespanListeners.forEach(listener -> listener.timespanChanged(movieStart, movieEnd));
    }

    private static int deltaT;

    private static void relativeTimeAdvance() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            View view = layer.getView();
            JHVDate nextTime = getNextTime(advanceMode, lastTimestamp,
                    view::getFirstTime, view::getLastTime,
                    view::getLowerTime, view::getHigherTime);

            if (nextTime == null)
                pause();
            else
                setTime(nextTime);
        }
    }

    private static void absoluteTimeAdvance() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            View view = layer.getView();

            JHVDate first = view.getFirstTime();
            JHVDate last = view.getLastTime();
            JHVDate nextTime = getNextTime(advanceMode, lastTimestamp,
                    () -> first, () -> last,
                    lastTimestamp -> new JHVDate(Math.max(first.milli, lastTimestamp.milli - deltaT)),
                    lastTimestamp -> new JHVDate(Math.min(last.milli, lastTimestamp.milli + deltaT)));

            if (nextTime == null)
                pause();
            else
                syncTime(nextTime);
        }
    }

    private static class RelativeTimeAdvanceListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            relativeTimeAdvance();
        }
    }

    private static class AbsoluteTimeAdvanceListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            absoluteTimeAdvance();
        }
    }

    private static final RelativeTimeAdvanceListener relativeTimeAdvanceListener = new RelativeTimeAdvanceListener();
    private static final AbsoluteTimeAdvanceListener absoluteTimeAdvanceListener = new AbsoluteTimeAdvanceListener();
    private static final Timer movieTimer = new Timer(1000 / 20, relativeTimeAdvanceListener);

    public static boolean isPlaying() {
        return movieTimer.isRunning();
    }

    public static void play() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null && layer.getView().isMultiFrame()) {
            movieTimer.restart();
            MoviePanel.setPlayState(true);
        }
    }

    public static void pause() {
        movieTimer.stop();
        MoviePanel.setPlayState(false);
        MovieDisplay.render(1); /* ! force update for on the fly resolution change */
    }

    public static void toggle() {
        if (isPlaying())
            pause();
        else
            play();
    }

    public static void setTime(JHVDate dateTime) {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            syncTime(layer.getView().getNearestTime(dateTime));
        }
    }

    public static void setFrame(int frame) {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            syncTime(layer.getView().getFrameTime(frame));
        }
    }

    public static void nextFrame() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            syncTime(layer.getView().getHigherTime(lastTimestamp));
        }
    }

    public static void previousFrame() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            syncTime(layer.getView().getLowerTime(lastTimestamp));
        }
    }

    private static JHVDate lastTimestamp = TimeUtils.START;
    private static long movieStart = TimeUtils.START.milli;
    private static long movieEnd = TimeUtils.START.milli;

    public static JHVDate getTime() {
        return lastTimestamp;
    }

    private static void syncTime(JHVDate dateTime) {
        if (recording && notDone)
            return;

        lastTimestamp = dateTime;

        Camera camera = Display.getCamera();
        camera.timeChanged(lastTimestamp);

        Layers.forEachImageLayer(layer -> layer.getView().setNearestFrame(dateTime));
        MovieDisplay.render(1);

        ViewpointLayer viewpointLayer = Layers.getViewpointLayer();
        if (viewpointLayer != null)
            viewpointLayer.fireTimeUpdated(camera.getViewpoint().time); // !

        timeListeners.forEach(listener -> listener.timeChanged(lastTimestamp.milli));

        View view = Layers.getActiveImageLayer().getView(); // should be not null
        int activeFrame = view.getCurrentFrameNumber();
        boolean last = activeFrame == view.getMaximumFrameNumber();

        frameListeners.forEach(listener -> listener.frameChanged(activeFrame, last));

        MoviePanel.setFrameSlider(activeFrame);

        if (recording)
            notDone = true;
    }

    private static final ArrayList<FrameListener> frameListeners = new ArrayList<>();
    private static final ArrayList<TimeListener> timeListeners = new ArrayList<>();
    private static final ArrayList<TimespanListener> timespanListeners = new ArrayList<>();

    public static void addFrameListener(FrameListener listener) {
        if (!frameListeners.contains(listener))
            frameListeners.add(listener);
    }

    public static void removeFrameListener(FrameListener listener) {
        frameListeners.remove(listener);
    }

    public static void addTimeListener(TimeListener listener) {
        if (!timeListeners.contains(listener)) {
            timeListeners.add(listener);
            listener.timeChanged(lastTimestamp.milli);
        }
    }

    public static void removeTimeListener(TimeListener listener) {
        timeListeners.remove(listener);
    }

    public static void addTimespanListener(TimespanListener listener) {
        if (!timespanListeners.contains(listener)) {
            timespanListeners.add(listener);
            listener.timespanChanged(movieStart, movieEnd);
        }
    }

    public static void removeTimespanListener(TimespanListener listener) {
        timespanListeners.remove(listener);
    }

    public static void setDesiredRelativeSpeed(int fps) {
        movieTimer.removeActionListener(absoluteTimeAdvanceListener);
        movieTimer.addActionListener(relativeTimeAdvanceListener);
        movieTimer.setDelay(1000 / fps);
        deltaT = 0;
    }

    public static void setDesiredAbsoluteSpeed(int sec) {
        movieTimer.removeActionListener(relativeTimeAdvanceListener);
        movieTimer.addActionListener(absoluteTimeAdvanceListener);
        movieTimer.setDelay(1000 / 20);
        deltaT = 1000 / 20 * sec;
    }

    private static AdvanceMode advanceMode = AdvanceMode.Loop;

    public static void setAdvanceMode(AdvanceMode mode) {
        advanceMode = mode;
    }

    private static boolean recording;
    private static boolean notDone;

    public static void grabDone() {
        notDone = false;
    }

    public static void startRecording() {
        recording = true;
    }

    public static void stopRecording() {
        recording = false;
    }

    public static boolean isRecording() {
        return recording;
    }

}
