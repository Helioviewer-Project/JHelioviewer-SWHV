package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.TimeListener;
import org.helioviewer.jhv.plugins.eveplugin.DrawConstants;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.jhv.viewmodel.view.View;

public class DrawController implements LineDataSelectorModelListener, JHVEventHighlightListener, LayersListener, TimeListener {

    public TimeAxis selectedAxis;
    public TimeAxis availableAxis;

    private static final HashSet<DrawControllerListener> listeners = new HashSet<DrawControllerListener>();

    private final DrawControllerOptionsPanel optionsPanel;

    private Rectangle graphSize;
    private boolean isLocked;
    private long latestMovieTime;
    private Rectangle graphArea;

    public DrawController() {
        graphSize = new Rectangle();

        long d = System.currentTimeMillis();
        availableAxis = new TimeAxis(d - TimeUtils.DAY_IN_MILLIS, d);
        selectedAxis = new TimeAxis(availableAxis.start, availableAxis.end);

        isLocked = false;
        latestMovieTime = Long.MIN_VALUE;

        optionsPanel = new DrawControllerOptionsPanel();

        EVEPlugin.ldsm.addLineDataSelectorModelListener(this);
    }

    public Component getOptionsPanel() {
        return optionsPanel;
    }

    public void addDrawControllerListener(DrawControllerListener listener) {
        listeners.add(listener);
    }

    public void removeDrawControllerListener(DrawControllerListener listener) {
        listeners.remove(listener);
    }

    public void setSelectedInterval(long newStart, long newEnd) {
        selectedAxis.set(newStart, newEnd, true);
        setAvailableInterval();
    }

    public void moveX(double pixelDistance) {
        selectedAxis.move(graphArea.x, graphArea.width, pixelDistance);
        setAvailableInterval();
    }

    public void moveXAvailableBased(int x0, int x1) {
        long av_diff = availableAxis.pixel2value(0, graphSize.width, x1) - availableAxis.pixel2value(0, graphSize.width, x0);
        selectedAxis.move(av_diff);
        setAvailableInterval();
    }

    private void zoomX(int x, double factor) {
        selectedAxis.zoom(graphArea.x, graphArea.width, x, factor);
        setAvailableInterval();
    }

