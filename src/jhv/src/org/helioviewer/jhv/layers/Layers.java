package org.helioviewer.jhv.layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.base.time.ImmutableDateTime;
import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.View.AnimationMode;

public class Layers {

    private static View activeView;
    private static final ArrayList<View> layers = new ArrayList<View>();

    /**
     * Returns the view at a given position within the stack of layers.
     *
     * @param index
     *            Position within the stack of layers
     * @return View at given position
     */
    public static View getLayer(int index) {
        try {
            return layers.get(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns number of layers
     *
     * @return Number of layers
     * @see #getNumberOfVisibleLayer
     */
    public static int getNumLayers() {
        return layers.size();
    }

    /**
     * Return the view associated with the active Layer
     *
     * @return View associated with the active Layer
     */
    public static View getActiveView() {
        return activeView;
    }

    public static void setActiveView(View view) {
        activeView = view;
        setMasterMovie(view);
        fireActiveLayerChanged(view);
    }

    private static NextFrameCandidateChooser nextFrameCandidateChooser = new NextFrameCandidateLoopChooser();
    private static FrameChooser frameChooser = new RelativeFrameChooser();

    private static final Timer frameTimer = new Timer(1000 / 20, new FrameListener());

    private static class FrameListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            setFrame(frameChooser.moveToNextFrame(activeView.getCurrentFrameNumber()));
        }
    }

    private static void setMasterMovie(View view) {
        if (view == null || !view.isMultiFrame()) {
            pauseMovie();
            MoviePanel.unsetMovie();
        } else
            MoviePanel.setMovie(view);

        nextFrameCandidateChooser.setMaxFrame();
    }

    public static boolean isMoviePlaying() {
        return frameTimer.isRunning();
    }

    public static void playMovie() {
        if (activeView != null && activeView.isMultiFrame()) {
            frameTimer.restart();
            MoviePanel.setPlayState(true);
        }
    }

    public static void pauseMovie() {
        frameTimer.stop();
        MoviePanel.setPlayState(false);
        Displayer.render(); /*! force update for on the fly resolution change */
    }

    public static void toggleMovie() {
        if (isMoviePlaying()) {
            pauseMovie();
        } else {
            playMovie();
        }
    }

    public static void setTime(ImmutableDateTime dateTime) {
        if (activeView == null || !activeView.isMultiFrame())
            return;

        syncTime(activeView.getFrameDateTime(activeView.getFrame(dateTime)));
    }

    public static void setFrame(int frame) {
        if (activeView == null || !activeView.isMultiFrame())
            return;

        syncTime(activeView.getFrameDateTime(frame));
    }

    public static void nextFrame() {
        if (activeView != null) {
            setFrame(activeView.getCurrentFrameNumber() + 1);
        }
    }

    public static void previousFrame() {
        if (activeView != null) {
            setFrame(activeView.getCurrentFrameNumber() - 1);
        }
    }

    private static Date lastTimestamp = TimeUtils.epoch.getDate();

    public static Date getLastUpdatedTimestamp() {
        return lastTimestamp;
    }

    private static void syncTime(ImmutableDateTime dateTime) {
        lastTimestamp = dateTime.getDate();

        Displayer.getViewport().getCamera().timeChanged(lastTimestamp);
        for (final TimeListener listener : timeListeners) {
            listener.timeChanged(lastTimestamp);
        }

        for (View view : layers) {
            if (view == activeView || view.getImageLayer().isVisible()) {
                view.setFrame(view.getFrame(dateTime), lastTimestamp);
            }
        }
        MoviePanel.getSingletonInstance().setFrameSlider(activeView.getCurrentFrameNumber());
    }

    private static ImmutableDateTime getStartDateImmutable(View view) {
        return view.getFrameDateTime(0);
    }

    private static ImmutableDateTime getEndDateImmutable(View view) {
        return view.getFrameDateTime(view.getMaximumFrameNumber());
    }

    private static ImmutableDateTime getStartDateImmutable(int idx) {
        return getStartDateImmutable(getLayer(idx));
    }

    private static ImmutableDateTime getEndDateImmutable(int idx) {
        return getEndDateImmutable(getLayer(idx));
    }

    /**
     * Return the timestamp of the first available image data of the layer in
     * question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return timestamp of the first available image data
     */
    public static Date getStartDate(View view) {
        return getStartDateImmutable(view).getDate();
    }

    /**
     * Return the timestamp of the last available image data of the layer in
     * question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return timestamp of the last available image data
     */
    public static Date getEndDate(View view) {
        return getEndDateImmutable(view).getDate();
    }

    /**
     * Return the timestamp of the first available image data
     *
     * @return timestamp of the first available image data, null if no
     *         information available
     */
    public static Date getFirstDate() {
        ImmutableDateTime earliest = null;

        int size = layers.size();
        for (int idx = 0; idx < size; idx++) {
            ImmutableDateTime start = getStartDateImmutable(idx);
            if (earliest == null || start.compareTo(earliest) < 0) {
                earliest = start;
            }
        }
        return earliest == null ? null : earliest.getDate();
    }

    /**
     * Return the timestamp of the last available image data
     *
     * @return timestamp of the last available image data, null if no
     *         information available
     */
    public static Date getLastDate() {
        ImmutableDateTime latest = null;

        int size = layers.size();
        for (int idx = 0; idx < size; idx++) {
            ImmutableDateTime end = getEndDateImmutable(idx);
            if (latest == null || end.compareTo(latest) > 0) {
                latest = end;
            }
        }
        return latest == null ? null : latest.getDate();
    }

    /**
     * Check if the given index is valid, given the current state of the
     * ViewChain
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the index is valid
     */
    private static boolean isValidIndex(int idx) {
        if (idx >= 0 && idx < layers.size()) {
            return true;
        }
        return false;
    }

    /**
     * Calculate a new activeLayer after the old Layer has been deleted
     *
     * @param oldActiveLayerIdx
     *            - index of old active, but deleted, layer
     * @return the index of the new active layer to choose, or -1 if no suitable
     *         new layer can be found
     */
    private static int determineNewActiveLayer(int oldActiveLayerIdx) {
        int candidate = oldActiveLayerIdx;
        if (!isValidIndex(candidate)) {
            candidate = layers.size() - 1;
        }

        return candidate;
    }

    /**
     * Remove the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public static void removeLayer(View view) {
        int index = layers.indexOf(view);

        Displayer.removeRenderListener(view);
        view.removeDataHandler();

        layers.remove(view);
        setActiveView(getLayer(determineNewActiveLayer(index)));

        view.abolish();
    }

    public static void addLayer(View view) {
        layers.add(view);
        fireLayerAdded(view);
        setActiveView(view);

        view.setDataHandler(Displayer.displayDataHandler);
        Displayer.addRenderListener(view);
        Layers.setFrame(0); // sync layers
    }

    private static void fireLayerAdded(View view) {
        for (LayersListener ll : layerListeners) {
            ll.layerAdded(view);
        }
    }

    private static void fireActiveLayerChanged(View view) {
        for (LayersListener ll : layerListeners) {
            ll.activeLayerChanged(view);
        }
    }

    private static final HashSet<LayersListener> layerListeners = new HashSet<LayersListener>();
    private static final HashSet<TimeListener> timeListeners = new HashSet<TimeListener>();

    public static void addLayersListener(LayersListener layerListener) {
        layerListeners.add(layerListener);
    }

    public static void removeLayersListener(LayersListener layerListener) {
        layerListeners.remove(layerListener);
    }

    public static void addTimeListener(final TimeListener timeListener) {
        timeListeners.add(timeListener);
    }

    public static void removeTimeListener(final TimeListener timeListener) {
        timeListeners.remove(timeListener);
    }

    public static void setDesiredRelativeSpeed(int fps) {
        frameTimer.setDelay(1000 / fps);
    }

    public void setDesiredAbsoluteSpeed(int secondsPerSecond) {
        /*
        long[] obsMillis = new long[parentImageRef.getMaximumFrameNumber() + 1];
        for (int i = 0; i <= parentImageRef.getMaximumFrameNumber(); ++i) {
            obsMillis[i] = parentImageRef.metaDataList[i].getDateObs().getMillis() / secondsPerSecond;
        }

        frameChooser = new AbsoluteFrameChooser(obsMillis);
        ((AbsoluteFrameChooser) frameChooser).resetStartTime(currParams.compositionLayer);
        */
    }

