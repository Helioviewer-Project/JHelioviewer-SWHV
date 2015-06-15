package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.awt.Rectangle;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.cache.RequestCache;
import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;

/**
 *
 *
 * @author Stephan Pagel
 * */
public class EVECacheController {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    /** the sole instance of this class */
    private static final EVECacheController singletonInstance = new EVECacheController();

    /***/
    private final LinkedList<EVECacheControllerListener> controllerListeners = new LinkedList<EVECacheControllerListener>();

    /***/
    private final EVEBandCache cache = new EVEBandCache();

    private final Map<Band, RequestCache> requestCache = new HashMap<Band, RequestCache>();

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * The private constructor to support the singleton pattern.
     * */
    private EVECacheController() {
    }

    /**
     * Method returns the sole instance of this class.
     *
     * @return the only instance of this class.
     * */
    public static EVECacheController getSingletonInstance() {
        return singletonInstance;
    }

    /***/
    public void addControllerListener(final EVECacheControllerListener listener) {
        controllerListeners.add(listener);
    }

    /***/
    public void removeControllerListener(final EVECacheControllerListener listener) {
        controllerListeners.remove(listener);
    }

    public void addToCache(Band band, double[] values, long[] dates) {
        cache.add(band, values, dates);
        fireDataAdded(band);
    }

    public List<Interval<Date>> addRequest(Band band, Interval<Date> interval) {
        RequestCache rc = getRequestCache(band);
        return rc.adaptRequestCache(interval.getStart(), interval.getEnd());
    }

    public EVEValues getDataInInterval(final Band band, final Interval<Date> interval) {
        if (band == null || interval == null || interval.getStart() == null || interval.getEnd() == null) {
            return null;
        }

        return cache.getValuesInInterval(band, interval, DrawController.getSingletonInstance().getGraphArea());
    }

    public List<Interval<Date>> getMissingDaysInInterval(final Band band, final Interval<Date> interval) {
        RequestCache rc = getRequestCache(band);
        return rc.getMissingIntervals(interval);
    }

    private RequestCache getRequestCache(Band band) {
        RequestCache rc = requestCache.get(band);
        if (rc == null) {
            requestCache.put(band, new RequestCache());
        }
        return requestCache.get(band);
    }

    private Date getDay(final Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);

        GregorianCalendar day = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        return day.getTime();
    }

    private void fireDataAdded(final Band band) {
        for (EVECacheControllerListener listener : controllerListeners) {
            listener.dataAdded(band);
        }
    }

    public EVEValues downloadData(Band band, Interval<Date> interval, Rectangle plotArea) {
        if (band == null || interval == null || interval.getStart() == null || interval.getEnd() == null) {
            return null;
        }

        return cache.getValuesInInterval(band, interval, plotArea);
    }

}
