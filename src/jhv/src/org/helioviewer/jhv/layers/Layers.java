package org.helioviewer.jhv.layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.View.AnimationMode;

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
        if (view != activeView) {
            activeView = view;
            setMasterMovie(view);
            fireActiveLayerChanged(view);
        }
    }

    private static final Timer frameTimer = new Timer(1000 / 20, new FrameTimerListener());

    private static class FrameTimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (activeView != null) {
                JHVDate nextTime = activeView.getNextTime(animationMode);
                if (nextTime == null)
                    pauseMovie();
                else
                    setTime(nextTime);
            }
        }
    }

    private static void setMasterMovie(View view) {
        if (view == null || !view.isMultiFrame()) {
            if (view != null && !view.isMultiFrame()) {
                setTime(view.getFirstTime());
            }

            pauseMovie();
            MoviePanel.unsetMovie();
        } else
            MoviePanel.setMovie(view);
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
        Displayer.render(1); /* ! force update for on the fly resolution change */
    }

    public static void toggleMovie() {
        if (isMoviePlaying()) {
            pauseMovie();
        } else {
            playMovie();
        }
    }

    public static void setTime(JHVDate dateTime) {
        if (activeView != null) {
            syncTime(activeView.getFrameTime(dateTime));
        }
    }

    public static void setFrame(int frame) {
        if (activeView != null) {
            syncTime(activeView.getFrameTime(frame));
        }
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

    private static JHVDate lastTimestamp = TimeUtils.Epoch;

    public static JHVDate getLastUpdatedTimestamp() {
        return lastTimestamp;
    }

    private static void syncTime(JHVDate dateTime) {
        lastTimestamp = dateTime;

        Camera camera = Displayer.getCamera();
        camera.timeChanged(lastTimestamp);
        for (View view : layers) {
            view.setFrame(dateTime);
        }
        Displayer.render(1);

        ImageViewerGui.getRenderableViewpoint().fireTimeUpdated();
        for (TimeListener listener : timeListeners) {
            listener.timeChanged(lastTimestamp);
        }

        int activeFrame = activeView.getCurrentFrameNumber();
        for (FrameListener listener : frameListeners) {
            listener.frameChanged(activeFrame);
        }

        MoviePanel.setFrameSlider(activeFrame);
    }

    /**
     * Return the timestamp of the first available image data
     *
     * @return timestamp of the first available image data, null if no
     *         information available
     */
    public static JHVDate getStartDate() {
        JHVDate earliest = null;

        for (View view : layers) {
            JHVDate start = view.getFirstTime();
            if (earliest == null || start.compareTo(earliest) < 0) {
                earliest = start;
            }
        }
        return earliest;
    }

    /**
     * Return the timestamp of the last available image data
     *
     * @return timestamp of the last available image data, null if no
     *         information available
     */
    public static JHVDate getEndDate() {
        JHVDate latest = null;

        for (View view : layers) {
            JHVDate end = view.getLastTime();
            if (latest == null || end.compareTo(latest) > 0) {
                latest = end;
            }
        }
        return latest;
    }

   /**
     * Check if the given index is valid
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

        layers.remove(view);
        if (view == activeView) {
            setActiveView(getLayer(determineNewActiveLayer(index)));
        }
        view.abolish();
    }

    public static void addLayer(View view) {
        layers.add(view);
        fireLayerAdded(view);
        setActiveView(view);
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

    private static final HashSet<FrameListener> frameListeners = new HashSet<FrameListener>();
    private static final HashSet<LayersListener> layerListeners = new HashSet<LayersListener>();
    private static final HashSet<TimeListener> timeListeners = new HashSet<TimeListener>();

    public static void addFrameListener(FrameListener frameListener) {
        frameListeners.add(frameListener);
    }

    public static void removeFrameListener(FrameListener frameListener) {
        frameListeners.remove(frameListener);
    }

    public static void addLayersListener(LayersListener layerListener) {
        layerListeners.add(layerListener);
    }

    public static void removeLayersListener(LayersListener layerListener) {
        layerListeners.remove(layerListener);
    }

    public static JHVDate addTimeListener(TimeListener timeListener) {
        timeListeners.add(timeListener);
        return lastTimestamp;
    }

    public static void removeTimeListener(TimeListener timeListener) {
        timeListeners.remove(timeListener);
    }

    public static void setDesiredRelativeSpeed(int fps) {
        frameTimer.setDelay(1000 / fps);
    }

    private static AnimationMode animationMode = AnimationMode.LOOP;

    public static void setAnimationMode(AnimationMode mode) {
        animationMode = mode;
    }

    public static double getLargestPhysicalSize() {
        double newSize, size = 0;

        for (View v : layers) {
            MetaData m;
            ImageData d = v.getImageLayer().getImageData();
            if (d == null) // not yet decoded
                m = v.getMetaData(new JHVDate(0));
            else
                m = d.getMetaData();

            newSize = m.getPhysicalRegion().height;
            if (newSize > size) {
                size = newSize;
            }
        }
        return size;
    }

}
