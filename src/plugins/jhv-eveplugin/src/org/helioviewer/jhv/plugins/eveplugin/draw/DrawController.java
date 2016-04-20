package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.Rectangle;
import java.util.ArrayList;
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
import org.helioviewer.jhv.plugins.eveplugin.lines.data.DownloadController;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.jhv.viewmodel.view.View;

public class DrawController implements LineDataSelectorModelListener, JHVEventHighlightListener, LayersListener, TimeListener {

    private static DrawController instance;
    private Interval selectedInterval;
    private Interval availableInterval;

    private final List<TimingListener> tListeners;
    private Rectangle graphSize;
    private final List<GraphDimensionListener> gdListeners;

    private List<YAxisElement> yAxisSet;

    private final Map<DrawableType, Set<DrawableElement>> drawableElements;
    private final List<DrawControllerListener> listeners;

    private double scaledMinTime;
    private double scaledMaxTime;
    private double scaledSelectedMinTime;
    private double scaledSelectedMaxTime;
    private double minSelectedTimeDiff;

    private final Set<ValueSpace> valueSpaces;

    private DrawController() {
        drawableElements = new HashMap<DrawableType, Set<DrawableElement>>();
        listeners = new ArrayList<DrawControllerListener>();
        yAxisSet = new ArrayList<YAxisElement>();
        tListeners = new ArrayList<TimingListener>();
        gdListeners = new ArrayList<GraphDimensionListener>();
        graphSize = new Rectangle();

        long d = System.currentTimeMillis();
        availableInterval = new Interval(d - TimeUtils.DAY_IN_MILLIS, d);
        selectedInterval = availableInterval;

        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);
        scaledMinTime = 0.0;
        scaledMaxTime = 1.0;
        scaledSelectedMinTime = 0.0;
        scaledSelectedMaxTime = 1.0;
        minSelectedTimeDiff = 0;
        valueSpaces = new HashSet<ValueSpace>();

    }

    public int calculateXLocation(long timestamp) {
        return (int) ((timestamp - selectedInterval.start) * getRatioX()) + getPlotArea().x;
    }

    public double getRatioX() {
        return getPlotArea().width / (double) (selectedInterval.end - selectedInterval.start);
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

    public void fireRedrawRequest() {
        for (DrawControllerListener l : listeners) {
            l.drawRequest();
        }
    }

    public void setAvailableInterval(final Interval interval) {
        availableInterval = makeCompleteDay(interval.start, interval.end);
        fireAvailableIntervalChanged();

        final Interval downloadInterval = new Interval(availableInterval.start, availableInterval.end - TimeUtils.DAY_IN_MILLIS);

        DownloadController.getSingletonInstance().updateBands(downloadInterval, selectedInterval);

        setSelectedInterval(selectedInterval, false, false);
    }

    public final Interval getAvailableInterval() {
        return availableInterval;
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

    private void fireRedrawRequestMovieFrameChanged(final long time) {
        for (DrawControllerListener l : listeners) {
            l.drawMovieLineRequest(time);
        }
    }

    @Override
    public void timeChanged(JHVDate date) {
        fireRedrawRequestMovieFrameChanged(date.milli);
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

    @Override
    public void eventHightChanged(JHVRelatedEvents event) {
        fireRedrawRequest();
    }

    @Override
    public void layerAdded(View view) {
        Interval interval = new Interval(Layers.getStartDate().milli, Layers.getEndDate().milli);
        if (availableInterval == null) {
            availableInterval = interval;
        } else {
            long start = availableInterval.start;
            if (interval.start < start) {
                start = interval.start;
            }
            long end = availableInterval.end;
            if (interval.end > end) {
                end = interval.end;
            }
            setAvailableInterval(new Interval(start, end));
        }
        TimeIntervalLockModel lockModel = TimeIntervalLockModel.getInstance();
        if (lockModel.isLocked()) {
            setSelectedInterval(interval, true, false);
            lockModel.setLocked(true);
        }
    }

    @Override
    public void activeLayerChanged(View view) {
        if (view == null) {
            fireRedrawRequestMovieFrameChanged(Long.MIN_VALUE);
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

    public Interval setSelectedInterval(final Interval newSelectedInterval, boolean useFullValueSpace, boolean resetAvailable) {
        setSelectedInterval(newSelectedInterval, useFullValueSpace, true, resetAvailable);
        return selectedInterval;
    }

    private void setSelectedInterval(final Interval newSelectedInterval, boolean useFullValueSpace, boolean willUpdatePlotAreaSpace, boolean resetAvailable) {
        if (newSelectedInterval.start <= newSelectedInterval.end) {
            if (availableInterval.containsInclusive(newSelectedInterval)) {
                selectedInterval = newSelectedInterval;
                if (resetAvailable) {
                    setAvailableInterval(newSelectedInterval);
                }
            } else {
                long start = newSelectedInterval.start;
                long end = newSelectedInterval.end;

                long availableStart = availableInterval.start;
                long availableEnd = availableInterval.end;
                boolean changeAvailableInterval = false;

                if (!availableInterval.containsPointInclusive(start) && !availableInterval.containsPointInclusive(end)) {
                    changeAvailableInterval = true;
                    availableStart = start;
                    availableEnd = end;
                }

                if (start == end) {
                    selectedInterval = new Interval(availableStart, availableEnd);
                } else {
                    selectedInterval = new Interval(start, end);
                }
                if (changeAvailableInterval) {
                    setAvailableInterval(new Interval(availableStart, availableEnd));
                }
            }

            if (willUpdatePlotAreaSpace) {
                updatePlotAreaSpace();
            }
            fireSelectedIntervalChanged(useFullValueSpace);
            fireRedrawRequest();
        } else {
            Log.debug("Start was after end. Set by: ");
            Thread.dumpStack();
        }
    }

    public Interval getSelectedInterval() {
        return selectedInterval;
    }

    private void fireSelectedIntervalChanged(boolean keepFullValueRange) {
        for (TimingListener listener : tListeners) {
            listener.selectedIntervalChanged(keepFullValueRange);
        }
    }

    private void updatePlotAreaSpace() {
        long diffAvailable = availableInterval.end - availableInterval.start;
        double diffPlotAreaTime = getScaledMaxTime() - getScaledMinTime();
        double scaledSelectedStart = getScaledMinTime() + (1.0 * (selectedInterval.start - availableInterval.start) * diffPlotAreaTime / diffAvailable);
        double scaledSelectedEnd = getScaledMinTime() + (1.0 * (selectedInterval.end - availableInterval.start) * diffPlotAreaTime / diffAvailable);
        setMinSelectedTimeDiff(60000.0 / diffAvailable);
        setScaledSelectedTime(scaledSelectedStart, scaledSelectedEnd, true);
    }

    private void fireAvailableIntervalChanged() {
        for (TimingListener listener : tListeners) {
            listener.availableIntervalChanged();
        }
    }

    /*
     * public PlotAreaSpace getPlotAreaSpace() { return pas; }
     */

    public void setGraphInformation(Rectangle graphSize) {
        this.graphSize = graphSize;
        fireGraphDimensionsChanged();
        fireRedrawRequest();
    }

    private void fireGraphDimensionsChanged() {
        for (GraphDimensionListener l : gdListeners) {
            l.graphDimensionChanged();
        }
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

    private List<YAxisElement> getAllYAxisElementsForUnit(String unit) {
        List<YAxisElement> all = new ArrayList<YAxisElement>();
        for (YAxisElement el : yAxisSet) {
            if (el.getOriginalLabel().toLowerCase().equals(unit.toLowerCase())) {
                all.add(el);
            }
        }
        return all;
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

    public double getScaledMinTime() {
        return scaledMinTime;
    }

    public double getScaledMaxTime() {
        return scaledMaxTime;
    }

    public double getScaledSelectedMinTime() {
        return scaledSelectedMinTime;
    }

    public double getScaledSelectedMaxTime() {
        return scaledSelectedMaxTime;
    }

    public void setScaledSelectedTime(double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        if ((forced || !(this.scaledSelectedMinTime == scaledSelectedMinTime && this.scaledSelectedMaxTime == scaledSelectedMaxTime)) && (scaledSelectedMaxTime - scaledSelectedMinTime) > minSelectedTimeDiff) {
            this.scaledSelectedMinTime = scaledSelectedMinTime;
            this.scaledSelectedMaxTime = scaledSelectedMaxTime;
            if (this.scaledSelectedMinTime < scaledMinTime || this.scaledSelectedMaxTime > scaledMaxTime) {
                double oldScaledMinTime = scaledMinTime;
                double oldScaledMaxTime = scaledMaxTime;
                scaledMinTime = Math.min(this.scaledSelectedMinTime, scaledMinTime);
                scaledMaxTime = Math.max(this.scaledSelectedMaxTime, scaledMaxTime);
                fireAvailableAreaSpaceChanged(oldScaledMinTime, oldScaledMaxTime, scaledMinTime, scaledMaxTime);
            }
            firePlotAreaSpaceChanged(forced);
        }
    }

    private void fireAvailableAreaSpaceChanged(double oldScaledMinTime, double oldScaledMaxTime, double newMinTime, double newMaxTime) {
        if (oldScaledMinTime > newMinTime || oldScaledMaxTime < newMaxTime) {
            double timeRatio = (availableInterval.end - availableInterval.start) / (oldScaledMaxTime - oldScaledMinTime);
            double startDifference = oldScaledMinTime - newMinTime;
            double endDifference = newMaxTime - oldScaledMaxTime;

            long tempStartDate = availableInterval.start - Math.round(startDifference * timeRatio);
            long tempEndDate = availableInterval.end + Math.round(endDifference * timeRatio);

            setAvailableInterval(new Interval(tempStartDate, tempEndDate));
        }

    }

    public Set<ValueSpace> getValueSpaces() {
        return valueSpaces;
    }

    public boolean minMaxTimeIntervalContainsTime(double value) {
        return value >= scaledMinTime && value <= scaledMaxTime;
    }

    public void resetSelectedValueAndTimeInterval() {
        scaledSelectedMinTime = scaledMinTime;
        scaledSelectedMaxTime = scaledMaxTime;
        for (ValueSpace vs : valueSpaces) {
            vs.resetScaledSelectedRange();
        }
    }

    @Override
    public String toString() {
        return "Scaled min time  : " + scaledMinTime + "\n" + "Scaled max time  : " + scaledMaxTime + "\n" + "\n" + "Selected scaled min time  : " + scaledSelectedMinTime + "\n" + "Selected scaled max time  : " + scaledSelectedMaxTime + "\n";
    }

    private void firePlotAreaSpaceChanged(boolean forced) {
        long diffTime = availableInterval.end - availableInterval.start;
        double scaleDiff = scaledMaxTime - scaledMinTime;
        double selectedMin = (scaledSelectedMinTime - scaledMinTime) / scaleDiff;
        double selectedMax = (scaledSelectedMaxTime - scaledMinTime) / scaleDiff;
        long newSelectedStartTime = availableInterval.start + Math.round(diffTime * selectedMin);
        long newSelectedEndTime = availableInterval.start + Math.round(diffTime * selectedMax);
        if (forced || !(newSelectedEndTime == selectedInterval.end) && newSelectedStartTime == selectedInterval.start) {
            setSelectedInterval(new Interval(newSelectedStartTime, newSelectedEndTime), false, false, false);
        }
    }

    public void addValueSpace(ValueSpace valueSpace) {
        valueSpaces.add(valueSpace);
    }

    public void removeValueSpace(ValueSpace valueSpace) {
        valueSpaces.remove(valueSpace);
    }

    public void setMinSelectedTimeDiff(double minSelectedTimeDiff) {
        this.minSelectedTimeDiff = minSelectedTimeDiff;
    }

}
