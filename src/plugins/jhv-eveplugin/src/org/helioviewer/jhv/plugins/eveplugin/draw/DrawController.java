package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.Rectangle;
import java.util.HashSet;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.display.Displayer;
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

    private Rectangle graphSize;
    private boolean isLocked;
    private long latestMovieTime;
    private Rectangle graphArea;
    private Rectangle plotArea;
    private Rectangle leftAxisArea;

    public DrawController() {
        graphSize = new Rectangle();

        long d = System.currentTimeMillis();
        availableAxis = new TimeAxis(d - TimeUtils.DAY_IN_MILLIS, d);
        selectedAxis = new TimeAxis(availableAxis.start, availableAxis.end);

        isLocked = false;
        latestMovieTime = Long.MIN_VALUE;

        EVEPlugin.ldsm.addLineDataSelectorModelListener(this);
        JHVRelatedEvents.addHighlightListener(this);
        JHVRelatedEvents.addHighlightListener(Displayer.getSingletonInstance());
        Layers.addLayersListener(this);
        Layers.addTimeListener(this);
    }

    public void addDrawControllerListener(DrawControllerListener listener) {
        listeners.add(listener);
    }

    public void removeDrawControllerListener(DrawControllerListener listener) {
        listeners.remove(listener);
    }

    public final Interval getAvailableInterval() {
        return new Interval(availableAxis.start, availableAxis.end);
    }

    public void setSelectedInterval(long newStart, long newEnd) {
        selectedAxis.set(newStart, newEnd);
        setAvailableInterval();
    }

    public void move(int x0, int w, double pixelDistance) {
        selectedAxis.move(x0, w, pixelDistance);
        setAvailableInterval();
    }

    public void zoom(int x0, int w, int x, double factor) {
        selectedAxis.zoom(x0, w, x, factor);
        setAvailableInterval();
    }

    public void setAvailableInterval() {
        long availableStart = availableAxis.start;
        long availableEnd = availableAxis.end;

        if ((selectedAxis.start <= availableAxis.start || selectedAxis.end >= availableAxis.end)) {
            availableStart = Math.min(selectedAxis.start, availableStart);
            availableEnd = Math.max(selectedAxis.end, availableEnd);
            Interval availableInterval = TimeUtils.makeCompleteDay(availableStart, availableEnd);
            availableAxis.start = availableInterval.start;
            availableAxis.end = availableInterval.end;
        }
        fireRedrawRequest();
        for (LineDataSelectorElement el : EVEPlugin.ldsm.getAllLineDataSelectorElements()) {
            el.fetchData(selectedAxis, availableAxis);
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
        plotArea = new Rectangle(0, 0, width, height);
        leftAxisArea = new Rectangle(0, DrawConstants.GRAPH_TOP_SPACE, DrawConstants.GRAPH_LEFT_SPACE, height - (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE));
    }

    public Rectangle getGraphArea() {
        return graphArea;
    }

    public Rectangle getPlotArea() {
        return plotArea;
    }

    public Rectangle getLeftAxisArea() {
        return leftAxisArea;
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
        centraliseSelected(date.milli);
        fireRedrawRequestMovieFrameChanged(date.milli);
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
            fireRedrawRequestMovieFrameChanged(Long.MIN_VALUE);
        }
    }

    public void fireRedrawRequest() {
        for (DrawControllerListener l : listeners) {
            l.drawRequest();
        }
    }

    private void fireRedrawRequestMovieFrameChanged(final long time) {
        for (DrawControllerListener l : listeners) {
            l.drawMovieLineRequest(time);
        }
    }

    private void centraliseSelected(long time) {
        if (time != Long.MIN_VALUE && latestMovieTime != time && isLocked
                && availableAxis.start <= time && availableAxis.end >= time) {
            latestMovieTime = time;
            long selectedIntervalDiff = selectedAxis.end - selectedAxis.start;
            setSelectedInterval(time - ((long) (0.5 * selectedIntervalDiff)), time + ((long) (0.5 * selectedIntervalDiff)));
        }
    }

}
