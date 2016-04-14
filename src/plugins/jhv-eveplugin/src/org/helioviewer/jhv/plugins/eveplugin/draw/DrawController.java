package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.JHVDate;
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

public class DrawController implements LineDataSelectorModelListener, JHVEventHighlightListener, LayersListener, TimeListener, PlotAreaSpaceListener {

    private static DrawController instance;
    private Interval selectedInterval;
    private Interval availableInterval;

    private PlotAreaSpace pas;

    private final List<TimingListener> tListeners;
    private Rectangle graphSize;
    // private Rectangle graphArea;
    // private Rectangle plotArea;
    // private Rectangle leftAxisArea;
    private final List<GraphDimensionListener> gdListeners;

    private List<YAxisElement> yAxisSet;

    private final Map<DrawableType, Set<DrawableElement>> drawableElements;
    private final List<DrawControllerListener> listeners;

    private DrawController() {
        drawableElements = new HashMap<DrawableType, Set<DrawableElement>>();
        listeners = new ArrayList<DrawControllerListener>();
        yAxisSet = new ArrayList<YAxisElement>();
        tListeners = new ArrayList<TimingListener>();
        gdListeners = new ArrayList<GraphDimensionListener>();
        graphSize = new Rectangle();

        Date d = new Date();
        availableInterval = new Interval(new Date(d.getTime() - 86400 * 1000), d);
        selectedInterval = availableInterval;

        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);
        pas = PlotAreaSpace.getSingletonInstance();
        pas.addPlotAreaSpaceListener(this);
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

        // request data if needed
        final Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(availableInterval.end);
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        final Interval downloadInterval = new Interval(availableInterval.start, calendar.getTime());

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

    private void fireRedrawRequestMovieFrameChanged(final Date time) {
        for (DrawControllerListener l : listeners) {
            l.drawMovieLineRequest(time);
        }
    }

    @Override
    public void timeChanged(JHVDate date) {
        fireRedrawRequestMovieFrameChanged(date.getDate());
    }

    public Date getLastDateWithData() {
        Date lastDate = null;
        for (Set<DrawableElement> des : drawableElements.values()) {
            for (DrawableElement de : des) {
                if (de.getLastDateWithData() != null) {
                    if (lastDate == null || de.getLastDateWithData().before(lastDate)) {
                        lastDate = de.getLastDateWithData();
                    }
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
        Interval interval = new Interval(Layers.getStartDate().getDate(), Layers.getEndDate().getDate());
        if (availableInterval == null) {
            availableInterval = interval;
        } else {
            Date start = availableInterval.start;
            if (interval.start.before(start)) {
                start = interval.start;
            }
            Date end = availableInterval.end;
            if (interval.end.after(end)) {
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
            fireRedrawRequestMovieFrameChanged(null);
        }
    }

    private Interval makeCompleteDay(final Date start, final Date end) {
        Date endDate = end;

        if (end.getTime() > System.currentTimeMillis()) {
            endDate = new Date();
        }

        final Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(start);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date s = calendar.getTime();

        calendar.clear();
        calendar.setTime(endDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date e = calendar.getTime();

        return new Interval(s, e);
    }

    public Interval setSelectedInterval(final Interval newSelectedInterval, boolean useFullValueSpace, boolean resetAvailable) {
        setSelectedInterval(newSelectedInterval, useFullValueSpace, true, resetAvailable);
        return selectedInterval;
    }

    private void setSelectedInterval(final Interval newSelectedInterval, boolean useFullValueSpace, boolean willUpdatePlotAreaSpace, boolean resetAvailable) {
        if (newSelectedInterval.start.compareTo(newSelectedInterval.end) <= 0) {
            if (availableInterval.containsInclusive(newSelectedInterval)) {
                selectedInterval = newSelectedInterval;
                if (resetAvailable) {
                    setAvailableInterval(newSelectedInterval);
                }
            } else {
                Date start = newSelectedInterval.start;
                Date end = newSelectedInterval.end;

                Date availableStart = availableInterval.start;
                Date availableEnd = availableInterval.end;
                boolean changeAvailableInterval = false;

                if (!availableInterval.containsPointInclusive(start) && !availableInterval.containsPointInclusive(end)) {
                    changeAvailableInterval = true;
                    availableStart = start;
                    availableEnd = end;
                }

                if (start.equals(end)) {
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
        long diffAvailable = availableInterval.end.getTime() - availableInterval.start.getTime();
        double diffPlotAreaTime = pas.getScaledMaxTime() - pas.getScaledMinTime();
        double scaledSelectedStart = pas.getScaledMinTime() + (1.0 * (selectedInterval.start.getTime() - availableInterval.start.getTime()) * diffPlotAreaTime / diffAvailable);
        double scaledSelectedEnd = pas.getScaledMinTime() + (1.0 * (selectedInterval.end.getTime() - availableInterval.start.getTime()) * diffPlotAreaTime / diffAvailable);
        pas.setMinSelectedTimeDiff(60000.0 / diffAvailable);
        pas.setScaledSelectedTime(scaledSelectedStart, scaledSelectedEnd, true);
    }

    private void fireAvailableIntervalChanged() {
        for (TimingListener listener : tListeners) {
            listener.availableIntervalChanged();
        }
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinTime, double scaledMaxTime, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        long diffTime = availableInterval.end.getTime() - availableInterval.start.getTime();
        double scaleDiff = scaledMaxTime - scaledMinTime;
        double selectedMin = (scaledSelectedMinTime - scaledMinTime) / scaleDiff;
        double selectedMax = (scaledSelectedMaxTime - scaledMinTime) / scaleDiff;
        Date newSelectedStartTime = new Date(availableInterval.start.getTime() + Math.round(diffTime * selectedMin));
        Date newSelectedEndTime = new Date(availableInterval.start.getTime() + Math.round(diffTime * selectedMax));
        if (forced || !(newSelectedEndTime.equals(selectedInterval.end) && newSelectedStartTime.equals(selectedInterval.start))) {
            setSelectedInterval(new Interval(newSelectedStartTime, newSelectedEndTime), false, false, false);
        }
    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinTime, double oldMaxTime, double newMinTime, double newMaxTime) {
        if (oldMinTime > newMinTime || oldMaxTime < newMaxTime) {
            double timeRatio = (availableInterval.end.getTime() - availableInterval.start.getTime()) / (oldMaxTime - oldMinTime);
            double startDifference = oldMinTime - newMinTime;
            double endDifference = newMaxTime - oldMaxTime;

            Date tempStartDate = new Date(availableInterval.start.getTime() - Math.round(startDifference * timeRatio));
            Date tempEndDate = new Date(availableInterval.end.getTime() + Math.round(endDifference * timeRatio));

            setAvailableInterval(new Interval(tempStartDate, tempEndDate));
        }
    }

    public PlotAreaSpace getPlotAreaSpace() {
        return pas;
    }

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

    public List<YAxisElement> getAllYAxisElementsForUnit(String unit) {
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

}