    public static void setAnimationMode(AnimationMode mode) {
        switch (mode) {
        case LOOP:
            nextFrameCandidateChooser = new NextFrameCandidateLoopChooser();
            break;
        case STOP:
            nextFrameCandidateChooser = new NextFrameCandidateStopChooser();
            break;
        case SWING:
            nextFrameCandidateChooser = new NextFrameCandidateSwingChooser();
            break;
        }
        nextFrameCandidateChooser.setMaxFrame();
    }

    private static abstract class NextFrameCandidateChooser {
        protected int maxFrame;

        protected void setMaxFrame() {
            if (activeView == null)
                maxFrame = 0;
            else
                maxFrame = activeView.getMaximumFrameNumber();
        }

        protected void resetStartTime(int frame) {
            if (frameChooser instanceof AbsoluteFrameChooser) {
                ((AbsoluteFrameChooser) frameChooser).resetStartTime(frame);
            }
        }

        public abstract int getNextCandidate(int lastCandidate);
    }

    private static class NextFrameCandidateLoopChooser extends NextFrameCandidateChooser {
        @Override
        public int getNextCandidate(int lastCandidate) {
            if (++lastCandidate > maxFrame) {
                System.gc();
                resetStartTime(0);
                return 0;
            }
            return lastCandidate;
        }
    }

