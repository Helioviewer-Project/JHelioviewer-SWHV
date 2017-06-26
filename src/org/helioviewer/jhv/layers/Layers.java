package org.helioviewer.jhv.layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.view.View;
import org.helioviewer.jhv.view.View.AnimationMode;
/*
import java.util.HashMap;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.imagedata.ImageData;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;
*/
public class Layers {

    private static View activeView;
    private static final ArrayList<View> layers = new ArrayList<>();

    public static JHVDate getStartDate() {
        JHVDate min = null;
        for (View view : layers) {
            JHVDate d = view.getFirstTime();
            if (min == null || d.milli < min.milli) {
                min = d;
            }
        }

        return min == null ? lastTimestamp : min;
    }

    public static JHVDate getEndDate() {
        JHVDate max = null;
        for (View view : layers) {
            JHVDate d = view.getLastTime();
            if (max == null || d.milli > max.milli) {
                max = d;
            }
        }

        return max == null ? lastTimestamp : max;
    }

    public static int getNumLayers() {
        return layers.size();
    }

    public static int getNumEnabledLayers() {
        int ct = 0;
        for (View v : layers)
            if (v.getImageLayer().isEnabled())
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
                cadence = ObservationDialog.getInstance().getObservationPanel().getCadence();
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
        // fireTimespanChanged();
        if (view == activeView) {
            setActiveView(getLayer(layers.size() - 1));
        }
    }

    static void addLayer(View view) {
        layers.add(view);
        fireTimespanChanged();
        setActiveView(view);
        setFrame(0);
    }

    private static void fireTimespanChanged() {
        long start = getStartDate().milli;
        long end = getEndDate().milli;
        for (TimespanListener ll : timespanListeners) {
            ll.timespanChanged(start, end);
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

        ImageViewerGui.getRenderableContainer().getRenderableViewpoint().fireTimeUpdated(camera.getViewpoint().time); // !
        for (TimeListener listener : timeListeners) {
            listener.timeChanged(lastTimestamp.milli);
        }

        int activeFrame = activeView.getCurrentFrameNumber();
        boolean last = activeFrame == activeView.getMaximumFrameNumber();
        for (FrameListener listener : frameListeners) {
            listener.frameChanged(activeFrame, last);
        }

        MoviePanel.setFrameSlider(activeFrame);
    }

    private static final HashSet<FrameListener> frameListeners = new HashSet<>();
    private static final HashSet<LayersListener> layerListeners = new HashSet<>();
    private static final HashSet<TimeListener> timeListeners = new HashSet<>();
    private static final HashSet<TimespanListener> timespanListeners = new HashSet<>();

    public static void addFrameListener(FrameListener listener) {
        frameListeners.add(listener);
    }

    public static void removeFrameListener(FrameListener listener) {
        frameListeners.remove(listener);
    }

    public static void addLayersListener(LayersListener listener) {
        layerListeners.add(listener);
    }

    public static void removeLayersListener(LayersListener listener) {
        layerListeners.remove(listener);
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

    public static double getLargestPhysicalHeight() {
        double size = 0;

        for (View v : layers) {
            if (v.getImageLayer().isEnabled()) {
                MetaData m = v.getImageLayer().getMetaData();
                double newSize = m.getPhysicalRegion().height;
                if (newSize > size) {
                    size = newSize;
                }
            }
        }
        return size;
    }

    public static double getLargestPhysicalSize() {
        double size = 0;

        for (View v : layers) {
            if (v.getImageLayer().isEnabled()) {
                MetaData m = v.getImageLayer().getMetaData();
                double h = m.getPhysicalRegion().height;
                double w = m.getPhysicalRegion().width;
                double newSize = Math.sqrt(h * h + w * w);
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
            if (v.getImageLayer().isEnabled()) {
                MetaData m = v.getImageLayer().getMetaData();
                if (m instanceof HelioviewerMetaData) {
                    HelioviewerMetaData hm = (HelioviewerMetaData) m;
                    if (hm.getObservatory().contains("SDO") && hm.getInstrument().contains("AIA"))
                        str.append(',').append(hm.getMeasurement());
                }
            }
        }
        return str.toString();
    }
/*
    public static void getSAMPMessage(Message msg) {
        if (activeView == null)
            return;

        if (!activeView.getImageLayer().isEnabled() || activeView.getImageLayer().getAPIRequest() == null || activeView.getImageLayer().getImageData() == null)
            return;

        ImageData id = activeView.getImageLayer().getImageData();
        MetaData m = id.getMetaData();
        if (!(m instanceof HelioviewerMetaData))
            return;
        HelioviewerMetaData hm = (HelioviewerMetaData) m;

        msg.addParam("timestamp", hm.getViewpoint().time.toString().replace('-', '/') + ".000");
        msg.addParam("start", activeView.getFirstTime().toString().replace('-', '/') + ".000");
        msg.addParam("end", activeView.getFirstTime().toString().replace('-', '/') + ".000");
        msg.addParam("cadence", SampUtils.encodeLong(activeView.getImageLayer().getAPIRequest().cadence * 1000L));
        msg.addParam("cutout.set", SampUtils.encodeBoolean(true));

        Region region = Region.scale(id.getRegion(), 1 / id.getMetaData().getUnitPerArcsec());
        msg.addParam("cutout.x0", SampUtils.encodeFloat(region.llx + region.width / 2.));
        msg.addParam("cutout.y0", SampUtils.encodeFloat(-(region.lly + region.height / 2.)));
        msg.addParam("cutout.w", SampUtils.encodeFloat(region.width));
        msg.addParam("cutout.h", SampUtils.encodeFloat(region.height));

        ArrayList<HashMap<String, String>> layersData = new ArrayList<>();

        for (View v : layers) {
            if (v.getImageLayer().isEnabled()) {
                id = v.getImageLayer().getImageData();
                if (id == null)
                    continue;

                m = id.getMetaData();
                if (m instanceof HelioviewerMetaData) {
                    hm = (HelioviewerMetaData) m;

                    HashMap<String, String> layerMsg = new HashMap<>();
                    layerMsg.put("observatory", hm.getObservatory());
                    layerMsg.put("instrument", hm.getInstrument());
                    layerMsg.put("detector", hm.getDetector());
                    layerMsg.put("measurement", hm.getMeasurement());
                    layerMsg.put("timestamp", hm.getViewpoint().time.toString().replace('-', '/') + ".000");
                    layersData.add(layerMsg);
                }
            }
        }
        msg.addParam("layers", layersData);
    }
*/
}