    private void moveAndZoomY(Point p, double distanceY, int scrollDistance, boolean zoom, boolean move) {
        boolean yAxisVerticalCondition = (p.y > graphArea.y && p.y <= graphArea.y + graphArea.height);
        boolean inRightYAxes = p.x > graphArea.x + graphArea.width && yAxisVerticalCondition;
        boolean inLeftYAxis = p.x < graphArea.x && yAxisVerticalCondition;
        int rightYAxisNumber = (p.x - (graphArea.x + graphArea.width)) / DrawConstants.RIGHT_AXIS_WIDTH;
        int ct = -1;
        for (LineDataSelectorElement el : EVEPlugin.ldsm.getAllLineDataSelectorElements()) {
            if (el.showYAxis()) {
                if ((rightYAxisNumber == ct && inRightYAxes) || (ct == -1 && inLeftYAxis)) {
                    if (move)
                        el.getYAxis().shiftDownPixels(distanceY, graphArea.height);
                    if (zoom)
                        el.getYAxis().zoomSelectedRange(scrollDistance, graphSize.height - p.y - graphArea.y, graphArea.height);
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
        fireRedrawRequest();
    }

    public void resetAxis(Point p) {
        boolean yAxisVerticalCondition = (p.y > graphArea.y && p.y <= graphArea.y + graphArea.height);
        boolean inRightYAxes = p.x > graphArea.x + graphArea.width && yAxisVerticalCondition;
        boolean inLeftYAxis = p.x < graphArea.x && yAxisVerticalCondition;
        int rightYAxisNumber = (p.x - (graphArea.x + graphArea.width)) / DrawConstants.RIGHT_AXIS_WIDTH;
        int ct = -1;
        for (LineDataSelectorElement el : EVEPlugin.ldsm.getAllLineDataSelectorElements()) {
            if (el.showYAxis()) {
                if ((rightYAxisNumber == ct && inRightYAxes) || (ct == -1 && inLeftYAxis)) {
                    el.resetAxis();
                } else if (!inRightYAxes && !inLeftYAxis) {
                    el.zoomToFitAxis();
                }
                ct++;
            }
        }
        fireRedrawRequest();
    }

    public void moveY(Point p, double distanceY) {
        moveAndZoomY(p, distanceY, 0, false, true);
    }

    private void zoomY(Point p, int scrollDistance) {
        moveAndZoomY(p, 0, scrollDistance, true, false);
    }

    public void zoomXY(Point p, int scrollDistance, boolean shift, boolean alt, boolean ctrl) {
        double zoomTimeFactor = 10;
        boolean inGraphArea = (p.x >= graphArea.x && p.x <= graphArea.x + graphArea.width && p.y > graphArea.y && p.y <= graphArea.y + graphArea.height);
        boolean inXAxisOrAboveGraph = (p.x >= graphArea.x && p.x <= graphArea.x + graphArea.width && (p.y <= graphArea.y || p.y >= graphArea.y + graphArea.height));

        if (inGraphArea || inXAxisOrAboveGraph) {
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

    public void moveAllAxes(double distanceY) {
        for (LineDataSelectorElement el : EVEPlugin.ldsm.getAllLineDataSelectorElements()) {
            if (el.showYAxis()) {
                el.getYAxis().shiftDownPixels(distanceY, graphArea.height);
            }
        }
    }

    private void setAvailableInterval() {
        long availableStart = availableAxis.start;
        long availableEnd = availableAxis.end;

        if ((selectedAxis.start <= availableAxis.start || selectedAxis.end >= availableAxis.end)) {
            availableStart = Math.min(selectedAxis.start, availableStart);
            availableEnd = Math.max(selectedAxis.end, availableEnd);
            Interval availableInterval = TimeUtils.makeCompleteDay(availableStart, availableEnd);
            availableAxis.start = availableInterval.start;
            availableAxis.end = availableInterval.end;
        }
        optionsPanel.updateSelectedInterval(selectedAxis);

        for (LineDataSelectorElement el : EVEPlugin.ldsm.getAllLineDataSelectorElements()) {
            el.fetchData(selectedAxis, availableAxis);
        }
        fireRedrawRequest();
    }

    private void centraliseSelected(long time) {
        if (time != Long.MIN_VALUE && latestMovieTime != time && isLocked
                && availableAxis.start <= time && availableAxis.end >= time) {
            latestMovieTime = time;
            long selectedIntervalDiff = selectedAxis.end - selectedAxis.start;
            selectedAxis.set(time - ((long) (0.5 * selectedIntervalDiff)), time + ((long) (0.5 * selectedIntervalDiff)), false);
            fireRedrawRequest();
            for (LineDataSelectorElement el : EVEPlugin.ldsm.getAllLineDataSelectorElements()) {
                el.fetchData(selectedAxis, availableAxis);
            }
        }
    }

    public Interval getSelectedInterval() {
        return new Interval(selectedAxis.start, selectedAxis.end);
    }

    public void setGraphInformation(Rectangle graphSize) {
        this.graphSize = graphSize;
        createGraphArea();
        fireRedrawRequest();
    }

    private void createGraphArea() {
        int height = graphSize.height - (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE);
        int noRightAxes = Math.max(0, (EVEPlugin.ldsm.getNumberOfAxes() - 1));
        int width = (graphSize.width - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + noRightAxes * DrawConstants.RIGHT_AXIS_WIDTH));
        graphArea = new Rectangle(DrawConstants.GRAPH_LEFT_SPACE, DrawConstants.GRAPH_TOP_SPACE, width, height);
    }

    public Rectangle getGraphArea() {
        return graphArea;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
        if (isLocked && latestMovieTime != Long.MIN_VALUE) {
            centraliseSelected(latestMovieTime);
        }
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        createGraphArea();
        fireRedrawRequest();
    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        createGraphArea();
    }

    @Override
    public void timeChanged(JHVDate date) {
        movieTimestamp = date.milli;
        centraliseSelected(date.milli);
        fireRedrawRequestMovieFrameChanged();
    }

    private long movieTimestamp = Long.MIN_VALUE;

    public int getMovieLinePosition() {
        int movieLinePosition = -1;
        if (movieTimestamp == Long.MIN_VALUE) {
            movieLinePosition = -1;
        } else {
            movieLinePosition = selectedAxis.value2pixel(graphArea.x, graphArea.width, movieTimestamp);
            if (movieLinePosition < graphArea.x || movieLinePosition > (graphArea.x + graphArea.width)) {
                movieLinePosition = -1;
            }
        }
        return movieLinePosition;
    }

    public void setMovieFrame(Point point) {
        if (movieTimestamp == Long.MIN_VALUE || !graphArea.contains(point)) {
            return;
        }
        long millis = selectedAxis.pixel2value(graphArea.x, graphArea.width, point.x);
        Layers.setTime(new JHVDate(millis));
    }

    @Override
    public void eventHightChanged(JHVRelatedEvents event) {
        fireRedrawRequest();
    }

    @Override
    public void layerAdded(View view) {
        setSelectedInterval(Layers.getStartDate().milli, Layers.getEndDate().milli);
    }

    @Override
    public void activeLayerChanged(View view) {
        if (view == null) {
            movieTimestamp = Long.MIN_VALUE;
            fireRedrawRequestMovieFrameChanged();
            optionsPanel.lockButton.setEnabled(false);
        } else {
            fireMovieIntervalChanged(view.getFirstTime().milli, view.getLastTime().milli);
            optionsPanel.lockButton.setEnabled(true);
        }
    }

    public void fireRedrawRequest() {
        for (DrawControllerListener l : listeners) {
            l.drawRequest();
        }
    }

    private void fireRedrawRequestMovieFrameChanged() {
        for (DrawControllerListener l : listeners) {
            l.drawMovieLineRequest();
        }
    }

    private void fireMovieIntervalChanged(long start, long end) {
        for (DrawControllerListener l : listeners) {
            l.movieIntervalChanged(start, end);
        }
    }

}
