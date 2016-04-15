package org.helioviewer.jhv.plugins.eveplugin.draw;

import org.helioviewer.jhv.base.interval.Interval;

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
    private long latestMovieTime;

    private final PlotAreaSpace plotAreaSpace;

    /**
     * Private constructor
     */
    private TimeIntervalLockModel() {
        drawController = DrawController.getSingletonInstance();
        isLocked = false;
        // currentAvailableInterval = new Interval(null, null);
        drawController.addTimingListener(this);
        drawController.addDrawControllerListener(this);
        latestMovieTime = Long.MIN_VALUE;
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
            if (latestMovieTime != Long.MIN_VALUE) {
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
     * drawMovieLineRequest(long time)
     */
    @Override
    public void drawMovieLineRequest(long time) {
        Interval currentAvailableInterval = drawController.getAvailableInterval();
        if (time != Long.MIN_VALUE && latestMovieTime != time && isLocked && currentAvailableInterval.containsPointInclusive(time)) {
            latestMovieTime = time;

            long movieTimeDiff = time - currentAvailableInterval.start;
            double availableIntervalWidthScaled = plotAreaSpace.getScaledMaxTime() - plotAreaSpace.getScaledMinTime();
            long availableIntervalWidthAbs = currentAvailableInterval.end - currentAvailableInterval.start;
            double scaledPerTime = availableIntervalWidthScaled / availableIntervalWidthAbs;
            double scaledMoviePosition = plotAreaSpace.getScaledMinTime() + movieTimeDiff * scaledPerTime;

            double halfSelectedSpaceWidth = 0.5 * (plotAreaSpace.getScaledSelectedMaxTime() - plotAreaSpace.getScaledSelectedMinTime());
            double newSelectedScaledStart = scaledMoviePosition - halfSelectedSpaceWidth;
            double newSelectedScaledEnd = scaledMoviePosition + halfSelectedSpaceWidth;
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