    private static class NextFrameCandidateStopChooser extends NextFrameCandidateChooser {
        @Override
        public int getNextCandidate(int lastCandidate) {
            if (++lastCandidate > maxFrame) {
                pauseMovie();
                resetStartTime(0);
                return 0;
            }
            return lastCandidate;
        }
    }

    private static class NextFrameCandidateSwingChooser extends NextFrameCandidateChooser {
        private int currentDirection = 1;

        @Override
        public int getNextCandidate(int lastCandidate) {
            lastCandidate += currentDirection;
            if (lastCandidate < 0 && currentDirection == -1) {
                currentDirection = 1;
                resetStartTime(0);
                return 1;
            } else if (lastCandidate > maxFrame && currentDirection == 1) {
                currentDirection = -1;
                resetStartTime(maxFrame);
                return maxFrame - 1;
            }

            return lastCandidate;
        }
    }

    private interface FrameChooser {
        public int moveToNextFrame(int frameNumber);
    }

    private static class RelativeFrameChooser implements FrameChooser {
        @Override
        public int moveToNextFrame(int frame) {
            return nextFrameCandidateChooser.getNextCandidate(frame);
        }
    }

    private static class AbsoluteFrameChooser implements FrameChooser {

        private final long[] obsMillis;
        private long absoluteStartTime;
        private long systemStartTime;

        public AbsoluteFrameChooser(long[] _obsMillis) {
            obsMillis = _obsMillis;
        }

        public void resetStartTime(int frame) {
            absoluteStartTime = obsMillis[frame];
            systemStartTime = System.currentTimeMillis();
        }

        @Override
        public int moveToNextFrame(int frame) {
            int lastCandidate, nextCandidate = frame;
            long lastDiff, nextDiff = -Long.MAX_VALUE;

            do {
                lastCandidate = nextCandidate;
                nextCandidate = nextFrameCandidateChooser.getNextCandidate(nextCandidate);

                lastDiff = nextDiff;
                nextDiff = Math.abs(obsMillis[nextCandidate] - absoluteStartTime) - (System.currentTimeMillis() - systemStartTime);
            } while (nextDiff < 0);

            if (-lastDiff < nextDiff) {
                return lastCandidate;
                // return lastDiff;
            } else {
                return nextCandidate;
                // return nextDiff;
            }
        }
    }

}
