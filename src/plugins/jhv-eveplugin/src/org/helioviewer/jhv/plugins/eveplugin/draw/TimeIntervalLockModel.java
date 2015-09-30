/**
 *
 */
package org.helioviewer.jhv.plugins.eveplugin.draw;

import java.util.Date;

import org.helioviewer.base.interval.Interval;

/**
 * Adapts the plot area space of the plots in case the interval is locked.
 *
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class TimeIntervalLockModel implements TimingListener, DrawControllerListener {

    /** Instance of the draw controller */
    private final DrawController drawController;

    /** Is the time interval locked */
    private boolean isLocked;

    /** Singleton instance of the time interval lock model */
    private static TimeIntervalLockModel instance;

    /** Holds the previous movie time */
    private Date latestMovieTime;

    private final PlotAreaSpace plotAreaSpace;

    /**
     * Private constructor
     */
    private TimeIntervalLockModel() {
        drawController = DrawController.getSingletonInstance();
        isLocked = false;
        // currentAvailableInterval = new Interval<Date>(null, null);
        drawController.addTimingListener(this);
        drawController.addDrawControllerListener(this);
        latestMovieTime = new Date();
        plotAreaSpace = drawController.getPlotAreaSpace();
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
            if (latestMovieTime != null) {
                drawMovieLineRequest(latestMovieTime);
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
        double selectedSpaceWidth = plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime();

        Interval<Date> currentAvailableInterval = drawController.getAvailableInterval();
        if (time != null && isLocked && currentAvailableInterval.containsPointInclusive(time) && !latestMovieTime.equals(time)) {
            latestMovieTime = time;

            long movieTimeDiff = time.getTime() - currentAvailableInterval.getStart().getTime();
            double availableIntervalWidthScaled = plotAreaSpace.getScaledMaxTime() - plotAreaSpace.getScaledMinTime();
            long availableIntervalWidthAbs = currentAvailableInterval.getEnd().getTime() - currentAvailableInterval.getStart().getTime();
            double scaledPerTime = availableIntervalWidthScaled / availableIntervalWidthAbs;
            double scaledMoviePosition = plotAreaSpace.getScaledMinTime() + movieTimeDiff * scaledPerTime;
            double newSelectedScaledStart = scaledMoviePosition - (selectedSpaceWidth / 2);
            double newSelectedScaledEnd = scaledMoviePosition + (selectedSpaceWidth / 2);
            plotAreaSpace.setScaledSelectedTime(newSelectedScaledStart, newSelectedScaledEnd, false);

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
    public void availableIntervalChanged() {
        drawMovieLineRequest(latestMovieTime);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.helioviewer.plugins.eveplugin.controller.ZoomControllerListener#
     * selectedIntervalChanged(org.helioviewer.base.math.Interval)
     */
    @Override
    public void selectedIntervalChanged(boolean keepFullValueRange) {
    }

}
