package org.helioviewer.plugins.eveplugin.controller;

import java.awt.EventQueue;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.base.logging.Log;

/**
 *
 * @author Stephan Pagel
 * */
public class ZoomController {

    /** the sole instance of this class */
    private static final ZoomController singletonInstance = new ZoomController();

    public enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington
    };

    private final DrawController drawController;

    /**
     * The private constructor to support the singleton pattern.
     * */
    private ZoomController() {
        drawController = DrawController.getSingletonInstance();
    }

    /**
     * Method returns the sole instance of this class.
     *
     * @return the only instance of this class.
     * */
    public static ZoomController getSingletonInstance() {
        return singletonInstance;
    }

    public Interval<Date> zoomTo(final ZOOM zoom, final int value) {
        if (!EventQueue.isDispatchThread()) {
            Log.error("Called by other thread than event queue : " + Thread.currentThread().getName());
            Thread.dumpStack();
            System.exit(666);
        }
        Interval<Date> newInterval = new Interval<Date>(null, null);
        Interval<Date> selectedInterval = drawController.getSelectedInterval();
        Interval<Date> availableInterval = drawController.getAvailableInterval();
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
        return drawController.setSelectedInterval(newInterval, true);
    }

    private Interval<Date> computeCarringtonInterval(Interval<Date> interval, int value) {
        return computeZoomForMilliSeconds(interval, value * 2356585920l);
    }

    private Interval<Date> computeZoomForMilliSeconds(final Interval<Date> interval, long differenceMilli) {
        Date middle = new Date(interval.getStart().getTime() + (interval.getEnd().getTime() - interval.getStart().getTime()) / 2);
        Date startDate = interval.getStart();
        Interval<Date> availableInterval = drawController.getAvailableInterval();
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

        drawController.setAvailableInterval(new Interval<Date>(availableS, availableE));

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
}
