package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
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
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis.YAxisLocation;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.jhv.viewmodel.view.View;

public class DrawController implements LineDataSelectorModelListener, JHVEventHighlightListener, LayersListener, TimeListener {

    public TimeAxis selectedAxis;
    public TimeAxis availableAxis;

    private static final HashSet<DrawControllerListener> listeners = new HashSet<DrawControllerListener>();
    private static final HashSet<TimingListener> tListeners = new HashSet<TimingListener>();
    private static final HashSet<RangeListener> rangeListeners = new HashSet<RangeListener>();
    private static final HashSet<GraphDimensionListener> gdListeners = new HashSet<GraphDimensionListener>();

    private static final HashMap<DrawableType, Set<DrawableElement>> drawableElements = new HashMap<DrawableType, Set<DrawableElement>>();

    private Rectangle graphSize;
    private List<YAxis> yAxes;
    private boolean isLocked;
    private long latestMovieTime;
    private Rectangle graphArea;
    private Rectangle plotArea;
    private Rectangle leftAxisArea;
    private boolean fullValueRange;

    public DrawController() {
        yAxes = new ArrayList<YAxis>();
        graphSize = new Rectangle();

        long d = System.currentTimeMillis();
        availableAxis = new TimeAxis(d - TimeUtils.DAY_IN_MILLIS, d);
        selectedAxis = new TimeAxis(availableAxis.min, availableAxis.max);

        isLocked = false;
        latestMovieTime = Long.MIN_VALUE;
        fullValueRange = false;

        EVEPlugin.ldsm.addLineDataSelectorModelListener(this);
        JHVRelatedEvents.addHighlightListener(this);
        JHVRelatedEvents.addHighlightListener(Displayer.getSingletonInstance());
    }

    public void addDrawControllerListener(DrawControllerListener listener) {
        listeners.add(listener);
    }

    public void removeDrawControllerListener(DrawControllerListener listener) {
        listeners.remove(listener);
    }

    public void addGraphDimensionListener(GraphDimensionListener l) {
        gdListeners.add(l);
    }

    public void addTimingListener(TimingListener listener) {
        tListeners.add(listener);
    }

    public void addRangeListener(RangeListener rangeListener) {
        rangeListeners.add(rangeListener);
    }

    public void updateDrawableElement(DrawableElement drawableElement, boolean needsFire) {
        addDrawableElement(drawableElement, false);
        if (needsFire && drawableElement.hasElementsToDraw()) {
            fireRedrawRequest();
        }
    }

    public void removeDrawableElement(DrawableElement element) {
        removeDrawableElement(element, true, false);
    }

    public boolean isFullValueRange() {
        return fullValueRange;
    }

    public void useFullValueRange(boolean b) {
        fullValueRange = b;
    }

    public List<YAxis> getYAxes() {
        return yAxes;
    }

    public Map<DrawableType, Set<DrawableElement>> getDrawableElements() {
        return drawableElements;
    }

    public void setSelectedRange(Range selectedRange) {
        fireRedrawRequest();
    }

    public final Interval getAvailableInterval() {
        return new Interval(availableAxis.min, availableAxis.max);
    }

    public void setSelectedInterval() {
        setSelectedInterval(selectedAxis.min, selectedAxis.max);
    }

    public void setSelectedInterval(long newStart, long newEnd) {
        if (newStart <= newEnd) {
            long now = (new Date()).getTime();
            selectedAxis.min = newStart;
            selectedAxis.max = newEnd;
            long intervalLength = newEnd - newStart;
            if (intervalLength == 0) {
                selectedAxis.max = selectedAxis.min + TimeUtils.MINUTE_IN_MILLIS;
                intervalLength = TimeUtils.MINUTE_IN_MILLIS;
            }
            if (newEnd > now) {
                selectedAxis.min = now - intervalLength;
                selectedAxis.max = now;
            }

            long availableStart = availableAxis.min;
            long availableEnd = availableAxis.max;

            if ((selectedAxis.min <= availableAxis.min || selectedAxis.max >= availableAxis.max)) {
                availableStart = Math.min(selectedAxis.min, availableStart);
                availableEnd = Math.max(selectedAxis.max, availableEnd);
                setAvailableInterval(availableStart, availableEnd);
            }

            fullValueRange = false;
            fireSelectedIntervalChanged();
            fireRedrawRequest();
        } else {
            Log.debug("Start was after end. Set by: ");
            Thread.dumpStack();
        }
    }

    public Interval getSelectedInterval() {
        return new Interval(selectedAxis.min, selectedAxis.max);
    }

    public long getLastDateWithData() {
        long lastDate = Long.MAX_VALUE;
        for (Set<DrawableElement> des : drawableElements.values()) {
            for (DrawableElement de : des) {
                long temp = de.getLastDateWithData();
                if (temp != -1 && temp < lastDate) {
                    lastDate = temp;
                }
            }
        }
        return lastDate;
    }

    public void setGraphInformation(Rectangle graphSize) {
        this.graphSize = graphSize;
        createGraphArea();
        fireGraphDimensionsChanged();
        fireRedrawRequest();
    }

    private void createGraphArea() {

        int height = graphSize.height - (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE);

        int twoYAxis = 0;
        if (getYAxes().size() >= 2) {
            twoYAxis = 1;
        }
        int width = graphSize.width - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + twoYAxis * DrawConstants.TWO_AXIS_GRAPH_RIGHT);

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

