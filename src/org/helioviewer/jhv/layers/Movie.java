package org.helioviewer.jhv.layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.swing.Timer;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.ViewerState;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.view.View;

public class Movie {

    public enum AdvanceMode {
        Loop, Stop, Swing, SwingDown
    }

    public interface Listener {
        void frameChanged(int frame, boolean last);
    }

    public static final int FPS_RELATIVE_DEFAULT = 20;
    public static final int FPS_ABSOLUTE = 30;

    private static JHVTime playbackFirstTime = TimeUtils.START;
    private static JHVTime playbackLastTime = TimeUtils.START;
    private static AdvanceMode currentAdvanceMode = AdvanceMode.Loop;

    @Nullable
    private static JHVTime nextTime(AdvanceMode mode, JHVTime time,
                                    Supplier<JHVTime> firstTime, Supplier<JHVTime> lastTime,
                                    Function<JHVTime, JHVTime> lowerTime, Function<JHVTime, JHVTime> higherTime) {
        JHVTime next = mode == AdvanceMode.SwingDown ? lowerTime.apply(time) : higherTime.apply(time);
        if (next.milli == time.milli) { // already at the edges
            switch (mode) {
                case Loop -> {
                    if (next.milli == lastTime.get().milli) {
                        return firstTime.get();
                    }
                }
                case Stop -> {
                    if (next.milli == lastTime.get().milli) {
                        return null;
                    }
                }
                case Swing -> {
                    if (next.milli == lastTime.get().milli) {
                        setCurrentAdvanceMode(AdvanceMode.SwingDown);
                        return lowerTime.apply(next);
                    }
                }
                case SwingDown -> {
                    if (next.milli == firstTime.get().milli) {
                        setCurrentAdvanceMode(AdvanceMode.Swing);
                        return higherTime.apply(next);
                    }
                }
            }
        }
        return next;
    }

    public static void setMaster(ImageLayer layer) {
        if (layer == null) {
            pause();
            playbackFirstTime = TimeUtils.START;
            playbackLastTime = TimeUtils.START;
            ViewerState.clearMovie();
        } else {
            View view = layer.getView();
            playbackFirstTime = view.getFirstTime();
            playbackLastTime = view.getLastTime();
            ViewerState.setMovieAvailable(view.getMaximumFrameNumber());
            syncTime(playbackFirstTime);
        }
        timeRangeChanged();
    }

