package org.helioviewer.jhv.timelines.draw;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.Timer;

import org.helioviewer.jhv.events.JHVEventListener;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.json.JSONObject;

public class DrawController implements LazyComponent, JHVEventListener.Highlight, TimeListener.Change, TimeListener.Range {

    public interface Listener {
        void drawRequest();
        void drawMovieLineRequest();
    }

    public static final TimeAxis selectedAxis = new TimeAxis(0, 0);
    public static final TimeAxis availableAxis = new TimeAxis(0, 0);

    private static final DrawControllerOptionsPanel optionsPanel = new DrawControllerOptionsPanel();
    private static final ArrayList<Listener> listeners = new ArrayList<>();

    private static Rectangle graphArea = new Rectangle();
    private static Rectangle graphSize = new Rectangle();
    private static long currentTime;
    private static boolean locked;

    private static final Timer layersTimer = new Timer(1000 / 2, e -> {
        long start = TimeUtils.ceilSec(selectedAxis.start());
        long end = TimeUtils.floorSec(selectedAxis.end());
        MoviePanel.getInstance().syncLayersSpan(start, end);
    });

    public DrawController() {
        long t = System.currentTimeMillis();
        setSelectedInterval(t - 2 * TimeUtils.DAY_IN_MILLIS, t);
        layersTimer.setRepeats(false);
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

    public static Component getOptionsPanel() {
        return optionsPanel;
    }

    public static void addDrawListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public static void setSelectedInterval(long start, long end) {
        selectedAxis.set(start, end);
        setAvailableInterval();
    }

    public static void moveX(double pixelDistance) {
        selectedAxis.move(graphArea.width, pixelDistance);
        setAvailableInterval();
    }

    public static void moveXAvailableBased(int x0, int x1) {
        long av_diff = availableAxis.pixel2value(0, graphSize.width, x1) - availableAxis.pixel2value(0, graphSize.width, x0);
        selectedAxis.move(av_diff);
        setAvailableInterval();
    }

    private static void zoomX(int x, double factor) {
        selectedAxis.zoom(graphArea.x, graphArea.width, x, factor);
        setAvailableInterval();
    }

    private static void moveAndZoomY(Point p, double distanceY, int scrollDistance, boolean zoom, boolean move) {
        boolean yAxisVerticalCondition = (p.y > graphArea.y && p.y <= graphArea.y + graphArea.height);
        boolean inRightYAxes = p.x > graphArea.x + graphArea.width && yAxisVerticalCondition;
        boolean inLeftYAxis = p.x < graphArea.x && yAxisVerticalCondition;
        int rightYAxisNumber = (p.x - (graphArea.x + graphArea.width)) / DrawConstants.RIGHT_AXIS_WIDTH;
        int ct = -1;
        for (TimelineLayer tl : TimelineLayers.get()) {
            if (tl.showYAxis()) {
                if ((rightYAxisNumber == ct && inRightYAxes) || (ct == -1 && inLeftYAxis)) {
                    if (move) {
                        tl.getYAxis().shiftDownPixels(distanceY, graphArea.height);
                    }
                    if (zoom) {
                        tl.getYAxis().zoomSelectedRange(scrollDistance, graphSize.height - p.y - graphArea.y, graphArea.height);
                    }
                    tl.yaxisChanged();
                } else if ((!inRightYAxes && !inLeftYAxis) && move) {
                    tl.getYAxis().shiftDownPixels(distanceY, graphArea.height);
                    tl.yaxisChanged();
                } else if ((!inRightYAxes && !inLeftYAxis) && zoom) {
                    tl.getYAxis().zoomSelectedRange(scrollDistance, graphSize.height - p.y - graphArea.y, graphArea.height);
                    tl.yaxisChanged();
                }
                ct++;
            }
        }
        drawRequest();
    }

    public static void resetAxis(Point p) {
        boolean yAxisVerticalCondition = p.y > graphArea.y && p.y <= graphArea.y + graphArea.height;
        boolean inRightYAxes = p.x > graphArea.x + graphArea.width && yAxisVerticalCondition;
        boolean inLeftYAxis = p.x < graphArea.x && yAxisVerticalCondition;
        int rightYAxisNumber = (p.x - (graphArea.x + graphArea.width)) / DrawConstants.RIGHT_AXIS_WIDTH;
        int ct = -1;
        for (TimelineLayer tl : TimelineLayers.get()) {
            if (tl.showYAxis()) {
                if ((rightYAxisNumber == ct && inRightYAxes) || (ct == -1 && inLeftYAxis)) {
                    tl.resetAxis();
                } else if (!inRightYAxes && !inLeftYAxis) {
                    tl.zoomToFitAxis();
                }
                ct++;
            }
        }
        drawRequest();
    }

    public static void moveY(Point p, double distanceY) {
        moveAndZoomY(p, distanceY, 0, false, true);
    }

    private static void zoomY(Point p, int scrollDistance) {
        moveAndZoomY(p, 0, scrollDistance, true, false);
    }

    public static void zoomXY(Point p, int scrollDistance, boolean shift, boolean alt, boolean ctrl) {
        boolean inGraphArea = p.x >= graphArea.x && p.x <= graphArea.x + graphArea.width && p.y > graphArea.y && p.y <= graphArea.y + graphArea.height;
        boolean inXAxisOrAboveGraph = p.x >= graphArea.x && p.x <= graphArea.x + graphArea.width && (p.y <= graphArea.y || p.y >= graphArea.y + graphArea.height);

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
        for (TimelineLayer tl : TimelineLayers.get()) {
            if (tl.showYAxis()) {
                tl.getYAxis().shiftDownPixels(distanceY, graphArea.height);
            }
        }
    }

    private static void setAvailableInterval() {
        if (locked)
            layersTimer.restart();

        long diff = selectedAxis.end() - selectedAxis.start();
        long availableStart = selectedAxis.start() - diff;
        long availableEnd = selectedAxis.end() + diff;
        availableAxis.set(TimeUtils.floorDay(availableStart), TimeUtils.floorDay(availableEnd) + TimeUtils.DAY_IN_MILLIS);

        TimelineLayers.get().forEach(timelineLayer -> timelineLayer.fetchData(selectedAxis));
        drawRequest();
    }

    public static void setGraphInformation(Rectangle _graphSize) {
        graphSize = _graphSize;
        graphAreaChanged();
    }

    private static void createGraphArea() {
        int nrPropagatedAxes = Math.max(0, TimelineLayers.getNumberOfPropagationAxes());
        int height = graphSize.height - (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE + DrawConstants.GRAPH_BOTTOM_AXIS_SPACE * (nrPropagatedAxes + 1));
        int nrRightAxes = Math.max(0, TimelineLayers.getNumberOfYAxes() - 1);
        int width = graphSize.width - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + nrRightAxes * DrawConstants.RIGHT_AXIS_WIDTH);
        graphArea = new Rectangle(DrawConstants.GRAPH_LEFT_SPACE, DrawConstants.GRAPH_TOP_SPACE, width, height);
    }

    public static Rectangle getGraphArea() {
        return graphArea;
    }

    public static Rectangle getGraphSize() {
        return graphSize;
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
        int movieLinePosition = selectedAxis.value2pixel(graphArea.x, graphArea.width, currentTime);
        if (movieLinePosition < graphArea.x || movieLinePosition > (graphArea.x + graphArea.width)) {
            return -1;
        }
        return movieLinePosition;
    }

    public static void setMovieFrame(Point point) {
        if (!graphArea.contains(point))
            return;
        Movie.setTime(new JHVTime(selectedAxis.pixel2value(graphArea.x, graphArea.width, point.x)));
    }

    @Override
    public void highlightChanged() {
        drawRequest();
    }

    public static void graphAreaChanged() {
        createGraphArea();
        moveX(0); // force recalculation of polylines
        drawRequest();
    }

    @Override
    public void timeRangeChanged(long start, long end) {
        if (locked)
            setSelectedInterval(start, end);
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