    public boolean hasAxisAvailable() {
        return yAxes.size() < 2;
    }

    public boolean canBePutOnAxis(String unit) {
        for (YAxis el : yAxes) {
            if (el.getLabel().equalsIgnoreCase(unit)) {
                return true;
            }
        }
        return false;
    }

    public YAxis getYAxisForUnit(String unit) {
        for (YAxis el : yAxes) {
            if (el.getOriginalLabel().equalsIgnoreCase(unit)) {
                return el;
            }
        }
        return null;
    }

    public boolean canChangeAxis(String unitLabel) {
        return getAllYAxesForUnit(unitLabel).size() == 2 || yAxes.size() < 2;
    }

    public YAxis.YAxisLocation getYAxisLocation(YAxis yAxis) {
        switch (yAxes.indexOf(yAxis)) {
        case 0:
            return YAxis.YAxisLocation.LEFT;
        case 1:
            return YAxis.YAxisLocation.RIGHT;
        }
        return YAxisLocation.LEFT;
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

    public void rangeChanged() {
        for (RangeListener rl : rangeListeners) {
            rl.rangeChanged();
        }
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        fireRedrawRequest();
    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
        fireRedrawRequest();
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

    private void fireRedrawRequest() {
        for (DrawControllerListener l : listeners) {
            l.drawRequest();
        }
    }

    private void addDrawableElement(DrawableElement element, boolean redraw) {
        Set<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
        if (elements == null) {
            elements = new HashSet<DrawableElement>();
            drawableElements.put(element.getDrawableElementType().getLevel(), elements);
        }
        elements.add(element);

        if (element.getYAxis() != null && !yAxes.contains(element.getYAxis())) {
            yAxes.add(element.getYAxis());
            createGraphArea();
        }
        if (redraw) {
            fireRedrawRequest();
        }
    }

    private void removeDrawableElement(DrawableElement element, boolean redraw, boolean keepYAxis) {
        Set<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
        if (elements != null && !keepYAxis) {
            elements.remove(element);
            if (elements.isEmpty()) {
                drawableElements.remove(element.getDrawableElementType().getLevel());
            }
            createYAxisSet();
        }
        if (redraw) {
            fireRedrawRequest();
        }
    }

    private void setAvailableInterval(final long start, final long end) {
        Interval availableInterval = makeCompleteDay(start, end);
        availableAxis.min = availableInterval.start;
        availableAxis.max = availableInterval.end;
        fireAvailableIntervalChanged();
    }

    private void fireRedrawRequestMovieFrameChanged(final long time) {

        for (DrawControllerListener l : listeners) {
            l.drawMovieLineRequest(time);
        }
    }

    private void centraliseSelected(long time) {
        if (time != Long.MIN_VALUE && latestMovieTime != time && isLocked && availableAxis.min <= time && availableAxis.max >= time) {
            latestMovieTime = time;
            long selectedIntervalDiff = selectedAxis.max - selectedAxis.min;
            setSelectedInterval(time - ((long) (0.5 * selectedIntervalDiff)), time + ((long) (0.5 * selectedIntervalDiff)));
        }
    }

    private Interval makeCompleteDay(final long start, final long end) {
        long endDate = end;
        long now = System.currentTimeMillis();
        if (end > now) {
            endDate = now;
        }

        long new_start = start - start % TimeUtils.DAY_IN_MILLIS;
        long new_end = endDate - endDate % TimeUtils.DAY_IN_MILLIS + TimeUtils.DAY_IN_MILLIS;

        return new Interval(new_start, new_end);
    }

    private void fireSelectedIntervalChanged() {
        for (TimingListener listener : tListeners) {
            listener.selectedIntervalChanged();
        }
    }

    private void fireAvailableIntervalChanged() {
        centraliseSelected(latestMovieTime);
        for (TimingListener listener : tListeners) {
            listener.availableIntervalChanged();
        }
    }

    private void fireGraphDimensionsChanged() {
        for (GraphDimensionListener l : gdListeners) {
            l.graphDimensionChanged();
        }
    }

    private void createYAxisSet() {
        YAxis[] tempArray = new YAxis[2];
        for (Set<DrawableElement> elementsSet : drawableElements.values()) {
            for (DrawableElement de : elementsSet) {
                if (de.getYAxis() != null && yAxes.contains(de.getYAxis())) {
                    tempArray[yAxes.indexOf(de.getYAxis())] = de.getYAxis();
                }
            }
        }
        List<YAxis> newYAxisList = new ArrayList<YAxis>();
        for (int i = 0; i < 2; i++) {
            if (tempArray[i] != null) {
                newYAxisList.add(tempArray[i]);
            }
        }

        createGraphArea();
        yAxes = newYAxisList;
    }

    private List<YAxis> getAllYAxesForUnit(String unit) {
        List<YAxis> all = new ArrayList<YAxis>();
        for (YAxis el : yAxes) {
            if (el.getOriginalLabel().equalsIgnoreCase(unit)) {
                all.add(el);
            }
        }
        return all;
    }

    public void resetAvailableTime() {
        setAvailableInterval(selectedAxis.min, selectedAxis.max);
    }

}
