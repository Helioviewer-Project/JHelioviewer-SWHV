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
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement.YAxisLocation;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.jhv.viewmodel.view.View;

public class DrawController implements LineDataSelectorModelListener, JHVEventHighlightListener, LayersListener, TimeListener {

    private static DrawController instance;
    public TimeAxis selectedAxis;
    public TimeAxis availableAxis;

    private final List<TimingListener> tListeners;
    private final List<GraphDimensionListener> gdListeners;
    private final List<DrawControllerListener> listeners;
    private Rectangle graphSize;
    private List<YAxisElement> yAxisSet;
    private final Map<DrawableType, Set<DrawableElement>> drawableElements;
    private boolean isLocked;
    private long latestMovieTime;

    private DrawController() {
        drawableElements = new HashMap<DrawableType, Set<DrawableElement>>();
        listeners = new ArrayList<DrawControllerListener>();
        yAxisSet = new ArrayList<YAxisElement>();
        tListeners = new ArrayList<TimingListener>();
        gdListeners = new ArrayList<GraphDimensionListener>();
        graphSize = new Rectangle();

        long d = System.currentTimeMillis();
        availableAxis = new TimeAxis(d - TimeUtils.DAY_IN_MILLIS, d);
        selectedAxis = new TimeAxis(availableAxis.min, availableAxis.max);

        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);
        isLocked = false;
        latestMovieTime = Long.MIN_VALUE;
    }

    public static DrawController getSingletonInstance() {
        if (instance == null) {
            instance = new DrawController();
            JHVRelatedEvents.addHighlightListener(instance);
            JHVRelatedEvents.addHighlightListener(Displayer.getSingletonInstance());
        }
        return instance;
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

    public void updateDrawableElement(DrawableElement drawableElement, boolean needsFire) {
        addDrawableElement(drawableElement, false);
        if (needsFire && drawableElement.hasElementsToDraw()) {
            fireRedrawRequest();
        }
    }

    public void removeDrawableElement(DrawableElement element) {
        removeDrawableElement(element, true, false);
    }

    public List<YAxisElement> getYAxisElements() {
        return yAxisSet;
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

    public void setSelectedInterval(boolean useFullValueSpace, boolean resetAvailable) {
        setSelectedInterval(new Interval(selectedAxis.min, selectedAxis.max), useFullValueSpace, resetAvailable);
    }

    public void setSelectedInterval(final Interval newSelectedInterval, boolean useFullValueSpace, boolean resetAvailable) {
        if (newSelectedInterval.start <= newSelectedInterval.end) {
            long now = (new Date()).getTime();
            selectedAxis.min = newSelectedInterval.start;
            selectedAxis.max = newSelectedInterval.end;
            long intervalLength = newSelectedInterval.end - newSelectedInterval.start;
            if (intervalLength == 0) {
                selectedAxis.max = selectedAxis.min + 1000;
                intervalLength = 1000;
            }
            if (newSelectedInterval.end > now) {
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

            if (resetAvailable) {
                setAvailableInterval(selectedAxis.min, selectedAxis.max);
            }
            fireSelectedIntervalChanged(useFullValueSpace);
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
        fireGraphDimensionsChanged();
        fireRedrawRequest();
    }

    public Rectangle getPlotArea() {
        return new Rectangle(0, 0, getGraphWidth(), getGraphHeight());
    }

    public Rectangle getGraphArea() {
        return new Rectangle(DrawConstants.GRAPH_LEFT_SPACE, DrawConstants.GRAPH_TOP_SPACE, getGraphWidth(), getGraphHeight());
    }

    public Rectangle getLeftAxisArea() {
        return new Rectangle(0, DrawConstants.GRAPH_TOP_SPACE, DrawConstants.GRAPH_LEFT_SPACE, getGraphHeight() - (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE));
    }

    public boolean hasAxisAvailable() {
        return yAxisSet.size() < 2;
    }

    public boolean canBePutOnAxis(String unit) {
        for (YAxisElement el : yAxisSet) {
            if (el.getLabel().toLowerCase().equals(unit.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public YAxisElement getYAxisElementForUnit(String unit) {
        for (YAxisElement el : yAxisSet) {
            if (el.getOriginalLabel().toLowerCase().equals(unit.toLowerCase())) {
                return el;
            }
        }
        return null;
    }

    public boolean canChangeAxis(String unitLabel) {
        return getAllYAxisElementsForUnit(unitLabel).size() == 2 || yAxisSet.size() < 2;
    }

    public YAxisElement.YAxisLocation getYAxisLocation(YAxisElement yAxisElement) {
        switch (yAxisSet.indexOf(yAxisElement)) {
        case 0:
            return YAxisElement.YAxisLocation.LEFT;
        case 1:
            return YAxisElement.YAxisLocation.RIGHT;
        }
        return YAxisLocation.LEFT;
    }

    public void resetSelectedValueAndTimeInterval() {
        for (YAxisElement yAxis : yAxisSet) {
            yAxis.resetScaledSelectedRange();
        }
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
        if (isLocked) {
            if (latestMovieTime != Long.MIN_VALUE) {
                centraliseSelected(latestMovieTime);
            }
        }
    }

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
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
        setSelectedInterval(new Interval(Layers.getStartDate().milli, Layers.getEndDate().milli), false, false);
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

        if (element.getYAxisElement() != null) {
            if (!yAxisSet.contains(element.getYAxisElement())) {
                yAxisSet.add(element.getYAxisElement());
            }
        }
        if (redraw) {
            fireRedrawRequest();
        }
    }

    private void removeDrawableElement(DrawableElement element, boolean redraw, boolean keepYAxisElement) {
        Set<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
        if (elements != null && !keepYAxisElement) {
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
            setSelectedInterval(new Interval(time - ((long) (0.5 * selectedIntervalDiff)), time + ((long) (0.5 * selectedIntervalDiff))), false, false);
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

    private void fireSelectedIntervalChanged(boolean keepFullValueRange) {
        for (TimingListener listener : tListeners) {
            listener.selectedIntervalChanged(keepFullValueRange);
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

    private int getGraphHeight() {
        return graphSize.height - (DrawConstants.GRAPH_TOP_SPACE + DrawConstants.GRAPH_BOTTOM_SPACE);
    }

    private int getGraphWidth() {
        int twoYAxis = 0;
        if (getYAxisElements().size() >= 2) {
            twoYAxis = 1;
        }
        return graphSize.width - (DrawConstants.GRAPH_LEFT_SPACE + DrawConstants.GRAPH_RIGHT_SPACE + twoYAxis * DrawConstants.TWO_AXIS_GRAPH_RIGHT);
    }

    private void createYAxisSet() {
        YAxisElement[] tempArray = new YAxisElement[2];
        for (Set<DrawableElement> elementsSet : drawableElements.values()) {
            for (DrawableElement de : elementsSet) {
                if (de.getYAxisElement() != null) {
                    if (yAxisSet.contains(de.getYAxisElement())) {
                        tempArray[yAxisSet.indexOf(de.getYAxisElement())] = de.getYAxisElement();
                    }
                }
            }
        }
        List<YAxisElement> newYAxisList = new ArrayList<YAxisElement>();
        for (int i = 0; i < 2; i++) {
            if (tempArray[i] != null) {
                newYAxisList.add(tempArray[i]);
            }
        }

        yAxisSet = newYAxisList;
    }

    private List<YAxisElement> getAllYAxisElementsForUnit(String unit) {
        List<YAxisElement> all = new ArrayList<YAxisElement>();
        for (YAxisElement el : yAxisSet) {
            if (el.getOriginalLabel().toLowerCase().equals(unit.toLowerCase())) {
                all.add(el);
            }
        }
        return all;
    }

}
