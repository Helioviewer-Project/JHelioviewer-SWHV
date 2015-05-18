package org.helioviewer.plugins.eveplugin.draw;

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
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.TimeListener;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.plugins.eveplugin.lines.data.DownloadController;
import org.helioviewer.plugins.eveplugin.settings.BandType;
import org.helioviewer.plugins.eveplugin.view.chart.ChartConstants;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.viewmodel.view.AbstractView;

public class DrawController implements LineDataSelectorModelListener, JHVEventHighlightListener, LayersListener, TimeListener, PlotAreaSpaceListener {

    private static final DrawController instance = new DrawController();;
    // private final DrawControllerData drawControllerData;

    private Interval<Date> selectedInterval = new Interval<Date>(null, null);
    private Interval<Date> availableInterval = new Interval<Date>(null, null);

    private final PlotAreaSpace pas;

    private final List<TimingListener> tListeners;
    private boolean keepFullValueRange;
    private Rectangle graphSize;
    // private Rectangle graphArea;
    // private Rectangle plotArea;
    // private Rectangle leftAxisArea;
    private final List<GraphDimensionListener> gdListeners;

    private Set<YAxisElement> yAxisSet;
    private final Map<DrawableType, Set<DrawableElement>> drawableElements;
    private final List<DrawControllerListener> listeners;

    private DrawController() {
        drawableElements = new HashMap<DrawableType, Set<DrawableElement>>();
        listeners = new ArrayList<DrawControllerListener>();
        yAxisSet = new HashSet<YAxisElement>();
        // drawControllerData = new DrawControllerData();
        tListeners = new ArrayList<TimingListener>();
        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);
        LayersModel.addLayersListener(this);
        Displayer.addTimeListener(this);
        keepFullValueRange = false;
        pas = PlotAreaSpace.getSingletonInstance();
        pas.addPlotAreaSpaceListener(this);
        gdListeners = new ArrayList<GraphDimensionListener>();
        graphSize = new Rectangle();
    }

    public static DrawController getSingletonInstance() {
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

    public void updateDrawableElement(DrawableElement drawableElement) {
        removeDrawableElement(drawableElement, false);
        this.addDrawableElement(drawableElement, false);

        if (drawableElement.hasElementsToDraw()) {
            this.fireRedrawRequest();
        }
    }

    private void addDrawableElement(DrawableElement element, boolean redraw) {
        Set<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
        if (elements == null) {
            elements = new HashSet<DrawableElement>();
            drawableElements.put(element.getDrawableElementType().getLevel(), elements);
        }

        elements.add(element);

        Set<YAxisElement> tempSet = new HashSet<YAxisElement>(yAxisSet);
        if (element.getYAxisElement() != null) {
            tempSet.add(element.getYAxisElement());
            yAxisSet = tempSet;
        }
        if (redraw) {
            this.fireRedrawRequest();
        }
    }

    private void removeDrawableElement(DrawableElement element, boolean redraw) {
        Set<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
        if (elements != null) {
            elements.remove(element);
            createYAxisSet();
        }
        if (redraw) {
            this.fireRedrawRequest();
        }
    }

    public void removeDrawableElement(DrawableElement element) {
        removeDrawableElement(element, true);
    }

    public Set<YAxisElement> getYAxisElements() {
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

    private void fireRedrawRequest() {
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
        final BandType[] bandTypes = BandController.getSingletonInstance().getAllAvailableBandTypes();

        DownloadController.getSingletonInstance().updateBands(bandTypes, downloadInterval, selectedInterval);

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
                    if (tempLastDate == null || de.getLastDateWithData().before(lastDate)) {
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
    public void layerAdded(int idx) {
        final Interval<Date> interval = new Interval<Date>(LayersModel.getFirstDate(), LayersModel.getLastDate());
        if (availableInterval == null || availableInterval.getStart() == null || availableInterval.getEnd() == null) {
            availableInterval = interval;
        } else {
            Date start = availableInterval.getStart();
            if (interval.getStart().before(availableInterval.getStart())) {
                start = interval.getStart();
            }
            Date end = availableInterval.getEnd();
            if (interval.getEnd().after(availableInterval.getEnd())) {
                end = interval.getEnd();
            }
            this.setAvailableInterval(new Interval<Date>(start, end));
        }
    }

    @Override
    public void layerRemoved(int oldIdx) {
        if (LayersModel.getNumLayers() == 0) {
            fireRedrawRequestMovieFrameChanged(null);
        }
    }

    @Override
    public void activeLayerChanged(AbstractView view) {
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

                start = availableInterval.containsPointInclusive(start) ? start : availableInterval.getStart();
                end = availableInterval.containsPointInclusive(end) ? end : availableInterval.getEnd();

                if (start.equals(end)) {
                    selectedInterval = availableInterval;
                } else {
                    selectedInterval = new Interval<Date>(start, end);
                }
            }
            if (updatePlotAreaSpace) {
                updatePlotAreaSpace(selectedInterval);
            }
            fireSelectedIntervalChanged();
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

    private void fireSelectedIntervalChanged() {
        for (TimingListener listener : tListeners) {
            listener.selectedIntervalChanged();
        }
    }

    private void updatePlotAreaSpace(Interval<Date> selectedInterval) {
        if (availableInterval != null && availableInterval.getStart() != null && availableInterval.getEnd() != null && selectedInterval != null && selectedInterval.getStart() != null && selectedInterval.getEnd() != null) {
            long diffAvailable = availableInterval.getEnd().getTime() - availableInterval.getStart().getTime();
            double diffPlotAreaTime = pas.getScaledMaxTime() - pas.getScaledMinTime();
            double scaledSelectedStart = pas.getScaledMinTime() + (1.0 * (selectedInterval.getStart().getTime() - availableInterval.getStart().getTime()) * diffPlotAreaTime / diffAvailable);
            double scaledSelectedEnd = pas.getScaledMinTime() + (1.0 * (selectedInterval.getEnd().getTime() - availableInterval.getStart().getTime()) * diffPlotAreaTime / diffAvailable);
            pas.setScaledSelectedTimeAndValue(scaledSelectedStart, scaledSelectedEnd, pas.getScaledMinValue(), pas.getScaledMaxValue());
        }
    }

    private void fireAvailableIntervalChanged(final Interval<Date> newInterval) {
        for (TimingListener listener : tListeners) {
            listener.availableIntervalChanged();
        }
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
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
    public void availablePlotAreaSpaceChanged(double oldMinValue, double oldMaxValue, double oldMinTime, double oldMaxTime, double newMinValue, double newMaxValue, double newMinTime, double newMaxTime) {
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
        Set<YAxisElement> tempSet = new HashSet<YAxisElement>();
        for (Set<DrawableElement> elementsSet : drawableElements.values()) {
            for (DrawableElement de : elementsSet) {
                if (de.getYAxisElement() != null) {
                    tempSet.add(de.getYAxisElement());
                }
            }
        }
        yAxisSet = tempSet;
    }
}
