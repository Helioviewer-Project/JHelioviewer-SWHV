package org.helioviewer.jhv.timelines.draw;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.event.JHVEventHighlightListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.TimeListener;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;
import org.helioviewer.jhv.viewmodel.view.View;

public class DrawController implements JHVEventHighlightListener, LayersListener, TimeListener, TimespanListener {

    public static final TimeAxis selectedAxis;
    public static final TimeAxis availableAxis;

    private static final HashSet<DrawListener> listeners = new HashSet<>();
    private static final HashSet<DrawMovieIntervalListener> movieIntervalListeners = new HashSet<>();

    private static final DrawControllerOptionsPanel optionsPanel;

    private static Rectangle graphSize;
    private static boolean isLocked;
    private static long latestMovieTime;
    private static Rectangle graphArea;

    static {
        graphSize = new Rectangle();

        long d = System.currentTimeMillis();
        availableAxis = new TimeAxis(d - 2 * TimeUtils.DAY_IN_MILLIS, d);
        selectedAxis = new TimeAxis(availableAxis.start, availableAxis.end);

        isLocked = false;
        latestMovieTime = Long.MIN_VALUE;

        optionsPanel = new DrawControllerOptionsPanel();
    }

    public static Component getOptionsPanel() {
        return optionsPanel;
    }

    public static void addDrawListener(DrawListener listener) {
        listeners.add(listener);
    }

    public static void removeDrawListener(DrawListener listener) {
        listeners.remove(listener);
    }

    public static void addDrawMovieIntervalListener(DrawMovieIntervalListener listener) {
        movieIntervalListeners.add(listener);
    }

    public static void removeDrawMovieIntervalListener(DrawMovieIntervalListener listener) {
        movieIntervalListeners.remove(listener);
    }

    public static void setSelectedInterval(long start, long end) {
        selectedAxis.set(start, end, true);
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
        for (TimelineRenderable el : Timelines.getModel().getAllLineDataSelectorElements()) {
            if (el.showYAxis()) {
                if ((rightYAxisNumber == ct && inRightYAxes) || (ct == -1 && inLeftYAxis)) {
                    if (move) {
                        el.getYAxis().shiftDownPixels(distanceY, graphArea.height);
                    }
                    if (zoom) {
                        el.getYAxis().zoomSelectedRange(scrollDistance, graphSize.height - p.y - graphArea.y, graphArea.height);
                    }
                    el.yaxisChanged();
                } else if ((!inRightYAxes && !inLeftYAxis) && move) {
                    el.getYAxis().shiftDownPixels(distanceY, graphArea.height);
                    el.yaxisChanged();
                } else if ((!inRightYAxes && !inLeftYAxis) && zoom) {
                    el.getYAxis().zoomSelectedRange(scrollDistance, graphSize.height - p.y - graphArea.y, graphArea.height);
                    el.yaxisChanged();
                }
                ct++;
            }
        }
        drawRequest();
    }

