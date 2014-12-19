/**
 * 
 */
package org.helioviewer.plugins.eveplugin.model;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.controller.DrawControllerListener;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;

/**
 * Adapts the plot area space of the plots in case the interval is locked.
 * 
 * @author Bram.Bourgoignie@oma.be
 * 
 */
public class TimeIntervalLockModel implements ZoomControllerListener, DrawControllerListener, PlotAreaSpaceListener {
    /** Instance of the zoom controller */
    private final ZoomController zoomController;

    /** Instance of the draw controller */
    private final DrawController drawController;

    /** Instance of the plot area space manager */
    private final PlotAreaSpaceManager plotAreaSpaceManager;

    /** Is the time interval locked */
    private boolean isLocked;

    /** The current available time interval */
    private Interval<Date> currentAvailableInterval;

    /** The current selected widths of the plot area space */
    private final Map<PlotAreaSpace, Double> selectedSpaceWidth;

    /** Singleton instance of the time interval lock model */
    private static TimeIntervalLockModel instance;

    /** Holds the previous movie time */
    private Date previousMovieTime;

    /**
     * Private constructor
     */
    private TimeIntervalLockModel() {
        zoomController = ZoomController.getSingletonInstance();
        drawController = DrawController.getSingletonInstance();
        plotAreaSpaceManager = PlotAreaSpaceManager.getInstance();
        isLocked = false;
        currentAvailableInterval = new Interval<Date>(null, null);
        zoomController.addZoomControllerListener(this);
        drawController.addDrawControllerListenerForAllIdentifiers(this);
        selectedSpaceWidth = new HashMap<PlotAreaSpace, Double>();
        previousMovieTime = new Date();
        plotAreaSpaceManager.addPlotAreaSpaceListenerToAllSpaces(this);
    }

    /**
     * Gives acces to the singleton instance of the time interval lock model.
     * 
     * @return The singleton instance of the time interval lock model
     */
    public static TimeIntervalLockModel getInstance() {
        if (instance == null) {
            instance = new TimeIntervalLockModel();
        }
        return instance;
    }

    /**
     * Is the time interval locked
     * 
     * @return true if the interval is locked, false if the interval is not
     *         locked
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * Sets the locked state of the time interval.
     * 
     * @param isLocked
     *            true if the interval is locked, false if the interval is not
     *            locked
     */
    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
        if (isLocked) {
            Collection<PlotAreaSpace> spaces = plotAreaSpaceManager.getAllPlotAreaSpaces();
            for (PlotAreaSpace space : spaces) {
                selectedSpaceWidth.put(space, space.getScaledSelectedMaxTime() - space.getScaledSelectedMinTime());
            }
        }
    }

    /*
     * DrawControllerListener
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.helioviewer.plugins.eveplugin.controller.DrawControllerListener#
     * drawRequest()
     */
    @Override
    public void drawRequest() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.helioviewer.plugins.eveplugin.controller.DrawControllerListener#
     * drawMovieLineRequest(java.util.Date)
     */
    @Override
    public void drawMovieLineRequest(Date time) {
        if (isLocked && currentAvailableInterval.containsPointInclusive(time) && !previousMovieTime.equals(time)) {
            // Log.trace("previousTimeInterval : " + previousMovieTime +
            // " currentMovieTime : " + time);
            previousMovieTime = time;
            Map<PlotAreaSpace, Double> selectedStartTimes = new HashMap<PlotAreaSpace, Double>();
            Map<PlotAreaSpace, Double> selectedEndTimes = new HashMap<PlotAreaSpace, Double>();
            Collection<PlotAreaSpace> spaces = plotAreaSpaceManager.getAllPlotAreaSpaces();
            for (PlotAreaSpace space : spaces) {
                double selectedIntervalWidth = selectedSpaceWidth.get(space);
                long movieTimeDiff = time.getTime() - currentAvailableInterval.getStart().getTime();
                double availableIntervalWidthScaled = space.getScaledMaxTime() - space.getScaledMinTime();
                long availableIntervalWidthAbs = currentAvailableInterval.getEnd().getTime() - currentAvailableInterval.getStart().getTime();
                double scaledPerTime = availableIntervalWidthScaled / availableIntervalWidthAbs;
                double scaledMoviePosition = space.getScaledMinTime() + movieTimeDiff * scaledPerTime;
                double newSelectedScaledStart = Math.max(space.getScaledMinTime(), scaledMoviePosition - (selectedIntervalWidth / 2));
                double newSelectedScaledEnd = Math.min(space.getScaledMaxTime(), scaledMoviePosition + (selectedIntervalWidth / 2));
                selectedStartTimes.put(space, newSelectedScaledStart);
                selectedEndTimes.put(space, newSelectedScaledEnd);
            }
            for (PlotAreaSpace space : spaces) {
                space.setScaledSelectedTime(selectedStartTimes.get(space), selectedEndTimes.get(space), false);
            }
        }
    }

    /*
     * ZoomControllerListener
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener#
     * availableIntervalChanged(org.helioviewer.base.math.Interval)
     */
    @Override
    public void availableIntervalChanged(Interval<Date> newInterval) {
        currentAvailableInterval = newInterval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener#
     * selectedIntervalChanged(org.helioviewer.base.math.Interval)
     */
    @Override
    public void selectedIntervalChanged(Interval<Date> newInterval, boolean keepFullValueSpace) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener#
     * selectedResolutionChanged
     * (org.helioviewer.plugins.eveplugin.settings.EVEAPI
     * .API_RESOLUTION_AVERAGES)
     */
    @Override
    public void selectedResolutionChanged(API_RESOLUTION_AVERAGES newResolution) {
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        Collection<PlotAreaSpace> spaces = plotAreaSpaceManager.getAllPlotAreaSpaces();
        for (PlotAreaSpace space : spaces) {
            selectedSpaceWidth.put(space, scaledSelectedMaxTime - scaledSelectedMinTime);
        }
    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinValue, double oldMaxValue, double oldMinTime, double oldMaxTime, double newMinValue, double newMaxValue, double newMinTime, double newMaxTime) {
        // TODO Auto-generated method stub

    }

}
