package org.helioviewer.jhv.layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.View.AnimationMode;

public class Movie {

    static void setMaster(ImageLayer layer) {
        View view;
        if (layer == null || !(view = layer.getView()).isMultiFrame()) {
            pause();
            MoviePanel.unsetMovie();
        } else
            MoviePanel.setMovie(view);
    }

    public static long getStartTime() {
        return movieStart;
    }

    public static long getEndTime() {
        return movieEnd;
    }

    private static long getMovieStart() {
        long min = Long.MAX_VALUE;
        for (ImageLayer layer : Layers.getImageLayers()) {
            long t = layer.getView().getFirstTime().milli;
            if (t < min) {
                min = t;
            }
        }
        return min == Long.MAX_VALUE ? lastTimestamp.milli : min;
    }

    private static long getMovieEnd() {
        long max = Long.MIN_VALUE;
        for (ImageLayer layer : Layers.getImageLayers()) {
            long t = layer.getView().getLastTime().milli;
            if (t > max) {
                max = t;
            }
        }
        return max == Long.MIN_VALUE ? lastTimestamp.milli : max;
    }

    static void timespanChanged() {
        movieStart = getMovieStart();
        movieEnd = getMovieEnd();
        for (TimespanListener ll : timespanListeners) {
            ll.timespanChanged(movieStart, movieEnd);
        }
    }

    private static int deltaT = 0;
    private static final Timer frameTimer;

    static {
        frameTimer = new Timer(1000 / 20, new FrameTimerListener());
        frameTimer.setCoalesce(true);
    }

    private static class FrameTimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageLayer layer = Layers.getActiveImageLayer();
            if (layer != null) {
                JHVDate nextTime = layer.getView().getNextTime(animationMode, deltaT);
                if (nextTime == null)
                    pause();
                else
                    setTime(nextTime);
            }
        }
    }

    public static boolean isPlaying() {
        return frameTimer.isRunning();
    }

    public static void play() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null && layer.getView().isMultiFrame()) {
            frameTimer.restart();
            MoviePanel.setPlayState(true);
        }
    }

    public static void pause() {
        frameTimer.stop();
        MoviePanel.setPlayState(false);
        Displayer.render(1); /* ! force update for on the fly resolution change */
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
            syncTime(layer.getView().getFrameTime(dateTime));
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
            setFrame(layer.getView().getCurrentFrameNumber() + 1);
        }
    }

    public static void previousFrame() {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            setFrame(layer.getView().getCurrentFrameNumber() - 1);
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

        Camera camera = Displayer.getCamera();
        camera.timeChanged(lastTimestamp);

        for (ImageLayer layer : Layers.getImageLayers()) {
            layer.getView().setFrame(dateTime);
        }
        Displayer.render(1);

        Layers.getViewpointLayer().fireTimeUpdated(camera.getViewpoint().time); // !
        for (TimeListener listener : timeListeners) {
            listener.timeChanged(lastTimestamp.milli);
        }

        View view = Layers.getActiveImageLayer().getView(); // should be not null
        int activeFrame = view.getCurrentFrameNumber();
        boolean last = activeFrame == view.getMaximumFrameNumber();
        for (FrameListener listener : frameListeners) {
            listener.frameChanged(activeFrame, last);
        }

        MoviePanel.setFrameSlider(activeFrame);

        if (recording)
            notDone = true;
    }

    private static final HashSet<FrameListener> frameListeners = new HashSet<>();
    private static final HashSet<TimeListener> timeListeners = new HashSet<>();
    private static final HashSet<TimespanListener> timespanListeners = new HashSet<>();

    public static void addFrameListener(FrameListener listener) {
        frameListeners.add(listener);
    }

    public static void removeFrameListener(FrameListener listener) {
        frameListeners.remove(listener);
    }

    public static void addTimeListener(TimeListener listener) {
        timeListeners.add(listener);
    }

    public static void removeTimeListener(TimeListener listener) {
        timeListeners.remove(listener);
    }

    public static void addTimespanListener(TimespanListener listener) {
        timespanListeners.add(listener);
    }

    public static void removeTimespanListener(TimespanListener listener) {
        timespanListeners.remove(listener);
    }

    public static void setDesiredRelativeSpeed(int fps) {
        frameTimer.setDelay(1000 / fps);
        deltaT = 0;
    }

    public static void setDesiredAbsoluteSpeed(int sec) {
        frameTimer.setDelay(1000 / 20);
        deltaT = (int) (sec / 20.);
    }

    private static AnimationMode animationMode = AnimationMode.Loop;

    public static void setAnimationMode(AnimationMode mode) {
        animationMode = mode;
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
