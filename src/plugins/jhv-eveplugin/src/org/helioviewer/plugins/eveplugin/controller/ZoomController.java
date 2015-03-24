package org.helioviewer.plugins.eveplugin.controller;

import java.awt.EventQueue;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.plugins.eveplugin.lines.data.DownloadController;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;
import org.helioviewer.plugins.eveplugin.settings.BandType;
//import org.helioviewer.plugins.eveplugin.model.PlotTimeSpace;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;
import org.helioviewer.viewmodel.view.View;

/**
 * 
 * @author Stephan Pagel
 * */
public class ZoomController implements PlotAreaSpaceListener, LayersListener {

    /** the sole instance of this class */
    private static final ZoomController singletonInstance = new ZoomController();

    public enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington
    };

    private final LinkedList<ZoomControllerListener> listeners = new LinkedList<ZoomControllerListener>();

    private Interval<Date> availableInterval = new Interval<Date>(null, null);
    private Interval<Date> selectedInterval = new Interval<Date>(null, null);

    private API_RESOLUTION_AVERAGES selectedResolution = API_RESOLUTION_AVERAGES.MINUTE_1;

    private final PlotAreaSpaceManager plotAreaSpaceManager;

    /**
     * The private constructor to support the singleton pattern.
     * */
    private ZoomController() {
        plotAreaSpaceManager = PlotAreaSpaceManager.getInstance();
        plotAreaSpaceManager.addPlotAreaSpaceListenerToAllSpaces(this);
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static ZoomController getSingletonInstance() {
        return singletonInstance;
    }

    public boolean addZoomControllerListener(final ZoomControllerListener listener) {
        synchronized (listeners) {
            return listeners.add(listener);
        }
    }

    public boolean removeControllerListener(final ZoomControllerListener listener) {
        synchronized (listeners) {
            return listeners.remove(listener);
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

    public final Interval<Date> getAvailableInterval() {
        return availableInterval;
    }

    private void fireAvailableIntervalChanged(final Interval<Date> newInterval) {
        synchronized (listeners) {
            for (ZoomControllerListener listener : listeners) {
                listener.availableIntervalChanged(newInterval);
            }
        }
    }

    public Interval<Date> setSelectedInterval(final Interval<Date> newSelectedInterval, boolean useFullValueSpace) {
        synchronized (selectedInterval) {
            if (availableInterval.getStart() == null || availableInterval.getEnd() == null) {
                selectedInterval = new Interval<Date>(null, null);
            } else if (newSelectedInterval.getStart() == null || newSelectedInterval.getEnd() == null) {
                selectedInterval = availableInterval;
            } else if (availableInterval.containsInclusive(newSelectedInterval)) {
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

            updatePlotAreaSpace(selectedInterval);
            fireSelectedIntervalChanged(selectedInterval, useFullValueSpace);

            return selectedInterval;
        }
    }

    public Interval<Date> zoomTo(final ZOOM zoom, final int value) {
        Interval<Date> newInterval = new Interval<Date>(null, null);
        switch (zoom) {
        case CUSTOM:
            newInterval = selectedInterval;
            break;
        case All:
            newInterval = availableInterval;
            break;
        case Day:
            newInterval = computeZoomInterval(selectedInterval, Calendar.DAY_OF_MONTH, value);
            break;
        case Hour:
            newInterval = computeZoomInterval(selectedInterval, Calendar.HOUR, value);
            break;
        case Month:
            newInterval = computeZoomInterval(selectedInterval, Calendar.MONTH, value);
            break;
        case Year:
            newInterval = computeZoomInterval(selectedInterval, Calendar.YEAR, value);
            break;
        case Carrington:
            newInterval = computeCarringtonInterval(selectedInterval, value);
        }
        return setSelectedInterval(newInterval, true);
    }

    private Interval<Date> computeCarringtonInterval(Interval<Date> interval, int value) {
        return computeZoomForMilliSeconds(interval, value * 2356585920l);
    }

    private Interval<Date> computeZoomForMilliSeconds(final Interval<Date> interval, long differenceMilli) {
        Date middle = new Date(interval.getStart().getTime() + (interval.getEnd().getTime() - interval.getStart().getTime()) / 2);
        Date startDate = interval.getStart();
        // Date endDate = interval.getEnd();
        GregorianCalendar gce = new GregorianCalendar();
        gce.clear();
        gce.setTime(new Date(middle.getTime() + differenceMilli / 2));
        Date endDate = gce.getTime();

        final Date lastdataDate = DrawController.getSingletonInstance().getLastDateWithData();
        if (lastdataDate != null) {
            if (endDate.after(lastdataDate)) {
                endDate = lastdataDate;
            }
        } else if (endDate.after(new Date())) {
            endDate = new Date();
        }
        final Date availableStartDate = availableInterval.getStart();

        if (startDate == null || endDate == null || availableStartDate == null) {
            return new Interval<Date>(null, null);
        }

        final GregorianCalendar calendar = new GregorianCalendar();

        // add difference to start date -> when calculated end date is within
        // available interval it is the result
        calendar.clear();
        calendar.setTime(new Date(endDate.getTime() - differenceMilli));

        startDate = calendar.getTime();

        boolean sInAvailable = availableInterval.containsPointInclusive(startDate);
        boolean eInAvailable = availableInterval.containsPointInclusive(endDate);

        if (sInAvailable && eInAvailable) {
            return new Interval<Date>(startDate, endDate);
        }

        Date availableS = sInAvailable ? availableInterval.getStart() : startDate;
        Date availableE = eInAvailable ? availableInterval.getEnd() : endDate;

        setAvailableInterval(new Interval<Date>(availableS, availableE));

        return new Interval<Date>(startDate, endDate);

    }

    private Interval<Date> computeZoomInterval(final Interval<Date> interval, final int calendarField, final int difference) {
        return computeZoomForMilliSeconds(interval, differenceInMilliseconds(calendarField, difference));
    }

    private Long differenceInMilliseconds(final int calendarField, final int value) {
        switch (calendarField) {
        case Calendar.YEAR:
            return value * 365 * 24 * 60 * 60 * 1000l;
        case Calendar.MONTH:
            return value * 30 * 24 * 60 * 60 * 1000l;
        case Calendar.DAY_OF_MONTH:
        case Calendar.DAY_OF_WEEK:
        case Calendar.DAY_OF_WEEK_IN_MONTH:
        case Calendar.DAY_OF_YEAR:
            return value * 24 * 60 * 60 * 1000l;
        case Calendar.HOUR:
        case Calendar.HOUR_OF_DAY:
            return value * 60 * 60 * 1000l;
        case Calendar.MINUTE:
            return value * 60 * 1000l;
        case Calendar.SECOND:
            return value * 1000l;
        case Calendar.MILLISECOND:
            return value * 1l;
        default:
            return null;
        }
    }

    public Interval<Date> getSelectedInterval() {
        return selectedInterval;
    }

    private void fireSelectedIntervalChanged(final Interval<Date> newInterval, boolean keepFullValueSpace) {
        synchronized (listeners) {
            for (ZoomControllerListener listener : listeners) {
                listener.selectedIntervalChanged(newInterval, keepFullValueSpace);
            }
        }
    }

    public void setSelectedResolution(final API_RESOLUTION_AVERAGES resolution) {
        selectedResolution = resolution;
        fireSelectedResolutionChanged(selectedResolution);
    }

    public API_RESOLUTION_AVERAGES getSelectedResolution() {
        return selectedResolution;
    }

    private void fireSelectedResolutionChanged(final API_RESOLUTION_AVERAGES reolution) {
        synchronized (listeners) {
            for (ZoomControllerListener listener : listeners) {
                listener.selectedResolutionChanged(reolution);
            }
        }
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        if (availableInterval.getStart() != null && availableInterval.getEnd() != null && selectedInterval.getStart() != null && selectedInterval.getEnd() != null) {
            synchronized (selectedInterval) {
                long diffTime = availableInterval.getEnd().getTime() - availableInterval.getStart().getTime();
                double scaleDiff = scaledMaxTime - scaledMinTime;
                double selectedMin = (scaledSelectedMinTime - scaledMinTime) / scaleDiff;
                double selectedMax = (scaledSelectedMaxTime - scaledMinTime) / scaleDiff;
                Date newSelectedStartTime = new Date(availableInterval.getStart().getTime() + Math.round(diffTime * selectedMin));
                Date newSelectedEndTime = new Date(availableInterval.getStart().getTime() + Math.round(diffTime * selectedMax));
                // Log.info("plotareachanged starttime: " + newSelectedStartTime
                // + " endtime: " + newSelectedEndTime);
                if (forced || !(newSelectedEndTime.equals(selectedInterval.getEnd()) && newSelectedStartTime.equals(selectedInterval.getStart()))) {
                    selectedInterval = new Interval<Date>(newSelectedStartTime, newSelectedEndTime);
                    fireSelectedIntervalChanged(selectedInterval, false);
                }
            }
        }
    }

    private void updatePlotAreaSpace(Interval<Date> selectedInterval) {
        if (availableInterval != null && availableInterval.getStart() != null && availableInterval.getEnd() != null && selectedInterval != null && selectedInterval.getStart() != null && selectedInterval.getEnd() != null) {
            for (PlotAreaSpace pas : plotAreaSpaceManager.getAllPlotAreaSpaces()) {
                long diffAvailable = availableInterval.getEnd().getTime() - availableInterval.getStart().getTime();
                double diffPlotAreaTime = pas.getScaledMaxTime() - pas.getScaledMinTime();
                double scaledSelectedStart = pas.getScaledMinTime() + (1.0 * (selectedInterval.getStart().getTime() - availableInterval.getStart().getTime()) * diffPlotAreaTime / diffAvailable);
                double scaledSelectedEnd = pas.getScaledMinTime() + (1.0 * (selectedInterval.getEnd().getTime() - availableInterval.getStart().getTime()) * diffPlotAreaTime / diffAvailable);
                pas.setScaledSelectedTimeAndValue(scaledSelectedStart, scaledSelectedEnd, pas.getScaledMinValue(), pas.getScaledMaxValue());

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

    @Override
    public void layerAdded(int idx) {
        if (EventQueue.isDispatchThread()) {
            handleLayerAdded();
        } else {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    handleLayerAdded();
                }

            });
        }
    }

    private void handleLayerAdded() {
        final Interval<Date> interval = new Interval<Date>(LayersModel.getSingletonInstance().getFirstDate(), LayersModel.getSingletonInstance().getLastDate());
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
    public void layerRemoved(View oldView, int oldIdx) {
    }

    @Override
    public void layerChanged(int idx) {
    }

    @Override
    public void activeLayerChanged(int idx) {
    }

}
