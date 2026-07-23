package org.helioviewer.jhv.timelines.draw;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.event.JHVEventListener;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.thread.EDTTimer;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;

import org.json.JSONObject;

public final class DrawController implements Interfaces.LazyComponent, Interfaces.StatusReceiver, JHVEventListener.Highlight, TimeListener.Change {

    public interface Listener {
        void drawRequest();

        void drawMovieLineRequest();
    }

    public static final TimeAxis selectedAxis = new TimeAxis(0, 0);
    public static final TimeAxis availableAxis = new TimeAxis(0, 0);

    private static final DrawControllerOptions optionsPanel = new DrawControllerOptions();
    private static final ArrayList<Listener> listeners = new ArrayList<>();

    private static final GraphGeometry geometry = new GraphGeometry();
    private static long currentTime;

    private static boolean locked;
    private static final EDTTimer layersUpdater = new EDTTimer(1000 / 2, DrawController::syncLockedLayers);

    static {
        layersUpdater.setRepeats(false);
    }

    private static void syncLockedLayers() {
        layersUpdater.stop();
        long start = TimeUtils.ceilSec(selectedAxis.start());
        long end = TimeUtils.floorSec(selectedAxis.end());
        MainFrame.getLayersSectionPanel().syncLayersSpan(start, end);
    }

    public DrawController() {
        long t = System.currentTimeMillis();
        setSelectedInterval(t - 2 * TimeUtils.DAY_IN_MILLIS, t);
        UITimer.register(this);
    }

    public static void saveState(JSONObject jo) {
        JSONObject js = new JSONObject();
        js.put("startTime", TimeUtils.format(selectedAxis.start()));
        js.put("endTime", TimeUtils.format(selectedAxis.end()));
        jo.put("selectedAxis", js);
        jo.put("locked", locked);
    }

    public static void loadState(JSONObject jo) {
        JSONObject js = jo.optJSONObject("selectedAxis");
        if (js != null) {
            long t = System.currentTimeMillis();
            long start = TimeUtils.optParse(js.optString("startTime"), t - 2 * TimeUtils.DAY_IN_MILLIS);
            long end = TimeUtils.optParse(js.optString("endTime"), t);
            setSelectedInterval(start, end);
        }
        optionsPanel.setLocked(jo.optBoolean("locked", false));
    }

    public static JPanel getOptionsPanel() {
        return optionsPanel;
    }

    public static void addDrawListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public static void removeDrawListener(Listener listener) {
        listeners.remove(listener);
    }

    public static void setSelectedInterval(long start, long end) {
        if (start != selectedAxis.start() || end != selectedAxis.end()) {
            selectedAxis.set(start, end);
            setAvailableInterval();
        }
    }

    public static void moveX(double pixelDistance) {
        if (pixelDistance == 0)
            return;

        selectedAxis.move(geometry.graphWidth(), pixelDistance);
        setAvailableInterval();
    }

    public static void moveXAvailableBased(int x0, int x1) {
        if (x0 == x1)
            return;

        TimeAxis.Mapper mapper = availableAxis.mapper(0, geometry.size().width);
        long av_diff = mapper.toValue(x1) - mapper.toValue(x0);
        selectedAxis.move(av_diff);
        setAvailableInterval();
    }

    private static void zoomX(int x, double factor) {
        if (factor == 0)
            return;

        Rectangle graphArea = geometry.area();
        selectedAxis.zoom(graphArea.x, graphArea.width, x, factor);
        setAvailableInterval();
    }

    public static void resetAxis(Point p) {
        GraphGeometry.YAxisHit hit = geometry.yAxisHit(p);
        if (hit.outsideAxes()) {
            TimelineLayers.forEachYAxis((tl, axisIndex) -> tl.zoomToFitAxis());
        } else {
            TimelineLayers.forEachYAxis((tl, axisIndex) -> {
                if (hit.targets(axisIndex))
                    tl.resetAxis();
            });
        }
        drawRequest();
    }

    public static void moveY(Point p, double distanceY) {
        if (distanceY == 0)
            return;

        GraphGeometry.YAxisHit hit = geometry.yAxisHit(p);
        if (hit.outsideAxes()) {
            TimelineLayers.forEachYAxis((tl, axisIndex) -> moveYAxis(tl, distanceY));
        } else {
            TimelineLayers.forEachYAxis((tl, axisIndex) -> {
                if (hit.targets(axisIndex))
                    moveYAxis(tl, distanceY);
            });
        }
        drawRequest();
    }

