package org.helioviewer.jhv.movie;

import java.util.ArrayList;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.layers.Frames;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.thread.EDTTimer;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.time.TimeUtils;

public class Player {

    public enum AdvanceMode {
        Loop, Stop, Swing, SwingDown
    }

    public interface Listener {
        void frameChanged(int frame, boolean last);
    }

    public interface StatusListener {
        void movieStatusChanged();
    }

    public interface PlaybackRangeListener {
        void playbackRangeChanged();
    }

    public static final int FPS_RELATIVE_DEFAULT = 20;
    public static final int FPS_ABSOLUTE = 30;

    private static int playbackFirstFrame;
    private static int playbackLastFrame;
    private static JHVTime playbackFirstTime = TimeUtils.START;
    private static JHVTime playbackLastTime = TimeUtils.START;
    private static AdvanceMode advanceMode = AdvanceMode.Loop;

    @Nullable
    private static JHVTime nextTime(AdvanceMode mode, JHVTime time,
                                    Function<JHVTime, JHVTime> lowerTime, Function<JHVTime, JHVTime> higherTime) {
        if (playbackFirstTime.milli == playbackLastTime.milli)
            return null;
        JHVTime next = mode == AdvanceMode.SwingDown ? lowerTime.apply(time) : higherTime.apply(time);
        if (next.milli == time.milli) { // already at the edges
            switch (mode) {
                case Loop -> {
                    if (next.milli == playbackLastTime.milli) {
                        return playbackFirstTime;
                    }
                }
                case Stop -> {
                    if (next.milli == playbackLastTime.milli) {
                        return null;
                    }
                }
                case Swing -> {
                    if (next.milli == playbackLastTime.milli) {
                        advanceMode = AdvanceMode.SwingDown;
                        return lowerTime.apply(next);
                    }
                }
                case SwingDown -> {
                    if (next.milli == playbackFirstTime.milli) {
                        advanceMode = AdvanceMode.Swing;
                        return higherTime.apply(next);
                    }
                }
            }
        }
        return next;
    }

    public static void resetToMaster() {
        setPlaybackRange(0, getMaximumFrameNumber());
        syncTime(playbackFirstTime);
        notifyStatusChanged();
        timeRangeChanged();
    }

    public static void setPlaybackRange(int newPlaybackFirstFrame, int newPlaybackLastFrame) {
        int lastFrame = Math.max(0, newPlaybackLastFrame);
        int firstFrame = Math.clamp(newPlaybackFirstFrame, 0, lastFrame);
        if (lastFrame != newPlaybackLastFrame)
            Log.warn("Clamping invalid playback last frame " + newPlaybackLastFrame + " to " + lastFrame);
        if (firstFrame != newPlaybackFirstFrame)
            Log.warn("Clamping invalid playback first frame " + newPlaybackFirstFrame + " to " + firstFrame);

        Frames frames = Layers.getActiveImageLayer().frames();
        int maximum = frames.size() - 1;
        // A replacement timeline can have the same frame count but different times.
        playbackFirstTime = frames.time(Math.clamp(firstFrame, 0, maximum));
        playbackLastTime = frames.time(Math.clamp(lastFrame, 0, maximum));
        if (playbackFirstFrame == firstFrame && playbackLastFrame == lastFrame)
            return;

        playbackFirstFrame = firstFrame;
        playbackLastFrame = lastFrame;
        playbackRangeListeners.forEach(PlaybackRangeListener::playbackRangeChanged);
    }

    public static int getPlaybackFirstFrame() {
        return playbackFirstFrame;
    }

    public static int getPlaybackLastFrame() {
        return playbackLastFrame;
    }

    public static long getStartTime() {
        return movieStart;
    }

    public static long getEndTime() {
        return movieEnd;
    }

    private static void timeRangeChanged() {
        ImageLayer layer = Layers.getActiveImageLayer();
        movieStart = layer.getStartTime();
        movieEnd = layer.getEndTime();
        timeRangeListeners.forEach(listener -> listener.timeRangeChanged(movieStart, movieEnd));
    }

    private static int deltaT;

    private static void relativeTimeAdvance() {
        Frames frames = Layers.getActiveImageLayer().frames();
        JHVTime next = nextTime(advanceMode, lastTimestamp,
                time -> JHVTime.clamp(frames.lower(time), playbackFirstTime, playbackLastTime),
                time -> JHVTime.clamp(frames.higher(time), playbackFirstTime, playbackLastTime));

        if (next == null)
            pause();
        else
            syncTime(next);
    }

    private static void absoluteTimeAdvance() {
        JHVTime next = nextTime(advanceMode, lastTimestamp,
                time -> new JHVTime(Math.clamp(time.milli - deltaT, playbackFirstTime.milli, playbackLastTime.milli)),
                time -> new JHVTime(Math.clamp(time.milli + deltaT, playbackFirstTime.milli, playbackLastTime.milli)));

        if (next == null)
            pause();
        else
            syncTime(next);
    }

