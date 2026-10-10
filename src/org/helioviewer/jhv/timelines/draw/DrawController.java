package org.helioviewer.jhv.timelines.draw;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.event.EventListener;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.thread.UiTimer;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;

import org.json.JSONObject;

public final class DrawController implements EventListener.Highlight, TimeListener.Change {

    public interface Listener {
        default void drawRequest() {}

        default void drawMovieLineRequest() {}

        default void layoutChanged() {}

        // Lock, stacking or status text changed.
        default void optionsChanged() {}
    }

    public static final TimeAxis selectedAxis = new TimeAxis(0, 0);
    public static final TimeAxis availableAxis = new TimeAxis(0, 0);

    private static final ArrayList<Listener> listeners = new ArrayList<>();

    private static final GraphGeometry geometry = new GraphGeometry();
    private static long currentTime;

    private static boolean locked;
    @Nullable
    private static String status;
    private static final UiTimer layersUpdater = new UiTimer(1000 / 2, DrawController::syncLockedLayers);

    static {
        layersUpdater.setRepeats(false);
    }

    private static void syncLockedLayers() {
        layersUpdater.stop();
        long start = TimeUtils.ceilSec(selectedAxis.start());
        long end = TimeUtils.floorSec(selectedAxis.end());
        ImageLayers.syncLayersSpan(start, end);
    }

    public DrawController() {
        long t = System.currentTimeMillis();
        setSelectedInterval(t - 2 * TimeUtils.DAY_IN_MILLIS, t);
    }

    public static void saveState(JSONObject jo) {
        JSONObject js = new JSONObject();
        js.put("startTime", TimeUtils.format(selectedAxis.start()));
        js.put("endTime", TimeUtils.format(selectedAxis.end()));
        jo.put("selectedAxis", js);
        jo.put("locked", locked);
        jo.put("stacked", geometry.isStacked());
    }