    private static void zoomY(Point p, int scrollDistance) {
        if (scrollDistance == 0)
            return;

        GraphGeometry.YAxisHit hit = geometry.yAxisHit(p);
        if (hit.outsideAxes()) {
            TimelineLayers.forEachYAxis((tl, axisIndex) -> zoomYAxis(tl, p, scrollDistance));
        } else {
            TimelineLayers.forEachYAxis((tl, axisIndex) -> {
                if (hit.targets(axisIndex))
                    zoomYAxis(tl, p, scrollDistance);
            });
        }
        drawRequest();
    }

    private static void moveYAxis(TimelineLayer tl, double distanceY) {
        tl.getYAxis().shiftDownPixels(distanceY, geometry.graphHeight());
        tl.yaxisChanged();
    }

    private static void zoomYAxis(TimelineLayer tl, Point p, int scrollDistance) {
        tl.getYAxis().zoomSelectedRange(scrollDistance, geometry.axisZoomY(p), geometry.graphHeight());
        tl.yaxisChanged();
    }

    public static void zoomXY(Point p, int scrollDistance, boolean shift, boolean alt, boolean ctrl) {
        boolean inGraphArea = geometry.inGraph(p);
        boolean inXAxisOrAboveGraph = geometry.inXAxisOrAboveGraph(p);

        if (inGraphArea || inXAxisOrAboveGraph) {
            double zoomTimeFactor = 10;
            if ((!alt && !shift) || inXAxisOrAboveGraph) {
                zoomX(p.x, zoomTimeFactor * scrollDistance);
            } else if (shift) {
                moveX(zoomTimeFactor * scrollDistance);
            }
        }
        if ((inGraphArea && alt) || (inGraphArea && ctrl) || !inGraphArea) {
            zoomY(p, scrollDistance);
        }
    }

    public static void moveAllAxes(double distanceY) {
        if (distanceY == 0)
            return;

        TimelineLayers.forEachYAxis((tl, axisIndex) -> {
            tl.getYAxis().shiftDownPixels(distanceY, geometry.graphHeight());
            tl.yaxisChanged();
        });
        drawRequest();
    }

    private static void setAvailableInterval() {
        if (locked)
            layersUpdater.restart();

        long diff = selectedAxis.end() - selectedAxis.start();
        long availableStart = selectedAxis.start() - diff;
        long availableEnd = selectedAxis.end() + diff;
        availableAxis.set(TimeUtils.floorDay(availableStart), TimeUtils.floorDay(availableEnd) + TimeUtils.DAY_IN_MILLIS);

        TimelineLayers.fetchData(selectedAxis);
        drawRequest();
    }

    public static void setGraphSize(Rectangle _graphSize) {
        geometry.setSize(_graphSize);
        graphAreaChanged();
    }

    public static GraphGeometry getGeometry() {
        return geometry;
    }

    static void setLocked(boolean _locked) {
        locked = _locked;
        if (locked) // force sync
            setAvailableInterval();
    }

    @Override
    public void timeChanged(long milli) {
        currentTime = milli;
        drawMovieLine = true;
    }

    public static int getMovieLinePosition() {
        int movieLinePosition = geometry.xMapper(selectedAxis).toPixel(currentTime);
        if (movieLinePosition < geometry.area().x || movieLinePosition > geometry.graphRight()) {
            return -1;
        }
        return movieLinePosition;
    }

    public static void setMovieFrame(Point point) {
        if (!geometry.area().contains(point))
            return;
        Commands.seekTime(new JHVTime(geometry.xMapper(selectedAxis).toValue(point.x)));
    }

    @Override
    public void highlightChanged() {
        drawRequest();
    }

    public static void graphAreaChanged() {
        geometry.layout(Math.max(0, TimelineLayers.getNumberOfPropagationAxes()), TimelineLayers.getNumberOfYAxes());
        setAvailableInterval();
    }

    @Override
    public void setStatus(String status) {
        optionsPanel.setStatus(status);
    }

    public static void drawRequest() {
        toDraw = true;
    }

    private static boolean stopped;
    private static boolean toDraw;
    private static boolean drawMovieLine;

    @Override
    public void lazyRepaint() {
        if (stopped)
            return;

        if (toDraw) {
            toDraw = false;
            listeners.forEach(Listener::drawRequest);
        }
        if (drawMovieLine) {
            drawMovieLine = false;
            listeners.forEach(Listener::drawMovieLineRequest);
        }
    }

    public static void start() {
        stopped = false;
    }

    public static void stop() {
        stopped = true;
    }

}
