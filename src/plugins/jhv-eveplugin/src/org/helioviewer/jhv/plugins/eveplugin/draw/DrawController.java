package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.TimeListener;
import org.helioviewer.jhv.plugins.eveplugin.base.Range;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.DownloadController;
import org.helioviewer.jhv.plugins.eveplugin.view.chart.ChartConstants;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.viewmodel.view.View;

public class DrawController implements LineDataSelectorModelListener, JHVEventHighlightListener, LayersListener, TimeListener, PlotAreaSpaceListener {

    private static DrawController instance;
    private Interval<Date> selectedInterval = new Interval<Date>(null, null);
    private Interval<Date> availableInterval = new Interval<Date>(null, null);

    private PlotAreaSpace pas;

    private final List<TimingListener> tListeners;
    private boolean keepFullValueRange;
    private Rectangle graphSize;
    // private Rectangle graphArea;
    // private Rectangle plotArea;
    // private Rectangle leftAxisArea;
    private final List<GraphDimensionListener> gdListeners;

    private List<YAxisElement> yAxisSet;

    private final Map<DrawableType, Set<DrawableElement>> drawableElements;
    private final List<DrawControllerListener> listeners;

    // private final Map<YAxisElement, String> axisUnitMap;

    private DrawController() {
        drawableElements = new HashMap<DrawableType, Set<DrawableElement>>();
        listeners = new ArrayList<DrawControllerListener>();
        yAxisSet = new ArrayList<YAxisElement>();
        tListeners = new ArrayList<TimingListener>();
        keepFullValueRange = false;
        gdListeners = new ArrayList<GraphDimensionListener>();
        graphSize = new Rectangle();
        // axisUnitMap = new HashMap<YAxisElement, String>();
    }

    public static DrawController getSingletonInstance() {
        if (instance == null) {
            instance = new DrawController();
            instance.init();
        }
        return instance;
    }

    private void init() {
        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);
        pas = PlotAreaSpace.getSingletonInstance();
        pas.addPlotAreaSpaceListener(this);
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
        this.addDrawableElement(drawableElement, false);

