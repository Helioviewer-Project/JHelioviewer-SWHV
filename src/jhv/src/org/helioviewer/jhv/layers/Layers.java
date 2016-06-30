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
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.io.APIRequestManager.APIRequest;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.View.AnimationMode;

public class Layers {

    private static View activeView;
    private static final ArrayList<View> layers = new ArrayList<View>();

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

    public static int getNumLayers() {
        return layers.size();
    }

    public static int getNumVisibleLayers() {
        int ct = 0;
        for (View v : layers)
            if (v.getImageLayer().isVisible())
                ct++;
        return ct;
    }

    public static View getActiveView() {
        return activeView;
    }

    public static ImageLayer getImageLayerInViewport(int idx) {
        for (View v : layers) {
            ImageLayer l = v.getImageLayer();
            if (l.isVisible(idx))
                return l;
        }
        return null;
    }

    public static void setRender(Camera camera, double factor) {
        int i;
        for (View v : layers) {
            if ((i = v.getImageLayer().isVisibleIdx()) != -1)
                v.render(camera, Displayer.getViewports()[i], factor);
        }
    }

    public static void syncLayersSpan() {
        if (activeView != null) {
            APIRequest areq = activeView.getImageLayer().getAPIRequest();
            long startTime, endTime;
            int cadence;
            if (areq != null) {
                startTime = areq.startTime;
                endTime = areq.endTime;
                cadence = areq.cadence;
            } else {
                startTime = activeView.getFirstTime().milli;
                endTime = activeView.getLastTime().milli;
                cadence = ObservationDialog.getInstance().getObservationImagePane().getCadence();
            }

            for (View v : layers) {
                APIRequest vreq = v.getAPIRequest();
                if (v != activeView && vreq != null) {
                    v.getImageLayer().load(new APIRequest(vreq.server, vreq.sourceId, startTime, endTime, cadence));
                }
            }
        }
    }

    static void setActiveView(View view) {
        if (view != activeView) {
            activeView = view;
            setMasterMovie(view);
            fireActiveLayerChanged(view);
        }
    }

    static View getLayer(int index) {
        try {
            return layers.get(index);
        } catch (Exception e) {
            return null;
        }
    }

    static void removeLayer(View view) {
        layers.remove(view);
        if (view == activeView) {
            setActiveView(getLayer(layers.size() - 1));
        }
    }

    static void addLayer(View view) {
        layers.add(view);
        fireLayerAdded(view);
        setActiveView(view);
        setFrame(0);
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

    private static int deltaT = 0;
    private static final Timer frameTimer;

    static {
        frameTimer = new Timer(1000 / 20, new FrameTimerListener());
        frameTimer.setCoalesce(true);
    }

    private static class FrameTimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (activeView != null) {
                JHVDate nextTime = activeView.getNextTime(animationMode, deltaT);
                if (nextTime == null)
                    pauseMovie();
                else
                    setTime(nextTime);
            }
        }
    }

    private static void setMasterMovie(View view) {
        if (view == null || !view.isMultiFrame()) {
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

    private static JHVDate lastTimestamp = TimeUtils.EPOCH;

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

        ImageViewerGui.getRenderableViewpoint().fireTimeUpdated(camera.getViewpoint().time); // !
        for (TimeListener listener : timeListeners) {
            listener.timeChanged(lastTimestamp);
        }

        int activeFrame = activeView.getCurrentFrameNumber();
        boolean last = activeFrame == activeView.getMaximumFrameNumber();
        for (FrameListener listener : frameListeners) {
            listener.frameChanged(activeFrame, last);
        }

        MoviePanel.setFrameSlider(activeFrame);
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
        deltaT = 0;
    }

    public static void setDesiredAbsoluteSpeed(int sec) {
        frameTimer.setDelay(1000 / 20);
        deltaT = (int) (sec / 20.);
    }

    private static AnimationMode animationMode = AnimationMode.LOOP;

    public static void setAnimationMode(AnimationMode mode) {
        animationMode = mode;
    }

    public static double getLargestPhysicalHeight() {
        double newSize, size = 0;

        for (View v : layers) {
            if (v.getImageLayer().isVisible()) {
                MetaData m = v.getImageLayer().getMetaData();

                newSize = m.getPhysicalRegion().height;
                if (newSize > size) {
                    size = newSize;
                }
            }
        }
        return size;
    }

    public static double getLargestPhysicalSize() {
        double newSize, size = 0;

        for (View v : layers) {
            if (v.getImageLayer().isVisible()) {
                MetaData m = v.getImageLayer().getMetaData();
                double h = m.getPhysicalRegion().height;
                double w = m.getPhysicalRegion().width;

                newSize = Math.sqrt(h * h + w * w);
                if (newSize > size) {
                    size = newSize;
                }
            }
        }
        return size;
    }

    public static String getSDOCutoutString() {
        StringBuilder str = new StringBuilder();
        for (View v : layers) {
            if (v.getImageLayer().isVisible()) {
                MetaData md = v.getMetaData(Layers.getLastUpdatedTimestamp());
                if (md instanceof HelioviewerMetaData) {
                    HelioviewerMetaData hmd = (HelioviewerMetaData) md;
                    if (hmd.getObservatory().contains("SDO") && hmd.getInstrument().contains("AIA"))
                        str.append(',').append(hmd.getMeasurement());
                }
            }
        }
        return str.toString();
    }

}
