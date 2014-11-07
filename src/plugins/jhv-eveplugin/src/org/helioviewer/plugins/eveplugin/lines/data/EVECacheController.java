package org.helioviewer.plugins.eveplugin.lines.data;

import java.awt.EventQueue;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.base.Range;
import org.helioviewer.plugins.eveplugin.download.DataDownloader;
import org.helioviewer.plugins.eveplugin.download.DownloadedData;

/**
 * 
 * 
 * @author Stephan Pagel
 * */
public class EVECacheController implements DataDownloader {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    /** the sole instance of this class */
    private static final EVECacheController singletonInstance = new EVECacheController();

    /***/
    private final LinkedList<EVECacheControllerListener> controllerListeners = new LinkedList<EVECacheControllerListener>();

    /***/
    private final EVEBandCache cache = new EVEBandCache();

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

    public void addToCache(final EVEValue[] values, final Band band) {
        cache.add(band, values);

        fireDataAdded(band);
    }

    public EVEValues getDataInInterval(final Band band, final Interval<Date> interval) {
        if (band == null || interval == null || interval.getStart() == null || interval.getEnd() == null) {
            return null;
        }

        return cache.getValuesInInterval(band, interval);
    }

    public Range getMinMaxInInterval(final Band band, final Interval<Date> interval) {
        if (band == null || interval == null || interval.getStart() == null || interval.getEnd() == null) {
            return new Range();
        }

        return cache.getMinMaxInInterval(band, interval);
    }

    public List<Interval<Date>> getMissingDaysInInterval(final Band band, final Interval<Date> interval) {
        return cache.getMissingDatesInInterval(band, new Interval<Date>(getDay(interval.getStart()), getDay(interval.getEnd())));
    }

    private Date getDay(final Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);

        GregorianCalendar day = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        return day.getTime();
    }

    private void fireDataAdded(final Band band) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                // long start = System.currentTimeMillis();
                for (EVECacheControllerListener listener : controllerListeners) {
                    listener.dataAdded(band);
                }
                // Log.debug("fireDataAdded time" + (System.currentTimeMillis()
                // - start));
            }
        });

    }

    @Override
    public DownloadedData downloadData(Band band, Interval<Date> interval) {
        if (band == null || interval == null || interval.getStart() == null || interval.getEnd() == null) {
            return null;
        }

        return cache.getValuesInInterval(band, interval);
    }
}