    public static void setPlaybackRange(int firstFrame, int lastFrame) {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer == null) {
            playbackFirstTime = TimeUtils.START;
            playbackLastTime = TimeUtils.START;
        } else {
            View view = layer.getView();
            int maximum = view.getMaximumFrameNumber();
            playbackFirstTime = view.getFrameTime(Math.clamp(firstFrame, 0, maximum));
            playbackLastTime = view.getFrameTime(Math.clamp(lastFrame, 0, maximum));
        }
    }

    public static long getStartTime() {
        return movieStart;
    }

    public static long getEndTime() {
        return movieEnd;
    }

    private static long getMovieStart() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer == null || layer.isLoadingForTimespan())
            return lastTimestamp.milli;
        return layer.getStartTime();
    }

    private static long getMovieEnd() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer == null || layer.isLoadingForTimespan())
            return lastTimestamp.milli;
        return layer.getEndTime();
    }

    static void timeRangeChanged() {
        movieStart = getMovieStart();
        movieEnd = getMovieEnd();
        timeRangeListeners.forEach(listener -> listener.timeRangeChanged(movieStart, movieEnd));
    }

    private static int deltaT;

    private static void relativeTimeAdvance() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            View view = layer.getView();
            JHVTime next = nextTime(currentAdvanceMode, lastTimestamp,
                    () -> playbackFirstTime, () -> playbackLastTime,
                    time -> JHVTime.clamp(view.getLowerTime(time), playbackFirstTime, playbackLastTime),
                    time -> JHVTime.clamp(view.getHigherTime(time), playbackFirstTime, playbackLastTime));

            if (next == null)
                pause();
            else
                setTime(next);
        }
    }

    private static void absoluteTimeAdvance() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            JHVTime next = nextTime(currentAdvanceMode, lastTimestamp,
                    () -> playbackFirstTime, () -> playbackLastTime,
                    time -> new JHVTime(Math.max(playbackFirstTime.milli, time.milli - deltaT)),
                    time -> new JHVTime(Math.min(playbackLastTime.milli, time.milli + deltaT)));

            if (next == null)
                pause();
            else
                syncTime(next);
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
    private static final Timer movieTimer = new Timer(1000 / FPS_RELATIVE_DEFAULT, relativeTimeAdvanceListener);

    public static boolean isPlaying() {
        return movieTimer.isRunning();
    }

    public static void play() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null && layer.getView().isMultiFrame()) {
            movieTimer.restart();
            ViewerState.setMoviePlaying(true);
        }
    }

    public static void pause() {
        movieTimer.stop();
        ViewerState.setMoviePlaying(false);
        MovieDisplay.render(1); /* ! force update for on the fly resolution change */
    }

    public static void toggle() {
        if (isPlaying())
            pause();
        else
            play();
    }

    public static void setTime(JHVTime dateTime) {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            syncTime(JHVTime.clamp(layer.getView().getNearestTime(dateTime), playbackFirstTime, playbackLastTime));
        }
    }

    public static void setFrame(int frame) {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            syncTime(JHVTime.clamp(layer.getView().getFrameTime(frame), playbackFirstTime, playbackLastTime));
        }
    }

    public static void nextFrame() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            syncTime(JHVTime.clamp(layer.getView().getHigherTime(lastTimestamp), playbackFirstTime, playbackLastTime));
        }
    }

    public static void previousFrame() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            syncTime(JHVTime.clamp(layer.getView().getLowerTime(lastTimestamp), playbackFirstTime, playbackLastTime));
        }
    }

    private static JHVTime lastTimestamp = TimeUtils.START;
    private static long movieStart = TimeUtils.START.milli;
    private static long movieEnd = TimeUtils.START.milli;

    public static JHVTime getTime() {
        return lastTimestamp;
    }

    private static void syncTime(JHVTime dateTime) {
        if (recording && notDone)
            return;

        lastTimestamp = dateTime;
        Display.getCamera().timeChanged(dateTime);

        Layers.setImageLayersNearestFrame(dateTime);
        MovieDisplay.render(1);

        timeListeners.forEach(listener -> listener.timeChanged(lastTimestamp.milli));

        View view = Layers.getActiveImageLayer().getView(); // should be not null
        int activeFrame = view.getCurrentFrameNumber();
        boolean last = view.getFrameTime(activeFrame).equals(playbackLastTime);

        frameListeners.forEach(listener -> listener.frameChanged(activeFrame, last));

        ViewerState.setMovieActiveFrame(activeFrame);

        if (recording)
            notDone = true;
    }

    private static final ArrayList<Listener> frameListeners = new ArrayList<>();
    private static final ArrayList<TimeListener.Change> timeListeners = new ArrayList<>();
    private static final ArrayList<TimeListener.Range> timeRangeListeners = new ArrayList<>();

    public static void addFrameListener(Listener listener) {
        if (!frameListeners.contains(listener))
            frameListeners.add(listener);
    }

    public static void removeFrameListener(Listener listener) {
        frameListeners.remove(listener);
    }

    public static void addTimeListener(TimeListener.Change listener) {
        if (!timeListeners.contains(listener)) {
            timeListeners.add(listener);
            listener.timeChanged(lastTimestamp.milli);
        }
    }

    public static void removeTimeListener(TimeListener.Change listener) {
        timeListeners.remove(listener);
    }

    public static void addTimeRangeListener(TimeListener.Range listener) {
        if (!timeRangeListeners.contains(listener)) {
            timeRangeListeners.add(listener);
            listener.timeRangeChanged(movieStart, movieEnd);
        }
    }

    public static void removeTimeRangeListener(TimeListener.Range listener) {
        timeRangeListeners.remove(listener);
    }

    public static void setDesiredRelativeSpeed(int fps) {
        for (ActionListener listener : movieTimer.getActionListeners())
            movieTimer.removeActionListener(listener);
        movieTimer.addActionListener(relativeTimeAdvanceListener);
        movieTimer.setDelay(1000 / fps);
        deltaT = 0;
    }

    public static void setDesiredAbsoluteSpeed(int sec) {
        for (ActionListener listener : movieTimer.getActionListeners())
            movieTimer.removeActionListener(listener);
        movieTimer.addActionListener(absoluteTimeAdvanceListener);
        movieTimer.setDelay(1000 / FPS_ABSOLUTE);
        deltaT = 1000 / FPS_ABSOLUTE * sec;
    }

    public static void setAdvanceMode(AdvanceMode mode) {
        currentAdvanceMode = mode;
    }

    private static void setCurrentAdvanceMode(AdvanceMode mode) {
        currentAdvanceMode = mode;
    }

    private static boolean recording;
    private static boolean notDone;

    public static void grabDone() {
        notDone = false;
    }

    public static void startRecording() {
        recording = true;
        ViewerState.movieRecordingChanged();
    }

    public static void stopRecording() {
        recording = false;
        ViewerState.movieRecordingChanged();
    }

    public static boolean isRecording() {
        return recording;
    }

}