    public static void loadState(JSONObject jo) {
        JSONObject js = jo.optJSONObject("selectedAxis");
        if (js != null) {
            long t = System.currentTimeMillis();
            long start = TimeUtils.optParse(js.optString("startTime"), t - 2 * TimeUtils.DAY_IN_MILLIS);
            long end = TimeUtils.optParse(js.optString("endTime"), t);
            setSelectedInterval(start, end);
        }
        setLocked(jo.optBoolean("locked", false));
        setStacked(jo.optBoolean("stacked", false));
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
            timeRangeChanged();
        }
    }

    public static void moveX(double pixelDistance) {
        if (pixelDistance == 0)
            return;

        selectedAxis.move(geometry.graphWidth(), pixelDistance);
        timeRangeChanged();
    }

    public static void moveSelectedInterval(long distance) {
        if (distance == 0)
            return;

        selectedAxis.move(distance);
        timeRangeChanged();
    }

    private static void zoomX(int x, double factor) {
        if (factor == 0)
            return;

        Rectangle graphArea = geometry.area();
        selectedAxis.zoom(graphArea.x, graphArea.width, x, factor);
        timeRangeChanged();
    }

    public static void resetAxis(Point p) {
        if (geometry.isStacked()) {
            GraphGeometry.LayerLayout layout = geometry.getLayerLayout(p);
            if (layout != null)
                layout.layer().resetAxis();
            drawRequest();
            return;
        }

        GraphGeometry.YAxisHit hit = geometry.yAxisHit(p);
        if (hit.outsideAxes()) {
            for (GraphGeometry.LayerLayout layout : geometry.getLayerLayouts())
                layout.layer().zoomToFitAxis();
        } else {
            for (GraphGeometry.LayerLayout layout : geometry.getLayerLayouts()) {
                if (hit.targets(layout.axisIndex()))
                    layout.layer().resetAxis();
            }
        }
        drawRequest();
    }

    public static void moveY(Point p, double distanceY) {
        if (distanceY == 0)
            return;

        if (geometry.isStacked()) {
            GraphGeometry.LayerLayout layout = geometry.getLayerLayout(p);
            if (layout != null)
                moveYAxis(layout, distanceY, layout.area().height);
        } else {
            GraphGeometry.YAxisHit hit = geometry.yAxisHit(p);
            for (GraphGeometry.LayerLayout layout : geometry.getLayerLayouts()) {
                if (hit.outsideAxes() || hit.targets(layout.axisIndex()))
                    moveYAxis(layout, distanceY, geometry.graphHeight());
            }
        }
        drawRequest();
    }

    private static void zoomY(Point p, int scrollDistance) {
        if (scrollDistance == 0)
            return;

        if (geometry.isStacked()) {
            GraphGeometry.LayerLayout layout = geometry.getLayerLayout(p);
            if (layout != null) {
                Rectangle stripArea = layout.area();
                layout.yAxis().zoomSelectedRange(scrollDistance,
                        stripArea.y + stripArea.height - p.y, stripArea.height);
                layout.layer().yaxisChanged();
            }
        } else {
            GraphGeometry.YAxisHit hit = geometry.yAxisHit(p);
            for (GraphGeometry.LayerLayout layout : geometry.getLayerLayouts()) {
                if (hit.outsideAxes() || hit.targets(layout.axisIndex()))
                    zoomYAxis(layout, p, scrollDistance);
            }
        }
        drawRequest();
    }

    private static void moveYAxis(GraphGeometry.LayerLayout layout, double distanceY, int graphHeight) {
        layout.yAxis().shiftDownPixels(distanceY, graphHeight);
        layout.layer().yaxisChanged();
    }

    private static void zoomYAxis(GraphGeometry.LayerLayout layout, Point p, int scrollDistance) {
        layout.yAxis().zoomSelectedRange(scrollDistance, geometry.axisZoomY(p), geometry.graphHeight());
        layout.layer().yaxisChanged();
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
        boolean onYAxis = !geometry.yAxisHit(p).outsideAxes();
        if ((inGraphArea && alt) || (inGraphArea && ctrl) || onYAxis) {
            zoomY(p, scrollDistance);
        }
    }

    private static void timeRangeChanged() {
        if (locked)
            layersUpdater.restart();

        long diff = selectedAxis.end() - selectedAxis.start();
        long availableStart = selectedAxis.start() - diff;
        long availableEnd = selectedAxis.end() + diff;
        availableAxis.set(TimeUtils.floorDay(availableStart), TimeUtils.floorDay(availableEnd) + TimeUtils.DAY_IN_MILLIS);

        TimelineLayers.fetchData(selectedAxis);
        drawRequest();
    }

    public static void setGraphSize(int width, int height) {
        geometry.setSize(width, height);
        layoutChanged();
    }

    public static GraphGeometry getGeometry() {
        return geometry;
    }

    public static boolean isLocked() {
        return locked;
    }

    public static void setLocked(boolean _locked) {
        if (locked == _locked)
            return;
        locked = _locked;
        if (locked) // force sync
            timeRangeChanged();
        listeners.forEach(Listener::optionsChanged);
    }

    public static void setStacked(boolean _stacked) {
        if (geometry.isStacked() == _stacked)
            return;
        geometry.setStacked(_stacked);
        layoutChanged();
        listeners.forEach(Listener::optionsChanged);
    }

    @Nullable
    public static String getStatus() {
        return status;
    }

    public static void setStatus(@Nullable String _status) {
        status = _status;
        listeners.forEach(Listener::optionsChanged);
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
        if (!geometry.inGraph(point))
            return;
        Commands.seekTime(new JHVTime(geometry.xMapper(selectedAxis).toValue(point.x)));
    }

    @Override
    public void highlightChanged() {
        drawRequest();
    }

    public static boolean setYAxisHighlight(@Nullable GraphGeometry.YAxisHit hit) {
        boolean changed = false;
        for (GraphGeometry.LayerLayout layout : geometry.getLayerLayouts()) {
            boolean highlighted = hit != null && hit.targets(layout.axisIndex());
            YAxis yAxis = layout.yAxis();
            changed = changed || yAxis.isHighlighted() != highlighted;
            yAxis.setHighlighted(highlighted);
        }
        return changed;
    }

    public static void layoutChanged() {
        geometry.layout(TimelineLayers.get());
        listeners.forEach(Listener::layoutChanged);
        TimelineLayers.get().forEach(TimelineLayer::graphGeometryChanged);
        drawRequest();
    }

    public static void drawRequest() {
        toDraw = true;
    }

    private static boolean toDraw;
    private static boolean drawMovieLine;
    // Coalesces draw requests while the graph pane is showing.
    private static final UiTimer drawTimer = new UiTimer(1000 / 10, DrawController::flushDrawRequests);

    private static void flushDrawRequests() {
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
        drawTimer.start();
    }

    public static void stop() {
        drawTimer.stop();
    }

}