    public static void resetAxis(Point p) {
        boolean yAxisVerticalCondition = (p.y > graphArea.y && p.y <= graphArea.y + graphArea.height);
        boolean inRightYAxes = p.x > graphArea.x + graphArea.width && yAxisVerticalCondition;
        boolean inLeftYAxis = p.x < graphArea.x && yAxisVerticalCondition;
        int rightYAxisNumber = (p.x - (graphArea.x + graphArea.width)) / DrawConstants.RIGHT_AXIS_WIDTH;
        int ct = -1;
        for (TimelineRenderable el : Timelines.getModel().getAllLineDataSelectorElements()) {
            if (el.showYAxis()) {
                if ((rightYAxisNumber == ct && inRightYAxes) || (ct == -1 && inLeftYAxis)) {
                    el.resetAxis();
                } else if (!inRightYAxes && !inLeftYAxis) {
                    el.zoomToFitAxis();
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
        boolean inGraphArea = (p.x >= graphArea.x && p.x <= graphArea.x + graphArea.width && p.y > graphArea.y && p.y <= graphArea.y + graphArea.height);
        boolean inXAxisOrAboveGraph = (p.x >= graphArea.x && p.x <= graphArea.x + graphArea.width && (p.y <= graphArea.y || p.y >= graphArea.y + graphArea.height));

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
        for (TimelineRenderable el : Timelines.getModel().getAllLineDataSelectorElements()) {
            if (el.showYAxis()) {
                el.getYAxis().shiftDownPixels(distanceY, graphArea.height);
            }
        }
    }

    private static void setAvailableInterval() {
        long availableStart = availableAxis.start;
        long availableEnd = availableAxis.end;

        if ((selectedAxis.start <= availableAxis.start || selectedAxis.end >= availableAxis.end)) {
            availableStart = Math.min(selectedAxis.start, availableStart);
            availableEnd = Math.max(selectedAxis.end, availableEnd);
            Interval availableInterval = Interval.makeCompleteDay(availableStart, availableEnd);
            availableAxis.start = availableInterval.start;
            availableAxis.end = availableInterval.end;
        }
        optionsPanel.updateSelectedInterval();

        for (TimelineRenderable el : Timelines.getModel().getAllLineDataSelectorElements()) {
            el.fetchData(selectedAxis);
        }
        drawRequest();
    }

    private static void centraliseSelected(long time) {
        if (time != Long.MIN_VALUE && isLocked && availableAxis.start <= time && availableAxis.end >= time) {
            long selectedIntervalDiff = selectedAxis.end - selectedAxis.start;
            selectedAxis.set(time - (long) (0.5 * selectedIntervalDiff), time + (long) (0.5 * selectedIntervalDiff), false);
            drawRequest();
            for (TimelineRenderable el : Timelines.getModel().getAllLineDataSelectorElements()) {
                el.fetchData(selectedAxis);
            }
        }
    }

    public static void setGraphInformation(Rectangle _graphSize) {
        graphSize = _graphSize;
        graphAreaChanged();
    }

    private static void createGraphArea() {
        int height = graphSize.height - (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE);
        int noRightAxes = Math.max(0, Timelines.getModel().getNumberOfAxes() - 1);
        int width = graphSize.width - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + noRightAxes * DrawConstants.RIGHT_AXIS_WIDTH);
        graphArea = new Rectangle(DrawConstants.GRAPH_LEFT_SPACE, DrawConstants.GRAPH_TOP_SPACE, width, height);
    }

    public static Rectangle getGraphArea() {
        return graphArea;
    }

    public static Rectangle getGraphSize() {
        return graphSize;
    }

    public static void setLocked(boolean _isLocked) {
        isLocked = _isLocked;
        if (isLocked && latestMovieTime != Long.MIN_VALUE) {
            centraliseSelected(latestMovieTime);
        }
    }

    @Override
    public void timeChanged(long milli) {
        latestMovieTime = milli;
        centraliseSelected(latestMovieTime);
        drawMovieLine = true;
    }

    public static int getMovieLinePosition() {
        if (latestMovieTime == Long.MIN_VALUE) {
            return -1;
        }

        int movieLinePosition = selectedAxis.value2pixel(graphArea.x, graphArea.width, latestMovieTime);
        if (movieLinePosition < graphArea.x || movieLinePosition > (graphArea.x + graphArea.width)) {
            return -1;
        }
        return movieLinePosition;
    }

    public static void setMovieFrame(Point point) {
        if (latestMovieTime == Long.MIN_VALUE || !graphArea.contains(point)) {
            return;
        }
        long millis = selectedAxis.pixel2value(graphArea.x, graphArea.width, point.x);
        Layers.setTime(new JHVDate(millis));
    }

    @Override
    public void eventHightChanged() {
        drawRequest();
    }

    @Override
    public void timespanChanged(long start, long end) {
        setSelectedInterval(start, end);
    }

    @Override
    public void activeLayerChanged(View view) {
        if (view == null) {
            timeChanged(Long.MIN_VALUE);
            optionsPanel.lockButton.setEnabled(false);
            long now = System.currentTimeMillis();
            fireMovieIntervalChanged(now, now); // remove movie interval
        } else {
            optionsPanel.lockButton.setEnabled(true);
            fireMovieIntervalChanged(view.getFirstTime().milli, view.getLastTime().milli);
        }
    }

    public static void graphAreaChanged() {
        createGraphArea();
        moveX(0); // force recalculation of polylines
        drawRequest();
    }

    public static void drawRequest() {
        toDraw = true;
    }

    private static void fireMovieIntervalChanged(long start, long end) {
        for (DrawMovieIntervalListener l : movieIntervalListeners) {
            l.movieIntervalChanged(start, end);
        }
    }

    private static boolean toDraw;
    private static boolean drawMovieLine;
    private static final Timer drawTimer = new Timer(1000 / 20, new RedrawListener());

    private static class RedrawListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (toDraw) {
                toDraw = false;
                for (DrawListener l : listeners) {
                    l.drawRequest();
                }
            }
            if (drawMovieLine) {
                drawMovieLine = false;
                for (DrawListener l : listeners) {
                    l.drawMovieLineRequest();
                }
            }
        }
    }

    public static void start() {
        drawTimer.start();
    }

    public static void stop() {
        drawTimer.stop();
    }

}