        if (needsFire && drawableElement.hasElementsToDraw()) {
            this.fireRedrawRequest();
        }
    }

    public void updateDrawableElement(DrawableElement drawableElement) {
        updateDrawableElement(drawableElement, true);
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
            this.fireRedrawRequest();
        }
    }

    private void removeDrawableElement(DrawableElement element, boolean redraw, boolean keepYAxisElement) {
        Set<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
        if (elements != null && !keepYAxisElement) {
            elements.remove(element);
            createYAxisSet();
        }
        if (redraw) {
            this.fireRedrawRequest();
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

    public List<DrawableElement> getAllDrawableElements() {
        Collection<Set<DrawableElement>> allValues = getDrawableElements().values();
        ArrayList<DrawableElement> deList = new ArrayList<DrawableElement>();
        for (Set<DrawableElement> tempList : allValues) {
            deList.addAll(tempList);
        }
        return deList;
    }

    public boolean hasElementsToBeDrawn() {
        List<DrawableElement> allElements = this.getAllDrawableElements();
        return !allElements.isEmpty();
    }

    public boolean getIntervalAvailable() {
        if (selectedInterval == null) {
            return false;
        } else {
            return selectedInterval.getStart() != null && selectedInterval.getEnd() != null;
        }
    }

    public void setSelectedRange(Range selectedRange) {
        fireRedrawRequest();
    }

    public void fireRedrawRequest() {
        for (DrawControllerListener l : listeners) {
            l.drawRequest();
        }
    }

    public void setAvailableInterval(final Interval<Date> interval) {
        availableInterval = makeCompleteDay(interval);
        // Log.debug("New available interval : " + availableInterval);
        fireAvailableIntervalChanged(availableInterval);

        // request data if needed
        final Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(availableInterval.getEnd());
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        final Interval<Date> downloadInterval = new Interval<Date>(availableInterval.getStart(), calendar.getTime());

        DownloadController.getSingletonInstance().updateBands(downloadInterval, selectedInterval);

        // check if selected interval is in available interval and correct it if
        // needed
        setSelectedInterval(selectedInterval, false);
        // PlotTimeSpace.getInstance().setSelectedMinAndMaxTime(interval.getStart(),
        // interval.getEnd());
    }

    public final Interval<Date> getAvailableInterval() {
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
    public void timeChanged(Date date) {
        fireRedrawRequestMovieFrameChanged(date);
    }

    public Date getLastDateWithData() {
        Date lastDate = null;
        Date tempLastDate = null;
        for (Set<DrawableElement> des : drawableElements.values()) {
            for (DrawableElement de : des) {
                if (de.getLastDateWithData() != null) {
                    if (tempLastDate == null || de.getLastDateWithData().before(tempLastDate)) {
                        tempLastDate = de.getLastDateWithData();
                    }
                }
            }
        }
        if (tempLastDate != null) {
            if (lastDate == null || lastDate.before(tempLastDate)) {
                lastDate = tempLastDate;
            }
        }
        return lastDate;
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        fireRedrawRequest();
    }

    @Override
    public void layerAdded(View view) {
        final Interval<Date> interval = new Interval<Date>(Layers.getFirstDate(), Layers.getLastDate());
        if (availableInterval == null || availableInterval.getStart() == null || availableInterval.getEnd() == null) {
            availableInterval = interval;
        } else {
            Date start = availableInterval.getStart();
            if (interval.getStart().before(start)) {
                start = interval.getStart();
            }
            Date end = availableInterval.getEnd();
            if (interval.getEnd().after(end)) {
                end = interval.getEnd();
            }
            setAvailableInterval(new Interval<Date>(start, end));
        }
        TimeIntervalLockModel lockModel = TimeIntervalLockModel.getInstance();
        if (lockModel.isLocked()) {
            setSelectedInterval(interval, true);
            lockModel.setLocked(true);
        }
    }

    @Override
    public void activeLayerChanged(View view) {
        if (view == null) {
            fireRedrawRequestMovieFrameChanged(null);
        }
    }

    private Interval<Date> makeCompleteDay(final Interval<Date> interval) {
        return makeCompleteDay(interval.getStart(), interval.getEnd());
    }

    private Interval<Date> makeCompleteDay(final Date start, final Date end) {
        final Interval<Date> interval = new Interval<Date>(null, null);
        Date endDate = end;

        if (start == null || end == null) {
            return interval;
        }

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

        interval.setStart(calendar.getTime());

        calendar.clear();
        calendar.setTime(endDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        interval.setEnd(calendar.getTime());

        return interval;
    }

    public Interval<Date> setSelectedInterval(final Interval<Date> newSelectedInterval, boolean useFullValueSpace) {
        return setSelectedInterval(newSelectedInterval, useFullValueSpace, true);
    }

    private Interval<Date> setSelectedInterval(final Interval<Date> newSelectedInterval, boolean useFullValueSpace, boolean updatePlotAreaSpace) {
        keepFullValueRange = useFullValueSpace;
        if (availableInterval.getStart() == null || availableInterval.getEnd() == null) {
            selectedInterval = new Interval<Date>(null, null);
        } else if (newSelectedInterval.getStart() == null || newSelectedInterval.getEnd() == null) {
            selectedInterval = availableInterval;
        } else if (newSelectedInterval.getStart().before(newSelectedInterval.getEnd())) {
            if (availableInterval.containsInclusive(newSelectedInterval)) {
                selectedInterval = newSelectedInterval;
            } else {
                Date start = newSelectedInterval.getStart();
                Date end = newSelectedInterval.getEnd();

                // start = availableInterval.containsPointInclusive(start) ?
                // start : availableInterval.getStart();
                // end = availableInterval.containsPointInclusive(end) ? end :
                // availableInterval.getEnd();
                Date availableStart = availableInterval.getStart();
                Date availableEnd = availableInterval.getEnd();
                boolean changeAvailableInterval = false;

                if (!availableInterval.containsPointInclusive(start) && !availableInterval.containsPointInclusive(end)) {
                    changeAvailableInterval = true;
                    availableStart = start;
                    availableEnd = end;
                }

                if (start.equals(end)) {
                    selectedInterval = new Interval<Date>(availableStart, availableEnd);
                } else {
                    selectedInterval = new Interval<Date>(start, end);
                }
                if (changeAvailableInterval) {
                    setAvailableInterval(new Interval<Date>(availableStart, availableEnd));
                }
            }
            if (updatePlotAreaSpace) {
                updatePlotAreaSpace(selectedInterval);
            }
            fireSelectedIntervalChanged(useFullValueSpace);
            fireRedrawRequest();
        } else {
            Log.debug("Start was after end. Set by: ");
            Thread.dumpStack();
        }
        return selectedInterval;
    }

    public Interval<Date> getSelectedInterval() {
        return selectedInterval;
    }

    private void fireSelectedIntervalChanged(boolean keepFullValueRange) {
        for (TimingListener listener : tListeners) {
            listener.selectedIntervalChanged(keepFullValueRange);
        }
    }

    private void updatePlotAreaSpace(Interval<Date> selectedInterval) {
        if (availableInterval != null && availableInterval.getStart() != null && availableInterval.getEnd() != null && selectedInterval != null && selectedInterval.getStart() != null && selectedInterval.getEnd() != null) {
            long diffAvailable = availableInterval.getEnd().getTime() - availableInterval.getStart().getTime();
            double diffPlotAreaTime = pas.getScaledMaxTime() - pas.getScaledMinTime();
            double scaledSelectedStart = pas.getScaledMinTime() + (1.0 * (selectedInterval.getStart().getTime() - availableInterval.getStart().getTime()) * diffPlotAreaTime / diffAvailable);
            double scaledSelectedEnd = pas.getScaledMinTime() + (1.0 * (selectedInterval.getEnd().getTime() - availableInterval.getStart().getTime()) * diffPlotAreaTime / diffAvailable);
            pas.setMinSelectedTimeDiff(60000.0 / diffAvailable);
            pas.setScaledSelectedTime(scaledSelectedStart, scaledSelectedEnd, true);
        }
    }

    private void fireAvailableIntervalChanged(final Interval<Date> newInterval) {
        for (TimingListener listener : tListeners) {
            listener.availableIntervalChanged();
        }
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinTime, double scaledMaxTime, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        if (availableInterval.getStart() != null && availableInterval.getEnd() != null && selectedInterval.getStart() != null && selectedInterval.getEnd() != null) {
            long diffTime = availableInterval.getEnd().getTime() - availableInterval.getStart().getTime();
            double scaleDiff = scaledMaxTime - scaledMinTime;
            double selectedMin = (scaledSelectedMinTime - scaledMinTime) / scaleDiff;
            double selectedMax = (scaledSelectedMaxTime - scaledMinTime) / scaleDiff;
            Date newSelectedStartTime = new Date(availableInterval.getStart().getTime() + Math.round(diffTime * selectedMin));
            Date newSelectedEndTime = new Date(availableInterval.getStart().getTime() + Math.round(diffTime * selectedMax));
            // Log.info("plotareachanged starttime: " + newSelectedStartTime
            // + " endtime: " + newSelectedEndTime);
            if (forced || !(newSelectedEndTime.equals(selectedInterval.getEnd()) && newSelectedStartTime.equals(selectedInterval.getStart()))) {
                setSelectedInterval(new Interval<Date>(newSelectedStartTime, newSelectedEndTime), false, false);
            }
        }
    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinTime, double oldMaxTime, double newMinTime, double newMaxTime) {
        if (availableInterval != null && availableInterval.getStart() != null && availableInterval.getEnd() != null && (oldMinTime > newMinTime || oldMaxTime < newMaxTime)) {
            double timeRatio = (availableInterval.getEnd().getTime() - availableInterval.getStart().getTime()) / (oldMaxTime - oldMinTime);
            double startDifference = oldMinTime - newMinTime;
            double endDifference = newMaxTime - oldMaxTime;

            Date tempStartDate = new Date(availableInterval.getStart().getTime() - Math.round(startDifference * timeRatio));
            Date tempEndDate = new Date(availableInterval.getEnd().getTime() + Math.round(endDifference * timeRatio));

            setAvailableInterval(new Interval<Date>(tempStartDate, tempEndDate));
        }
    }

    public boolean keepfullValueRange() {
        return keepFullValueRange;
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
        return new Rectangle(ChartConstants.getGraphLeftSpace(), ChartConstants.getGraphTopSpace(), getGraphWidth(), getGraphHeight());
    }

    public Rectangle getLeftAxisArea() {
        return new Rectangle(0, ChartConstants.getGraphTopSpace(), ChartConstants.getGraphLeftSpace(), getGraphHeight() - (ChartConstants.getGraphTopSpace() + ChartConstants.getGraphBottomSpace()));
    }

    private int getGraphHeight() {
        return graphSize.height - (ChartConstants.getGraphTopSpace() + ChartConstants.getGraphBottomSpace());
    }

    private int getGraphWidth() {
        int twoYAxis = 0;
        if (getYAxisElements().size() >= 2) {
            twoYAxis = 1;
        }
        return graphSize.width - (ChartConstants.getGraphLeftSpace() + ChartConstants.getGraphRightSpace() + twoYAxis * ChartConstants.getTwoAxisGraphRight());
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
        return null;
    }
}