    private static final EDTTimer movieTimer = new EDTTimer(1000 / FPS_RELATIVE_DEFAULT, Player::relativeTimeAdvance);

    public static boolean isPlaying() {
        return movieTimer.isRunning();
    }

    public static void play() {
        if (isAvailable()) {
            movieTimer.restart();
            notifyStatusChanged();
        }
    }

    public static void pause() {
        movieTimer.stop();
        notifyStatusChanged();
        DisplayController.render(); /* ! force update for on the fly resolution change */
    }

    public static void toggle() {
        if (isPlaying())
            pause();
        else
            play();
    }

    public static void setTime(JHVTime dateTime) {
        Frames frames = Layers.getActiveImageLayer().frames();
        syncTime(JHVTime.clamp(frames.nearest(dateTime), playbackFirstTime, playbackLastTime));
    }

    public static void setFrame(int frame) {
        Frames frames = Layers.getActiveImageLayer().frames();
        syncTime(JHVTime.clamp(frames.time(frame), playbackFirstTime, playbackLastTime));
    }

    public static void nextFrame() {
        Frames frames = Layers.getActiveImageLayer().frames();
        syncTime(JHVTime.clamp(frames.higher(lastTimestamp), playbackFirstTime, playbackLastTime));
    }

    public static void previousFrame() {
        Frames frames = Layers.getActiveImageLayer().frames();
        syncTime(JHVTime.clamp(frames.lower(lastTimestamp), playbackFirstTime, playbackLastTime));
    }

    private static JHVTime lastTimestamp = TimeUtils.START;
    private static long movieStart = TimeUtils.START.milli;
    private static long movieEnd = TimeUtils.START.milli;

    public static JHVTime getTime() {
        return lastTimestamp;
    }

    public static boolean isAvailable() {
        return Layers.getActiveImageLayer().frames().size() > 1;
    }

    public static int getMaximumFrameNumber() {
        return Layers.getActiveImageLayer().frames().size() - 1;
    }

    private static void notifyStatusChanged() {
        statusListeners.forEach(StatusListener::movieStatusChanged);
    }

    private static void syncTime(JHVTime dateTime) {
        if (!ExportMovie.beginPlaybackFrame())
            return;

        lastTimestamp = dateTime;
        DisplayController.timeChanged(dateTime);

        Layers.setImageLayersNearestFrame(dateTime);
        DisplayController.render();

        timeListeners.forEach(listener -> listener.timeChanged(lastTimestamp.milli));

        Frames frames = Layers.getActiveImageLayer().frames();
        int activeFrame = frames.current();
        boolean last = dateTime.equals(playbackLastTime) && frames.time(activeFrame).equals(playbackLastTime);

        frameListeners.forEach(listener -> listener.frameChanged(activeFrame, last));
        ExportMovie.playbackFrameReady(last);
    }

    private static final ArrayList<PlaybackRangeListener> playbackRangeListeners = new ArrayList<>();
    private static final ArrayList<Listener> frameListeners = new ArrayList<>();
    private static final ArrayList<StatusListener> statusListeners = new ArrayList<>();
    private static final ArrayList<TimeListener.Change> timeListeners = new ArrayList<>();
    private static final ArrayList<TimeListener.Range> timeRangeListeners = new ArrayList<>();

    public static void addPlaybackRangeListener(PlaybackRangeListener listener) {
        if (!playbackRangeListeners.contains(listener)) {
            playbackRangeListeners.add(listener);
            listener.playbackRangeChanged();
        }
    }

    public static void removePlaybackRangeListener(PlaybackRangeListener listener) {
        playbackRangeListeners.remove(listener);
    }

    public static void addFrameListener(Listener listener) {
        if (!frameListeners.contains(listener))
            frameListeners.add(listener);
    }

    public static void removeFrameListener(Listener listener) {
        frameListeners.remove(listener);
    }

    public static void addStatusListener(StatusListener listener) {
        if (!statusListeners.contains(listener)) {
            statusListeners.add(listener);
            listener.movieStatusChanged();
        }
    }

    public static void removeStatusListener(StatusListener listener) {
        statusListeners.remove(listener);
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
        movieTimer.setTask(Player::relativeTimeAdvance);
        movieTimer.setDelay(1000 / fps);
        deltaT = 0;
    }

    public static void setDesiredAbsoluteSpeed(int sec) {
        movieTimer.setTask(Player::absoluteTimeAdvance);
        movieTimer.setDelay(1000 / FPS_ABSOLUTE);
        deltaT = (int) (1000L * sec / FPS_ABSOLUTE);
    }

    public static void setAdvanceMode(AdvanceMode mode) {
        advanceMode = mode;
    }

}
